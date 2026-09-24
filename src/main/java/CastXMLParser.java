import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CastXMLParser extends DefaultHandler {
    private static final int NUM_WORKERS = 4;

    private final String loginUser = DatabaseEnvironment.user();
    private final String loginPass = DatabaseEnvironment.password();
    private final String loginUrl = DatabaseEnvironment.url();

    private StringBuilder tempVal;
    private String currMovieId;
    private String currMovieTitle;
    private String currStarName;

    private BufferedWriter reportWriter;
    private Connection dbCon;

    private Map<String, String> movieIdToTitle;
    private Map<String, String> starNameToId;
    private Set<String> existingStarIds;
    private Set<String> existingStarMoviePairs;

    private int nextStarIdNumber;

    private BlockingQueue<CastInsertTask> queue;
    private ExecutorService executor;

    public void runExample() throws ParserConfigurationException, IOException, SAXException {
        long startTime = System.nanoTime();

        try (BufferedWriter reportWriter = new BufferedWriter(new FileWriter("cast_report.txt"))) {
            Class.forName("com.mysql.cj.jdbc.Driver");

            dbCon = DriverManager.getConnection(loginUrl, loginUser, loginPass);
            dbCon.setAutoCommit(false);

            this.reportWriter = reportWriter;

            queue = new LinkedBlockingQueue<>(5000);
            executor = Executors.newFixedThreadPool(NUM_WORKERS);

            for (int i = 0; i < NUM_WORKERS; i++) {
                executor.submit(new CastInsertWorker(queue, loginUrl, loginUser, loginPass));
            }

            parseDocument();

            for (int i = 0; i < NUM_WORKERS; i++) {
                queue.put(CastInsertTask.poisonPill());
            }

            executor.shutdown();

            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.out.println("Timed out waiting for cast workers.");
            }

            long endTime = System.nanoTime();
            System.out.println("Finished cast parsing!");
            System.out.println("Execution time: " + ((endTime - startTime) / 1_000_000) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dbCon != null) dbCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseDocument() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();
        saxParser.parse("casts124.xml", this);
    }

    @Override
    public void startDocument() {
        movieIdToTitle = new HashMap<>();
        starNameToId = new HashMap<>();
        existingStarIds = new HashSet<>();
        existingStarMoviePairs = new HashSet<>();

        try {
            loadExistingMovies();
            loadExistingStars();
            loadExistingStarMoviePairs();
            loadNextStarIdNumber();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadExistingMovies() throws SQLException {
        String query = "SELECT id, title FROM movies";

        try (PreparedStatement statement = dbCon.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                movieIdToTitle.put(
                        normalizeKey(rs.getString("id")),
                        rs.getString("title").trim()
                );
            }
        }
    }

    private void loadExistingStars() throws SQLException {
        String query = "SELECT id, name FROM stars";

        try (PreparedStatement statement = dbCon.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String starId = rs.getString("id");
                String starName = rs.getString("name");

                existingStarIds.add(starId);

                starNameToId.put(
                        normalizeKey(starName),
                        starId
                );
            }
        }
    }

    private void loadExistingStarMoviePairs() throws SQLException {
        String query = "SELECT star_id, movie_id FROM stars_in_movies";

        try (PreparedStatement statement = dbCon.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                existingStarMoviePairs.add(makePairKey(
                        rs.getString("star_id"),
                        rs.getString("movie_id")
                ));
            }
        }
    }

    private void loadNextStarIdNumber() throws SQLException {
        String query =
                "SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) AS max_id " +
                        "FROM stars " +
                        "WHERE UPPER(id) LIKE 'CA%'";

        try (PreparedStatement statement = dbCon.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                if (rs.wasNull()) {
                    nextStarIdNumber = 1;
                } else {
                    nextStarIdNumber = maxId + 1;
                }
            } else {
                nextStarIdNumber = 1;
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = new StringBuilder();

        if (qName.equalsIgnoreCase("m")) {
            currMovieId = null;
            currMovieTitle = null;
            currStarName = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = tempVal.toString().trim();

        if (qName.equalsIgnoreCase("f")) {
            currMovieId = value;
        } else if (qName.equalsIgnoreCase("t")) {
            currMovieTitle = value;
        } else if (qName.equalsIgnoreCase("a")) {
            currStarName = value;
        } else if (qName.equalsIgnoreCase("m")) {
            processCastRecord();
        }
    }

    private void processCastRecord() {
        try {
            if (isBlank(currMovieId)) {
                reportWriter.write("SKIPPED: cast entry has no movie id. Star=" + currStarName + "\n");
                return;
            }

            if (isBlank(currStarName) || isInvalidStarName(currStarName)) {
                reportWriter.write("SKIPPED: invalid actor '" + currStarName
                        + "' for movie " + currMovieId + " " + currMovieTitle + "\n");
                return;
            }

            String movieKey = normalizeKey(currMovieId);
            String dbTitle = movieIdToTitle.get(movieKey);

            if (dbTitle == null) {
                reportWriter.write("SKIPPED: movie id not found: " + currMovieId
                        + ", cast title=" + currMovieTitle + "\n");
                return;
            }

            if (!isBlank(currMovieTitle) && !titlesMatch(dbTitle, currMovieTitle)) {
                reportWriter.write("INCONSISTENT TITLE SKIPPED: movieId=" + currMovieId
                        + ", db title=" + dbTitle
                        + ", cast title=" + currMovieTitle + "\n");
                return;
            }

            String cleanStarName = currStarName.trim();
            String starKey = normalizeKey(cleanStarName);
            String starId = starNameToId.get(starKey);

            if (starId == null) {
                starId = insertNewStar(cleanStarName);
                starNameToId.put(starKey, starId);
            }

            String pairKey = makePairKey(starId, currMovieId);

            if (existingStarMoviePairs.contains(pairKey)) {
                reportWriter.write("DUPLICATE stars_in_movies SKIPPED: star="
                        + cleanStarName + ", movieId=" + currMovieId + "\n");
                return;
            }

            existingStarMoviePairs.add(pairKey);
            queue.put(new CastInsertTask(starId, currMovieId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String insertNewStar(String starName) throws SQLException, IOException {
        String starId = generateNewStarId();
        String query = "INSERT INTO stars (id, name, birth_year) VALUES (?, ?, NULL)";

        try (PreparedStatement statement = dbCon.prepareStatement(query)) {
            statement.setString(1, starId);
            statement.setString(2, starName);
            statement.executeUpdate();
        }

        dbCon.commit();
        reportWriter.write("NEW STAR INSERTED: " + starId + " " + starName + "\n");
        return starId;
    }

    private String generateNewStarId() {
        String starId;

        do {
            starId = String.format("CA%07d", nextStarIdNumber);
            nextStarIdNumber++;
        } while (existingStarIds.contains(starId));

        existingStarIds.add(starId);
        return starId;
    }

    private boolean titlesMatch(String dbTitle, String castTitle) {
        return normalizeTitle(dbTitle).equals(normalizeTitle(castTitle));
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }

    private String makePairKey(String starId, String movieId) {
        return normalizeKey(starId) + "#" + normalizeKey(movieId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isInvalidStarName(String starName) {
        String normalized = normalizeKey(starName);
        return normalized.equals("sa") || normalized.equals("s a");
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        CastXMLParser parser = new CastXMLParser();
        parser.runExample();
    }
}

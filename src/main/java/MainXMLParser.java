import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainXMLParser extends DefaultHandler {
    private static final String movieQuery =
            "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";

    private StringBuilder tempVal;
    private Movie tempMov;
    private String dir;
    private BufferedWriter writer;
    private BufferedWriter reportWriter;
    private Connection dbCon;
    private Set<String> existingMovieKeys;
    private Set<String> existingMovieIds;
    private Map<String, Integer> genreMapping;
    private Map<String, String> genreNormalizeMap;
    private static final int NUM_WORKERS = 4;
    private BlockingQueue<MovieInsertTask> queue;
    private ExecutorService executor;
    public void runExample() throws ParserConfigurationException, IOException, SAXException {
        String loginUser = "mytestuser";
        String loginPass = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
             BufferedWriter reportWriter = new BufferedWriter(new FileWriter("report.txt"))) {

            Class.forName("com.mysql.cj.jdbc.Driver");
            dbCon = DriverManager.getConnection(loginUrl, loginUser, loginPass);
            dbCon.setAutoCommit(false);

            this.writer = writer;
            this.reportWriter = reportWriter;

            queue = new LinkedBlockingQueue<>();
            executor = Executors.newFixedThreadPool(NUM_WORKERS);

            for (int i = 0; i < NUM_WORKERS; i++) {
                executor.submit(new MovieInsertWorker(queue, loginUrl, loginUser, loginPass));
            }

            parseDocument();

            for (int i = 0; i < NUM_WORKERS; i++) {
                queue.put(MovieInsertTask.poisonPill());
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            System.out.println("Finished!");
        } catch (Exception e) {
            try {
                if (dbCon != null) {
                    dbCon.rollback();
                }
            } catch (SQLException rollbackError) {
                rollbackError.printStackTrace();
            }

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
        saxParser.parse("mains243.xml", this);
    }

    @Override
    public void startDocument() {
        initGenreNormalizeMap();

        dir = "";
        existingMovieKeys = new HashSet<>();
        existingMovieIds = new HashSet<>();
        genreMapping = new HashMap<>();

        try {
            String movieSelectQuery = "SELECT id, title, year, director FROM movies";
            try (PreparedStatement statement = dbCon.prepareStatement(movieSelectQuery);
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    existingMovieIds.add(rs.getString("id"));

                    String key = makeMovieKey(
                            rs.getString("title"),
                            rs.getInt("year"),
                            rs.getString("director")
                    );
                    existingMovieKeys.add(key);
                }
            }

            String genreSelectQuery = "SELECT id, name FROM genres";
            try (PreparedStatement genStatement = dbCon.prepareStatement(genreSelectQuery);
                 ResultSet genrs = genStatement.executeQuery()) {

                while (genrs.next()) {
                    genreMapping.put(
                            genrs.getString("name").toLowerCase().trim(),
                            genrs.getInt("id")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGenreNormalizeMap() {
        genreNormalizeMap = new HashMap<>();

        genreNormalizeMap.put("actn", "Action");
        genreNormalizeMap.put("act", "Action");
        genreNormalizeMap.put("axtn", "Action");

        genreNormalizeMap.put("advt", "Adventure");
        genreNormalizeMap.put("adct", "Adventure");
        genreNormalizeMap.put("adctx", "Adventure");

        genreNormalizeMap.put("biop", "Biography");
        genreNormalizeMap.put("biog", "Biography");
        genreNormalizeMap.put("bio", "Biography");
        genreNormalizeMap.put("biopp", "Biography");
        genreNormalizeMap.put("biopx", "Biography");
        genreNormalizeMap.put("biob", "Biography");

        genreNormalizeMap.put("cart", "Animation");

        genreNormalizeMap.put("comd", "Comedy");
        genreNormalizeMap.put("comdx", "Comedy");

        genreNormalizeMap.put("crim", "Crime");

        genreNormalizeMap.put("docu", "Documentary");
        genreNormalizeMap.put("duco", "Documentary");
        genreNormalizeMap.put("ducu", "Documentary");
        genreNormalizeMap.put("dicu", "Documentary");

        genreNormalizeMap.put("dram", "Drama");
        genreNormalizeMap.put("draam", "Drama");
        genreNormalizeMap.put("dramn", "Drama");
        genreNormalizeMap.put("dramd", "Drama");
        genreNormalizeMap.put("dram>", "Drama");
        genreNormalizeMap.put("ram", "Drama");

        genreNormalizeMap.put("faml", "Family");
        genreNormalizeMap.put("fant", "Fantasy");
        genreNormalizeMap.put("hist", "History");
        genreNormalizeMap.put("horr", "Horror");
        genreNormalizeMap.put("hor", "Horror");
        genreNormalizeMap.put("h", "Horror");
        genreNormalizeMap.put("h**", "Horror");
        genreNormalizeMap.put("h0", "Horror");

        genreNormalizeMap.put("musc", "Music");
        genreNormalizeMap.put("muusc", "Music");
        genreNormalizeMap.put("muscl", "Musical");

        genreNormalizeMap.put("myst", "Mystery");
        genreNormalizeMap.put("mystp", "Mystery");

        genreNormalizeMap.put("romt", "Romance");
        genreNormalizeMap.put("ront", "Romance");
        genreNormalizeMap.put("romtx", "Romance");

        genreNormalizeMap.put("scfi", "Sci-Fi");
        genreNormalizeMap.put("scif", "Sci-Fi");
        genreNormalizeMap.put("s.f.", "Sci-Fi");
        genreNormalizeMap.put("sxfi", "Sci-Fi");

        genreNormalizeMap.put("susp", "Thriller");
        genreNormalizeMap.put("sctn", "Thriller");

        genreNormalizeMap.put("west", "Western");
        genreNormalizeMap.put("west1", "Western");

        genreNormalizeMap.put("sports", "Sport");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = new StringBuilder();

        if (qName.equalsIgnoreCase("film")) {
            tempMov = new Movie();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = tempVal.toString().trim();

        if (qName.equalsIgnoreCase("dirname")) {
            dir = value;
        } else if (qName.equalsIgnoreCase("t")) {
            tempMov.setTitle(value);
        } else if (qName.equalsIgnoreCase("year")) {
            try {
                int yearnum = Integer.parseInt(value);
                tempMov.setYear(yearnum);
            } catch (NumberFormatException e) {
                tempMov.setInvalid();
                tempMov.setErrInfo(value);
            }
        } else if (qName.equalsIgnoreCase("fid") || qName.equalsIgnoreCase("filmed")) {
            tempMov.setMovieID(value);
        } else if (qName.equalsIgnoreCase("cat")) {
            tempMov.setGenres(value);
        } else if (qName.equalsIgnoreCase("film")) {
            tempMov.setDir(dir);

            if (tempMov.isValid()) {
                try {
                    writer.write(tempMov.toString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                executeInsertQuery(tempMov);
            } else {
                try {
                    reportWriter.write("Invalid number input for " + tempMov.getTitle()
                            + ". Parsed year: " + tempMov.getErrInfo()
                            + ", expecting integer.\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void executeInsertQuery(Movie mov) {
        String currKey = makeMovieKey(mov.getTitle(), mov.getYear(), mov.getDirector());

        try {
            if (mov.getMovieID() == null || mov.getMovieID().trim().isEmpty()) {
                reportWriter.write("SKIPPED: Movie '" + mov.getTitle() + "' has no FID (ID).\n");
                return;
            }

            if (existingMovieIds.contains(mov.getMovieID())) {
                reportWriter.write("DUPLICATE ID SKIPPED: " + mov.getMovieID()
                        + " for movie " + mov.getTitle() + "\n");
                return;
            }

            if (existingMovieKeys.contains(currKey)) {
                reportWriter.write("DUPLICATE SKIPPED: " + mov.getTitle()
                        + " (" + mov.getYear() + ")\n");
                return;
            }

            List<Integer> genreIds = getOrCreateGenreIds(mov);
            queue.put(new MovieInsertTask(mov, genreIds));
            existingMovieIds.add(mov.getMovieID());
            existingMovieKeys.add(currKey);
        } catch (Exception e) {
            try {
                reportWriter.write("Fail in insert: " + mov.getTitle()
                        + " (" + mov.getYear() + " " + mov.getMovieID() + ")\n");
            } catch (IOException i) {
                i.printStackTrace();
            }

            e.printStackTrace();
            System.out.println("fail when inserting movie: " + mov);
        }
    }

    private String makeMovieKey(String title, int year, String director) {
        return (title + year + director).toLowerCase().trim();
    }

    private List<Integer> getOrCreateGenreIds(Movie mov) {
        List<Integer> genreIds = new ArrayList<>();

        ArrayList<String> genres = mov.getGenres();
        if (genres == null) return genreIds;

        for (String genre : genres) {
            String rawGenre = genre.trim();
            if (rawGenre.isEmpty()) continue;

            String normGenre = rawGenre.toLowerCase();
            String canonicalGenre = genreNormalizeMap.get(normGenre);

            if (canonicalGenre == null) {
                canonicalGenre = rawGenre;
            }

            String genreKey = canonicalGenre.toLowerCase().trim();

            Integer genreId = genreMapping.get(genreKey);

            if (genreId == null) {
                genreId = insertNewGenre(canonicalGenre);

                if (genreId != null) {
                    genreMapping.put(genreKey, genreId);
                }
            }

            if (genreId != null) {
                genreIds.add(genreId);
            }
        }

        return genreIds;
    }
    private Integer insertNewGenre(String genreName) {
        String query = "INSERT INTO genres (name) VALUES (?)";

        try (PreparedStatement statement = dbCon.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, genreName);
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    reportWriter.write("NEW GENRE INSERTED: " + genreName + "\n");
                    dbCon.commit();
                    return id;
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        MainXMLParser saxParserExample = new MainXMLParser();
        saxParserExample.runExample();
    }
}

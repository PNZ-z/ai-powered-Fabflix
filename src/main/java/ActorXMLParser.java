import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ActorXMLParser extends DefaultHandler {
    private static final int NUM_WORKERS = 4;

    private final String loginUser = "mytestuser";
    private final String loginPass = "My6$Password";
    private final String loginUrl =
            "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";

    private StringBuilder tempVal;
    private String currStageName;
    private String currDob;

    private BufferedWriter reportWriter;
    private Connection dbCon;

    private Map<String, String> starNameToId;
    private Set<String> existingStarIds;
    private int nextActorIdNumber;

    private BlockingQueue<ActorInsertTask> queue;
    private ExecutorService executor;

    public void runExample() throws ParserConfigurationException, IOException, SAXException {
        long startTime = System.nanoTime();

        try (BufferedWriter reportWriter = new BufferedWriter(new FileWriter("actor_report.txt"))) {
            Class.forName("com.mysql.cj.jdbc.Driver");

            dbCon = DriverManager.getConnection(loginUrl, loginUser, loginPass);
            this.reportWriter = reportWriter;

            queue = new LinkedBlockingQueue<>(5000);
            executor = Executors.newFixedThreadPool(NUM_WORKERS);

            for (int i = 0; i < NUM_WORKERS; i++) {
                executor.submit(new ActorInsertWorker(queue, loginUrl, loginUser, loginPass));
            }

            parseDocument();

            for (int i = 0; i < NUM_WORKERS; i++) {
                queue.put(ActorInsertTask.poisonPill());
            }

            executor.shutdown();

            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.out.println("Timed out waiting for actor workers.");
            }

            long endTime = System.nanoTime();
            System.out.println("Finished actor parsing!");
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

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream("actors63.xml"),
                "UTF-8")) {
            InputSource inputSource = new InputSource(reader);
            inputSource.setEncoding("UTF-8");
            saxParser.parse(inputSource, this);
        }
    }

    @Override
    public void startDocument() {
        starNameToId = new HashMap<>();
        existingStarIds = new HashSet<>();

        try {
            loadExistingStars();
            loadNextActorIdNumber();
        } catch (Exception e) {
            e.printStackTrace();
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
                starNameToId.put(normalizeKey(starName), starId);
            }
        }
    }

    private void loadNextActorIdNumber() throws SQLException {
        String query =
                "SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) AS max_id " +
                        "FROM stars " +
                        "WHERE id LIKE 'AC%'";

        try (PreparedStatement statement = dbCon.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                if (rs.wasNull()) {
                    nextActorIdNumber = 1;
                } else {
                    nextActorIdNumber = maxId + 1;
                }
            } else {
                nextActorIdNumber = 1;
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = new StringBuilder();

        if (qName.equalsIgnoreCase("actor")) {
            currStageName = null;
            currDob = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = tempVal.toString().trim();

        if (qName.equalsIgnoreCase("stagename")) {
            currStageName = value;
        } else if (qName.equalsIgnoreCase("dob")) {
            currDob = value;
        } else if (qName.equalsIgnoreCase("actor")) {
            processActorRecord();
        }
    }

    private void processActorRecord() {
        try {
            if (isBlank(currStageName)) {
                reportWriter.write("SKIPPED: actor entry has blank stagename.\n");
                return;
            }

            String cleanStageName = currStageName.trim();
            Integer birthYear = parseBirthYear(currDob);

            String starKey = normalizeKey(cleanStageName);
            String existingStarId = starNameToId.get(starKey);

            if (existingStarId != null) {
                return;
            }

            String newStarId = generateNewActorId();

            if (birthYear == null && !isUnknownBirthYear(currDob)) {
                reportWriter.write("INVALID DOB SKIPPED: " + currDob
                        + " for new actor " + cleanStageName + "\n");
            }

            starNameToId.put(starKey, newStarId);
            queue.put(ActorInsertTask.insertStar(newStarId, cleanStageName, birthYear));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean isUnknownBirthYear(String dob) {
        if (isBlank(dob)) {
            return true;
        }

        String cleanDob = dob.trim().toLowerCase();
        return cleanDob.equals("*") || cleanDob.equals("n.a.");
    }
    private String generateNewActorId() {
        String actorId;

        do {
            actorId = String.format("AC%07d", nextActorIdNumber);
            nextActorIdNumber++;
        } while (existingStarIds.contains(actorId));

        existingStarIds.add(actorId);
        return actorId;
    }

    private Integer parseBirthYear(String dob) {
        if (isBlank(dob)) {
            return null;
        }

        String cleanDob = dob.trim().toLowerCase();

        if (cleanDob.equals("*") || cleanDob.equals("n.a.")) {
            return null;
        }

        if (!cleanDob.matches("\\d{1,4}")) {
            return null;
        }

        return Integer.parseInt(cleanDob);
    }

    private String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        ActorXMLParser parser = new ActorXMLParser();
        parser.runExample();
    }
}


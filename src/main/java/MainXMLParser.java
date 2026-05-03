
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;

import javax.naming.InitialContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;
import javax.sql.DataSource;

public class MainXMLParser extends DefaultHandler {
    static private String movieQuery = "INSERT INTO movies VALUES(?, ?, ?, ?)";
//    List<Employee> employeeList;
    // These variables keep track of the information we have collected from the parser as we encounter XML tags
    // and are used to create a new Employee object when we reach the </employee> tag.
    private String tempVal;
    private Movie tempMov;
    private String dir;
    private BufferedWriter writer;
    private BufferedWriter reportWriter;
    private DataSource dataSource;
    private Connection dbCon;
    private Set<String> existingMovieKeys;
    private HashMap<String, Integer> genreMapping;
    private PreparedStatement movieStatement;
    private PreparedStatement genreStatement;

    public void runExample() throws ParserConfigurationException, IOException, SAXException {
        String loginUser = "mytestuser";
        String loginPass = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
            BufferedWriter reportWriter = new BufferedWriter(new FileWriter("report.txt"))){
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbCon = DriverManager.getConnection(loginUrl,  loginUser, loginPass);
            dbCon.setAutoCommit(false);
            movieStatement = dbCon.prepareStatement(movieQuery);
            String genreQuery = "INSERT INTO genres_in_movies VALUES(?,?)";
            genreStatement = dbCon.prepareStatement(genreQuery);
            this.writer = writer;
            this.reportWriter = reportWriter;
            parseDocument();
            dbCon.commit();
            System.out.println("Finished!");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseDocument() throws ParserConfigurationException, SAXException, IOException {
        // Create a factory that creates new SAX parers
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        // Build a new parser from the factory
        SAXParser saxParser = saxParserFactory.newSAXParser();
        // Parses the file using the overridden methods below
        saxParser.parse("mains243.xml", this);
    }

//    private void printData() {
//        System.out.println("No of Employees '" + employeeList.size() + "'.");
//        employeeList.forEach(System.out::println);
//    }

    // Called once at the beginning of parsing a new document
    @Override
    public void startDocument() {
        dir = "";
        existingMovieKeys = new HashSet<>();
        try{
            String query = "SELECT title, year, director FROM movies";
            PreparedStatement statement = dbCon.prepareStatement(query);
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                String key = (rs.getString("title") + rs.getInt("year") + rs.getString("director")).toLowerCase().trim();
                existingMovieKeys.add(key);
            }

            genreMapping = new HashMap<>();

            String genquery = "SELECT id, name FROM genres";
            ResultSet genrs = statement.executeQuery(genquery);
            while (genrs.next()) {
                genreMapping.put(rs.getString("name").toLowerCase(), rs.getInt("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Overrides the method that is called whenever the parser encounters the starting tag of an XML element
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // Reset the temporary value we are storing
        tempVal = "";
        if (qName.equalsIgnoreCase("film")) {
            // We encountered a new Employee tag and create a new instance of employee
            tempMov = new Movie();
        }
    }

    // Overrides the method that is called whenever the parser encounters the characters in between XML start and
    // end elements
    @Override
    public void characters(char[] ch, int start, int length) {
        tempVal = new String(ch, start, length);
    }

    // Overrides the method that is called whenever the parser encounters the ending tag of an XML element
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("dirname")) {
            dir = tempVal;
        } else if (qName.equalsIgnoreCase("t")) {
            tempMov.setTitle(tempVal);
        } else if (qName.equalsIgnoreCase("year")) {
            try{
                int yearnum = Integer.parseInt(tempVal);
                tempMov.setYear(yearnum);
            } catch (NumberFormatException e){
                tempMov.setInvalid();
                tempMov.setErrInfo(tempVal);
            }

        }
        else if(qName.equalsIgnoreCase("fid")){
            tempMov.setMovieID(tempVal);
        }
        else if (qName.equalsIgnoreCase("cat")) {
            tempMov.setGenres(tempVal);
        }
        else if (qName.equalsIgnoreCase("film")) {
            tempMov.setDir(dir);
            if(tempMov.isValid()){
                try{
                    writer.write(tempMov.toString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();;
                }
                executeInsertQuery(tempMov);
            }
            else{
                try{
                    reportWriter.write("Invalid number input for " + tempMov.getTitle() + ". Parsed year:"
                            + tempMov.getErrInfo() + ", expecting integer. \n");

                } catch (IOException e) {
                    e.printStackTrace();;
                }
            }

        }
    }
    private void executeInsertQuery(Movie mov){
        String currKey = (mov.getTitle() + mov.getYear() + mov.getDirector()).toLowerCase().trim();
        if(existingMovieKeys.contains(currKey)){
            try{
                reportWriter.write("DUPLICATE SKIPPED: " + mov.getTitle() + " (" + mov.getYear() + ")\n");
            }catch (IOException e){
                e.printStackTrace();
            }
            mov.setInvalid();
        }
        else{
            try{
                movieStatement.setString(1, mov.getMovieID());
                movieStatement.setString(2, mov.getTitle());
                if (mov.isValid()) {
                    movieStatement.setInt(3, mov.getYear());
                } else {
                    movieStatement.setNull(3, java.sql.Types.INTEGER);
                }
                movieStatement.setString(4, mov.getDirector());
                movieStatement.executeUpdate();
                existingMovieKeys.add(currKey);
                executeInsertGenreQuery(mov);
            } catch (Exception e) {
                try{
                    reportWriter.write("Fail in insert: " + mov.getTitle() + " (" + mov.getYear() + " "+ mov.getMovieID() + ")\n");
                }catch (IOException i){
                    i.printStackTrace();
                }
                e.printStackTrace();
            }

        }

    }
    private void executeInsertGenreQuery(Movie mov) {
        ArrayList<String> genres = mov.getGenres();
        if (genres == null) return;

        for (String genre : genres) {
            String normGenre = genre.toLowerCase().trim();
            if (normGenre.isEmpty()) continue;

            if (!genreMapping.containsKey(normGenre)) {
                String query = "INSERT INTO genres (name) VALUES (?)";
                try (PreparedStatement ins = dbCon.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    ins.setString(1, normGenre);
                    ins.executeUpdate();
                    try (ResultSet rs = ins.getGeneratedKeys()) {
                        if (rs.next()) {
                            genreMapping.put(normGenre, rs.getInt(1));
                        }
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            }

            // 2. Link Movie and Genre in 'genres_in_movies'
            try {
                Integer gid = genreMapping.get(normGenre);
                if (gid != null) {
                    genreStatement.setInt(1, gid);
                    genreStatement.setString(2, mov.getMovieID()); // Link to Movie ID
                    genreStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        MainXMLParser saxParserExample = new MainXMLParser();
        saxParserExample.runExample();
    }
}

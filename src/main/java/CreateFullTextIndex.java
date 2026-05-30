import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CreateFullTextIndex {
    private static final String MYSQL_USER = "mytestuser";
    private static final String MYSQL_PASSWORD = "My6$Password";
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/moviedb";

    private static final String INDEX_NAME = "ft_movie_title";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection connection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
            if (hasMovieTitleFullTextIndex(connection)) {
                System.out.println("Full-text index already exists: " + INDEX_NAME);
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE movies ADD FULLTEXT INDEX " + INDEX_NAME + " (title)");
            }

            System.out.println("Created full-text index: " + INDEX_NAME);
        }
    }

    private static boolean hasMovieTitleFullTextIndex(Connection connection) throws Exception {
        String query = "SELECT 1 " +
                "FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = 'movies' " +
                "AND index_name = ? " +
                "AND index_type = 'FULLTEXT' " +
                "AND column_name = 'title' " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, INDEX_NAME);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
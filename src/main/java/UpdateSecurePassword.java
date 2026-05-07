import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class UpdateSecurePassword {

    /*
     * This program encrypts every password in the Customers table in the moviedb database.
     *
     * You should only run this program **once**, because this program encrypts and replaces the existing password
     *  replace them. If you run it more again it will encrypt the already encrypted password then replace it.
     */
    public static void main(String[] args) throws Exception {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             Statement statement = connection.createStatement()) {
            // Changes the customers table password column from VARCHAR(20) to VARCHAR(128)
            String alterQuery = "ALTER TABLE customers MODIFY COLUMN password VARCHAR(128)";
            int alterResult = statement.executeUpdate(alterQuery);
            System.out.println("Altering customers table schema completed. " + alterResult + " rows affected.");
            ArrayList<String> updateQueryList = new ArrayList<>();

            // Gets the ID and password for each customer
            try (ResultSet resultSet = statement.executeQuery("SELECT id, password FROM customers")) {

                // We use the StrongPasswordEncryptor from jasypt library (Java Simplified Encryption).
                // It uses SHA-256 algorithm and 10,000 iterations to calculate the encrypted password.
                PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                System.out.println("Encrypting passwords. This might take a while.");
                while (resultSet.next()) {
                    // Gets the ID and plain text password from current table
                    String id = resultSet.getString("id");
                    String password = resultSet.getString("password");

                    // Encrypts the password and adds a update query to the list
                    String encryptedPassword = passwordEncryptor.encryptPassword(password);
                    String updateQuery =
                            String.format(
                                    "UPDATE customers SET password='%s' WHERE id=%s;",
                                    encryptedPassword,
                                    id);
                    updateQueryList.add(updateQuery);
                }
            }

            // Executes the update queries to update the password
            System.out.println("Overwriting plain text passwords with encrypted ones.");
            int affectedRowsCount = 0;
            for (String updateQuery : updateQueryList) {
                int updateResult = statement.executeUpdate(updateQuery);
                affectedRowsCount += updateResult;
            }
            System.out.println("Updating password completed. " + affectedRowsCount + " rows affected.");
            System.out.println("Complete.");
        }
    }

}

import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class UpdateEmployeeSecurePassword {

    /*
     * This program encrypts every password in the employees table.
     *
     * Run this program only once.
     * If you run it again, it will encrypt an already encrypted password.
     */
    public static void main(String[] args) throws Exception {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             Statement statement = connection.createStatement()) {

            String alterQuery = "ALTER TABLE employees MODIFY COLUMN password VARCHAR(128)";
            int alterResult = statement.executeUpdate(alterQuery);
            System.out.println("Altering employees table schema completed. " + alterResult + " rows affected.");

            ArrayList<String> updateQueryList = new ArrayList<>();

            try (ResultSet resultSet = statement.executeQuery("SELECT email, password FROM employees")) {
                PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

                System.out.println("Encrypting employee passwords. This might take a while.");

                while (resultSet.next()) {
                    String email = resultSet.getString("email");
                    String password = resultSet.getString("password");

                    String encryptedPassword = passwordEncryptor.encryptPassword(password);

                    String updateQuery = String.format(
                            "UPDATE employees SET password='%s' WHERE email='%s';",
                            encryptedPassword,
                            email
                    );

                    updateQueryList.add(updateQuery);
                }
            }

            System.out.println("Overwriting plain text employee passwords with encrypted ones.");

            int affectedRowsCount = 0;
            for (String updateQuery : updateQueryList) {
                int updateResult = statement.executeUpdate(updateQuery);
                affectedRowsCount += updateResult;
            }

            System.out.println("Updating employee password completed. " + affectedRowsCount + " rows affected.");
            System.out.println("Complete.");
        }
    }
}
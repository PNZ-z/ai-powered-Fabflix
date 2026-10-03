package com.fabflix.boot.dev;

import com.fabflix.boot.FabflixApplication;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class CreateLocalCustomer {
    private static final String DEVELOPMENT_CARD_ID = "TEST-CARD-0001";

    private CreateLocalCustomer() {
    }

    public static void main(String[] args) {
        CustomerInput input = CustomerInput.from(args);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FabflixApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("local")
                .run()) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(
                    context.getBean(org.springframework.transaction.PlatformTransactionManager.class));

            Boolean created = transactionTemplate.execute(status -> createCustomer(jdbcTemplate, input));
            if (Boolean.TRUE.equals(created)) {
                System.out.printf("Created local customer: %s%n", input.email);
            } else {
                System.out.printf("Local customer already exists: %s%n", input.email);
            }
        }
    }

    private static boolean createCustomer(JdbcTemplate jdbcTemplate, CustomerInput input) {
        Integer existingCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE email = ?",
                Integer.class,
                input.email);
        if (existingCustomers != null && existingCustomers > 0) {
            return false;
        }

        jdbcTemplate.update(
                "INSERT INTO credit_cards (id, first_name, last_name, expiration) "
                        + "SELECT ?, ?, ?, ? WHERE NOT EXISTS "
                        + "(SELECT 1 FROM credit_cards WHERE id = ?)",
                DEVELOPMENT_CARD_ID,
                input.firstName,
                input.lastName,
                java.sql.Date.valueOf("2035-12-31"),
                DEVELOPMENT_CARD_ID);

        String encryptedPassword = new StrongPasswordEncryptor().encryptPassword(input.password);
        jdbcTemplate.update(
                "INSERT INTO customers "
                        + "(first_name, last_name, credit_card_id, address, email, password) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                input.firstName,
                input.lastName,
                DEVELOPMENT_CARD_ID,
                input.address,
                input.email,
                encryptedPassword);
        return true;
    }

    private static final class CustomerInput {
        private final String email;
        private final String password;
        private final String firstName;
        private final String lastName;
        private final String address;

        private CustomerInput(String email, String password, String firstName, String lastName, String address) {
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.address = address;
        }

        private static CustomerInput from(String[] args) {
            if (args.length != 0 && args.length != 5) {
                throw new IllegalArgumentException(
                        "Usage: CreateLocalCustomer [email password firstName lastName address]");
            }
            if (args.length == 0) {
                return new CustomerInput(
                        "test.user@fabflix.local",
                        "test-password",
                        "Test",
                        "User",
                        "2 Local Development Way");
            }
            return new CustomerInput(args[0], args[1], args[2], args[3], args[4]);
        }
    }
}

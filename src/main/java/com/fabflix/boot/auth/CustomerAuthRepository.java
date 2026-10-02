package com.fabflix.boot.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerAuthRepository {
    static final String LOGIN_SQL =
            "SELECT password FROM customers WHERE email = ? LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    public CustomerAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findPasswordHashByEmail(String email) {
        List<String> passwordHashes = jdbcTemplate.query(
                LOGIN_SQL,
                (resultSet, rowNumber) -> resultSet.getString("password"),
                email
        );
        return passwordHashes.stream().findFirst();
    }
}

package com.fabflix.boot.admin;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepository {
    static final String LOGIN_SQL = "SELECT password, fullname FROM employees WHERE email = ? LIMIT 1";
    private final JdbcTemplate jdbcTemplate;

    public EmployeeRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    Optional<EmployeeRecord> findByEmail(String email) {
        List<EmployeeRecord> rows = jdbcTemplate.query(LOGIN_SQL,
                (rs, rowNum) -> new EmployeeRecord(rs.getString("password"), rs.getString("fullname")), email);
        return rows.stream().findFirst();
    }
}

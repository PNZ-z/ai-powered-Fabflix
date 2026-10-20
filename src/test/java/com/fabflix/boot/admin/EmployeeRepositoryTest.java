package com.fabflix.boot.admin;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeRepositoryTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsPasswordAndFullNameForBoundEmail() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("password")).thenReturn("hashed-password");
        when(resultSet.getString("fullname")).thenReturn("Jane Doe");
        when(jdbcTemplate.query(eq(EmployeeRepository.LOGIN_SQL), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        Optional<EmployeeRecord> record =
                new EmployeeRepository(jdbcTemplate).findByEmail("jane@example.com");

        assertTrue(record.isPresent());
        assertEquals("hashed-password", record.get().getPasswordHash());
        assertEquals("Jane Doe", record.get().getFullName());
        verify(jdbcTemplate).query(eq(EmployeeRepository.LOGIN_SQL), any(RowMapper.class),
                eq("jane@example.com"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyWhenNoEmployeeMatchesEmail() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(eq(EmployeeRepository.LOGIN_SQL), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        Optional<EmployeeRecord> record =
                new EmployeeRepository(jdbcTemplate).findByEmail("unknown@example.com");

        assertTrue(record.isEmpty());
    }
}

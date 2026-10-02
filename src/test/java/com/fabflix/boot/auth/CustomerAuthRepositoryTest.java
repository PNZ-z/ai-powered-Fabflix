package com.fabflix.boot.auth;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

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

class CustomerAuthRepositoryTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsPasswordHashUsingBoundEmail() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("password")).thenReturn("encrypted-password");
        when(jdbcTemplate.query(
                eq(CustomerAuthRepository.LOGIN_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        String passwordHash = new CustomerAuthRepository(jdbcTemplate)
                .findPasswordHashByEmail("viewer@example.com")
                .orElseThrow();

        assertEquals("encrypted-password", passwordHash);
        verify(jdbcTemplate).query(
                eq(CustomerAuthRepository.LOGIN_SQL),
                any(RowMapper.class),
                eq("viewer@example.com")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyWhenCustomerDoesNotExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(
                eq(CustomerAuthRepository.LOGIN_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(Collections.emptyList());

        assertTrue(new CustomerAuthRepository(jdbcTemplate)
                .findPasswordHashByEmail("missing@example.com")
                .isEmpty());
    }
}

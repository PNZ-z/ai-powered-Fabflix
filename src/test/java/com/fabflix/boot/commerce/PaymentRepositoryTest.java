package com.fabflix.boot.commerce;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void checksCardUsingAllSubmittedFields() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PaymentRequest request = paymentRequest();
        when(jdbcTemplate.query(
                eq(PaymentRepository.CARD_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of(1));

        assertTrue(new PaymentRepository(jdbcTemplate).paymentCardMatches(request));
        verify(jdbcTemplate).query(
                eq(PaymentRepository.CARD_SQL),
                any(RowMapper.class),
                eq("1234"), eq("Ada"), eq("Lovelace"), eq("2030-01-01")
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findsCustomerAndExpandsCartQuantitiesIntoBatch() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("id")).thenReturn(42);
        when(jdbcTemplate.query(
                eq(PaymentRepository.CUSTOMER_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });
        PaymentRepository repository = new PaymentRepository(jdbcTemplate);

        assertEquals(42, repository.findCustomerIdByEmail("viewer@example.com").orElseThrow());
        repository.insertSales(42, Map.of("tt0001", 2, "tt0002", 1));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<Object[]>> batchCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(eq(PaymentRepository.INSERT_SALE_SQL), batchCaptor.capture());
        assertEquals(3, batchCaptor.getValue().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsMissingCardAndCustomer() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());
        PaymentRepository repository = new PaymentRepository(jdbcTemplate);

        assertFalse(repository.paymentCardMatches(paymentRequest()));
        assertTrue(repository.findCustomerIdByEmail("missing@example.com").isEmpty());
    }

    private PaymentRequest paymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setCreditCardNumber("1234");
        request.setExpirationDate("2030-01-01");
        return request;
    }
}

package com.fabflix.boot.commerce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    static final String CARD_SQL =
            "SELECT 1 FROM credit_cards " +
                    "WHERE id = ? AND first_name = ? AND last_name = ? AND expiration = ?";
    static final String CUSTOMER_SQL =
            "SELECT id FROM customers WHERE email = ?";
    static final String INSERT_SALE_SQL =
            "INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, CURDATE())";

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean paymentCardMatches(PaymentRequest request) {
        return !jdbcTemplate.query(
                CARD_SQL,
                (resultSet, rowNumber) -> resultSet.getInt(1),
                request.getCreditCardNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getExpirationDate()
        ).isEmpty();
    }

    public Optional<Integer> findCustomerIdByEmail(String email) {
        return jdbcTemplate.query(
                CUSTOMER_SQL,
                (resultSet, rowNumber) -> resultSet.getInt("id"),
                email
        ).stream().findFirst();
    }

    public void insertSales(int customerId, Map<String, Integer> cart) {
        List<Object[]> sales = new ArrayList<>();
        cart.forEach((movieId, quantity) -> {
            for (int index = 0; index < quantity; index++) {
                sales.add(new Object[]{customerId, movieId});
            }
        });
        jdbcTemplate.batchUpdate(INSERT_SALE_SQL, sales);
    }
}

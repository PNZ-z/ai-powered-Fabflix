package com.fabflix.boot.commerce;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

// One sale line of a placed order. Stored in the session as part of
// OrderSummary, so it must stay Serializable.
public class OrderLine implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String movieId;
    private final String movieTitle;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public OrderLine(
            String movieId,
            String movieTitle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    @JsonProperty("movie_id")
    public String getMovieId() {
        return movieId;
    }

    @JsonProperty("movie_title")
    public String getMovieTitle() {
        return movieTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    @JsonProperty("unit_price")
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @JsonProperty("line_total")
    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}

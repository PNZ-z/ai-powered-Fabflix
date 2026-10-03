package com.fabflix.boot.commerce;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CartItem {
    private final String movieId;
    private final String movieTitle;
    private final int quantity;
    private final BigDecimal price;

    public CartItem(String movieId, String movieTitle, int quantity, BigDecimal price) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.quantity = quantity;
        this.price = price;
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

    public BigDecimal getPrice() {
        return price;
    }
}

package com.fabflix.boot.movie;

import org.springframework.stereotype.Component;

@Component
public class MovieListQueryFactory {
    public MovieListQuery fromRequest(MovieListRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        String title = trimToNull(request.getTitle());
        String year = trimToNull(request.getYear());
        String director = trimToNull(request.getDirector());
        String star = trimToNull(request.getStar());
        String genre = trimToNull(request.getGenre());
        String startsWith = trimToNull(request.getStartsWith());
        String pageNumberValue = trimToNull(request.getPageNumber());
        String pageSizeValue = trimToNull(request.getPageSize());
        String sort1 = trimToNull(request.getSort1());
        String dir1 = trimToNull(request.getDir1());
        String sort2 = trimToNull(request.getSort2());
        String dir2 = trimToNull(request.getDir2());

        int pageSize = 10;
        if (isInteger(pageSizeValue)) {
            pageSize = Integer.parseInt(pageSizeValue);
        }

        int pageNumber = 0;
        if (isInteger(pageNumberValue)) {
            pageNumber = Integer.parseInt(pageNumberValue);
        }

        sort1 = validateSortField(sort1, "title");
        dir1 = validateSortDirection(dir1);
        sort2 = validateSortField(sort2, "rating");
        dir2 = validateSortDirection(dir2);

        if (sort1.equals(sort2)) {
            sort2 = sort1.equals("title") ? "rating" : "title";
        }

        return new MovieListQuery(
                title,
                year,
                director,
                star,
                genre,
                startsWith,
                pageSize,
                pageNumber * pageSize,
                sort1,
                dir1,
                sort2,
                dir2
        );
    }

    boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }

        return value;
    }

    String validateSortField(String field, String defaultValue) {
        if ("rating".equalsIgnoreCase(field)) {
            return "rating";
        }
        if ("title".equalsIgnoreCase(field)) {
            return "title";
        }

        return defaultValue;
    }

    String validateSortDirection(String field) {
        if ("DESC".equalsIgnoreCase(field)) {
            return "DESC";
        }

        return "ASC";
    }
}

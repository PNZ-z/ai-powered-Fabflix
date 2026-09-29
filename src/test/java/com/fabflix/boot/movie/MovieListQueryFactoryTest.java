package com.fabflix.boot.movie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MovieListQueryFactoryTest {
    @Test
    void usesLegacyDefaultsForMissingPaginationAndSorting() {
        MovieListRequest movieListRequest = new MovieListRequest();
        MovieListQueryFactory movieListQueryFactory = new MovieListQueryFactory();
        MovieListQuery query = movieListQueryFactory.fromRequest(movieListRequest);
        assertEquals(10, query.getPageSize());
        assertEquals(0, query.getOffset());
        assertEquals("title", query.getSort1());
        assertEquals("rating", query.getSort2());
        assertEquals("ASC", query.getDir1());
        assertEquals("ASC", query.getDir2());
    }

    @Test
    void trimsEmptySearchValuesToNull() {
        MovieListRequest request = new MovieListRequest();
        request.setTitle("   ");
        request.setYear("\t");
        request.setDirector("  ");
        request.setStar("\n");
        request.setGenre("");
        request.setStartsWith("   ");

        MovieListQueryFactory factory = new MovieListQueryFactory();

        MovieListQuery query = factory.fromRequest(request);

        assertNull(query.getTitle());
        assertNull(query.getYear());
        assertNull(query.getDirector());
        assertNull(query.getStar());
        assertNull(query.getGenre());
        assertNull(query.getStartsWith());
    }

    @Test
    void acceptsOnlySafeSortFieldsAndDirections() {
        MovieListRequest unsafeRequest = new MovieListRequest();
        unsafeRequest.setSort1("year");
        unsafeRequest.setDir1("sideways");
        unsafeRequest.setSort2("director");
        unsafeRequest.setDir2("down");

        MovieListQueryFactory factory = new MovieListQueryFactory();

        MovieListQuery safeQuery = factory.fromRequest(unsafeRequest);

        assertEquals("title", safeQuery.getSort1());
        assertEquals("ASC", safeQuery.getDir1());
        assertEquals("rating", safeQuery.getSort2());
        assertEquals("ASC", safeQuery.getDir2());

        MovieListRequest duplicateRequest = new MovieListRequest();
        duplicateRequest.setSort1("RATING");
        duplicateRequest.setDir1("desc");
        duplicateRequest.setSort2("rating");
        duplicateRequest.setDir2("DESC");

        MovieListQuery distinctQuery = factory.fromRequest(duplicateRequest);

        assertEquals("rating", distinctQuery.getSort1());
        assertEquals("DESC", distinctQuery.getDir1());
        assertEquals("title", distinctQuery.getSort2());
        assertEquals("DESC", distinctQuery.getDir2());
    }

    @Test
    void detectsIntegerValues() {
        MovieListQueryFactory factory = new MovieListQueryFactory();

        assertTrue(factory.isInteger("0"));
        assertTrue(factory.isInteger("25"));
        assertTrue(factory.isInteger("-1"));
        assertFalse(factory.isInteger(null));
        assertFalse(factory.isInteger(""));
        assertFalse(factory.isInteger("   "));
        assertFalse(factory.isInteger("1.5"));
        assertFalse(factory.isInteger("ten"));
        assertFalse(factory.isInteger("2147483648"));
    }

    @Test
    void acceptsDifferentSortingsInFields() {
        MovieListQueryFactory factory = new MovieListQueryFactory();
        MovieListRequest request = new MovieListRequest();
        request.setSort1("title");
        request.setSort2("title");

        MovieListQuery query = factory.fromRequest(request);

        assertEquals("title", query.getSort1());
        assertEquals("rating", query.getSort2());

        MovieListRequest request2 = new MovieListRequest();
        request2.setSort1("rating");
        request2.setSort2("rating");

        MovieListQuery query2 = factory.fromRequest(request2);

        assertEquals("rating", query2.getSort1());
        assertEquals("title", query2.getSort2());
    }
}

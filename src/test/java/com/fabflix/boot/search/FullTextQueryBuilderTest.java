package com.fabflix.boot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FullTextQueryBuilderTest {
    private final FullTextQueryBuilder builder = new FullTextQueryBuilder();

    @Test void tokenizesAndRemovesUnsafeBooleanSyntax() {
        assertEquals("+Alien* +movie*", builder.build(" Alien!  movie "));
    }

    @Test void returnsEmptyForMissingOrPunctuationOnlyInput() {
        assertEquals("", builder.build(null));
        assertEquals("", builder.build(" !!! "));
    }
}

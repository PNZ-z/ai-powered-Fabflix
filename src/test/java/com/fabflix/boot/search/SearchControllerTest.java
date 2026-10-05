package com.fabflix.boot.search;

import java.util.List;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(SearchApiExceptionHandler.class)
class SearchControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean SearchService searchService;

    @Test void returnsAutocompleteLegacyContract() throws Exception {
        when(searchService.autocomplete("Alien")).thenReturn(
                List.of(new AutocompleteSuggestion("tt0001", "Alien")));
        mockMvc.perform(get("/api/boot/autocomplete").param("query", "Alien")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Alien"))
                .andExpect(jsonPath("$[0].data.movie_id").value("tt0001"));
    }

    @Test void recordsQueryForJumpBackAfterResults() throws Exception {
        when(searchService.search(any())).thenReturn(List.of(
                new com.fabflix.boot.movie.MovieSummary("tt1", "Alien", 1979,
                        "Ridley Scott", "Horror", 8.5f, List.of())));
        MockHttpSession session = authenticatedSession();
        mockMvc.perform(get("/api/boot/full-text-search")
                        .queryParam("query", "Alien").queryParam("pageSize", "10")
                        .session(session))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals("query=Alien&pageSize=10",
                session.getAttribute("lastMovieQuery"));
    }

    @Test void redirectsToStoredOrDefaultMovieList() throws Exception {
        MockHttpSession stored = authenticatedSession();
        stored.setAttribute("lastMovieQuery", "query=Alien&pageSize=10");
        mockMvc.perform(get("/api/boot/jump-back").session(stored))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/movieList.html?query=Alien&pageSize=10"));

        mockMvc.perform(get("/api/boot/jump-back").session(authenticatedSession()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "/movieList.html?title=&pageSize=10&pageNumber=0"));
    }

    @Test void returnsSafeDatabaseError() throws Exception {
        when(searchService.autocomplete("Alien")).thenThrow(new RuntimeException("secret db details"));
        mockMvc.perform(get("/api/boot/autocomplete").param("query", "Alien")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Unable to complete search request."));
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        return session;
    }
}

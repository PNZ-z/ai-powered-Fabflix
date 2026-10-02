package com.fabflix.boot.movie;

import java.util.List;

import com.fabflix.boot.auth.AuthenticatedCustomer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieListController.class)
public class MovieListControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieListService movieListService;

    @Test
    void returnsMoviesUsingLegacyJsonContract() throws Exception {
        MovieSummary movie = new MovieSummary(
                "tt0078748",
                "Alien",
                1979,
                "Ridley Scott",
                "Horror, Sci-Fi",
                8.5F,
                List.of(new MovieSummaryStar("nm0000244", "Sigourney Weaver"))
        );
        when(movieListService.listMovies(org.mockito.ArgumentMatchers.any(MovieListRequest.class)))
                .thenReturn(List.of(movie));

        mockMvc.perform(get("/api/boot/movie-list")
                        .param("title", "Alien")
                        .param("pageNumber", "1")
                        .param("pageSize", "10")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movie_id").value("tt0078748"))
                .andExpect(jsonPath("$[0].movie_title").value("Alien"))
                .andExpect(jsonPath("$[0].movie_year").value(1979))
                .andExpect(jsonPath("$[0].movie_director").value("Ridley Scott"))
                .andExpect(jsonPath("$[0].movie_genres").value("Horror, Sci-Fi"))
                .andExpect(jsonPath("$[0].movie_rating").value(8.5))
                .andExpect(jsonPath("$[0].movie_stars[0].star_id").value("nm0000244"))
                .andExpect(jsonPath("$[0].movie_stars[0].star_name")
                        .value("Sigourney Weaver"));

        ArgumentCaptor<MovieListRequest> requestCaptor =
                ArgumentCaptor.forClass(MovieListRequest.class);
        verify(movieListService).listMovies(requestCaptor.capture());
        assertEquals("Alien", requestCaptor.getValue().getTitle());
        assertEquals("1", requestCaptor.getValue().getPageNumber());
        assertEquals("10", requestCaptor.getValue().getPageSize());
    }

    @Test
    void returnsFailureUsingLegacyErrorContract() throws Exception {
        when(movieListService.listMovies(org.mockito.ArgumentMatchers.any(MovieListRequest.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        mockMvc.perform(get("/api/boot/movie-list")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("database unavailable"));
    }
}

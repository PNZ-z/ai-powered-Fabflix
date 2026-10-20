package com.fabflix.boot.star;

import java.util.List;

import com.fabflix.boot.auth.AuthenticatedCustomer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StarDetailController.class)
@Import(StarApiExceptionHandler.class)
class StarDetailControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StarDetailService starDetailService;

    @Test
    void returnsStarUsingLegacyJsonContract() throws Exception {
        StarDetail star = new StarDetail(
                "nm0000244",
                "Sigourney Weaver",
                1949,
                List.of(new StarDetailMovie("tt0078748", "Alien"))
        );
        when(starDetailService.getStar("nm0000244")).thenReturn(star);

        mockMvc.perform(get("/api/boot/single-star")
                        .param("id", "nm0000244")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.star_id").value("nm0000244"))
                .andExpect(jsonPath("$.star_name").value("Sigourney Weaver"))
                .andExpect(jsonPath("$.star_birth_year").value(1949))
                .andExpect(jsonPath("$.star_movies[0].movie_id").value("tt0078748"))
                .andExpect(jsonPath("$.star_movies[0].movie_title").value("Alien"));

        verify(starDetailService).getStar("nm0000244");
    }

    @Test
    void returnsBadRequestForMissingId() throws Exception {
        when(starDetailService.getStar(null))
                .thenThrow(new IllegalArgumentException("Star id is required."));

        mockMvc.perform(get("/api/boot/single-star")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Star id is required."));
    }

    @Test
    void preservesLegacyNotFoundContract() throws Exception {
        when(starDetailService.getStar("missing-id"))
                .thenThrow(new StarNotFoundException("missing-id"));

        mockMvc.perform(get("/api/boot/single-star")
                        .param("id", "missing-id")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Star not found."));
    }

    @Test
    void returnsInternalServerErrorForUnexpectedFailure() throws Exception {
        when(starDetailService.getStar("nm0000244"))
                .thenThrow(new RuntimeException("database unavailable"));

        mockMvc.perform(get("/api/boot/single-star")
                        .param("id", "nm0000244")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("database unavailable"));
    }
}

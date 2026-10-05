package com.fabflix.boot.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminServiceTest {
    private AdminRepository repository;
    private AdminService service;

    @BeforeEach void setUp() {
        repository = mock(AdminRepository.class);
        service = new AdminService(repository);
    }

    @Test void addsTrimmedStarWithOptionalBirthYear() {
        when(repository.addStar("Jane Doe", 1980)).thenReturn("nm0000010");
        AdminResponse response = service.addStar(" Jane Doe ", "1980");
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().contains("nm0000010"));
        verify(repository).addStar("Jane Doe", 1980);
    }

    @Test void validatesStarInput() {
        assertEquals("fail", service.addStar(" ", null).getStatus());
        assertEquals("fail", service.addStar("Jane", "not-a-year").getStatus());
        assertEquals("fail", service.addStar("Jane", "-1").getStatus());
        verifyNoInteractions(repository);
    }

    @Test void addsMovieWithNormalizedValues() {
        when(repository.addMovie("Title", 2000, "Director", "Star", null, "Drama"))
                .thenReturn(AdminResponse.success("created"));
        AdminResponse response = service.addMovie(" Title ", "2000", " Director ", " Star ", " ", " Drama ");
        assertEquals("success", response.getStatus());
        verify(repository).addMovie("Title", 2000, "Director", "Star", null, "Drama");
    }

    @Test void validatesRequiredMovieFieldsAndYears() {
        assertEquals("fail", service.addMovie(null, "2000", "D", "S", null, "G").getStatus());
        assertEquals("fail", service.addMovie("T", "bad", "D", "S", null, "G").getStatus());
        assertEquals("fail", service.addMovie("T", "2000", "D", "S", "bad", "G").getStatus());
        verifyNoInteractions(repository);
    }
}

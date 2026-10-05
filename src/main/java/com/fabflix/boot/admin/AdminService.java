package com.fabflix.boot.admin;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final AdminRepository repository;
    public AdminService(AdminRepository repository) { this.repository = repository; }

    public AdminResponse addStar(String name, String birthYearText) {
        String normalized = trimToNull(name);
        if (normalized == null) return AdminResponse.failure("Star name cannot be empty.");
        Integer birthYear = parseNonNegative(birthYearText, "Birth year");
        if (birthYearText != null && !birthYearText.trim().isEmpty() && birthYear == null)
            return AdminResponse.failure("Birth year must be a valid non-negative number.");
        String id = repository.addStar(normalized, birthYear);
        return AdminResponse.success("Star inserted successfully. New star ID: " + id);
    }

    public AdminResponse addMovie(String title, String yearText, String director, String starName,
                                  String starBirthYearText, String genreName) {
        title = trimToNull(title); director = trimToNull(director); starName = trimToNull(starName); genreName = trimToNull(genreName);
        if (title == null || yearText == null || director == null || starName == null || genreName == null)
            return AdminResponse.failure("Please fill in all required fields.");
        Integer year = parseNonNegative(yearText, "Movie year");
        if (year == null) return AdminResponse.failure("Movie year must be a valid non-negative number.");
        Integer starBirthYear = parseNonNegative(starBirthYearText, "Star birth year");
        if (starBirthYearText != null && !starBirthYearText.trim().isEmpty() && starBirthYear == null)
            return AdminResponse.failure("Star birth year must be a valid non-negative number.");
        return repository.addMovie(title, year, director, starName, starBirthYear, genreName);
    }

    public List<MetadataTable> metadata() throws Exception { return repository.metadata(); }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseNonNegative(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        try { int number = Integer.parseInt(normalized); return number < 0 ? null : number; }
        catch (NumberFormatException ignored) { return null; }
    }
}

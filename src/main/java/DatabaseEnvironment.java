final class DatabaseEnvironment {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3307/moviedb?useUnicode=true&characterEncoding=UTF-8"
                    + "&connectionCollation=utf8mb4_unicode_ci";

    private DatabaseEnvironment() {
    }

    static String user() {
        return required("FABFLIX_DB_USER");
    }

    static String password() {
        return required("FABFLIX_DB_PASSWORD");
    }

    static String url() {
        String value = System.getenv("FABFLIX_DB_URL");
        return value == null || value.isBlank() ? DEFAULT_URL : value;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not configured");
        }
        return value;
    }
}

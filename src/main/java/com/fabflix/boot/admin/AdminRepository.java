package com.fabflix.boot.admin;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public AdminRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public String addStar(String name, Integer birthYear) {
        String maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM stars", String.class);
        int next = maxId == null ? 1 : Integer.parseInt(maxId.substring(2)) + 1;
        String id = String.format("nm%07d", next);
        jdbcTemplate.update("INSERT INTO stars (id, name, birth_year) VALUES (?, ?, ?)", id, name, birthYear);
        return id;
    }

    public AdminResponse addMovie(String title, int year, String director, String starName,
                                  Integer starBirthYear, String genreName) {
        return jdbcTemplate.execute((ConnectionCallback<AdminResponse>) connection -> {
            try (CallableStatement statement = connection.prepareCall("{CALL add_movie(?, ?, ?, ?, ?, ?)}")) {
                statement.setString(1, title);
                statement.setInt(2, year);
                statement.setString(3, director);
                statement.setString(4, starName);
                if (starBirthYear == null) statement.setNull(5, Types.INTEGER); else statement.setInt(5, starBirthYear);
                statement.setString(6, genreName);
                if (!statement.execute()) return AdminResponse.failure("Stored procedure returned no result set.");
                try (ResultSet rs = statement.getResultSet()) {
                    if (!rs.next()) return AdminResponse.failure("Stored procedure returned no result.");
                    return "success".equals(rs.getString("status"))
                            ? AdminResponse.success(rs.getString("message"))
                            : AdminResponse.failure(rs.getString("message"));
                }
            }
        });
    }

    public List<MetadataTable> metadata() throws Exception {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            List<MetadataTable> result = new ArrayList<>();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<MetadataColumn> columns = new ArrayList<>();
                    try (ResultSet columnRows = metadata.getColumns(connection.getCatalog(), null, tableName, "%")) {
                        while (columnRows.next()) {
                            columns.add(new MetadataColumn(columnRows.getString("COLUMN_NAME"),
                                    columnRows.getString("TYPE_NAME") + "(" + columnRows.getInt("COLUMN_SIZE") + ")"));
                        }
                    }
                    result.add(new MetadataTable(tableName, columns));
                }
            }
            return result;
        }
    }
}

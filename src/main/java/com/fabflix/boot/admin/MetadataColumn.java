package com.fabflix.boot.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataColumn {
    @JsonProperty("column_name") private final String columnName;
    @JsonProperty("column_type") private final String columnType;

    public MetadataColumn(String columnName, String columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }

    public String getColumnName() { return columnName; }
    public String getColumnType() { return columnType; }
}

package com.fabflix.boot.admin;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataTable {
    @JsonProperty("table_name") private final String tableName;
    private final List<MetadataColumn> columns;

    public MetadataTable(String tableName, List<MetadataColumn> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() { return tableName; }
    public List<MetadataColumn> getColumns() { return columns; }
}

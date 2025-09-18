package com.back.simpleDb;

import java.util.ArrayList;
import java.util.List;

public class SqlBuilder {

    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> parameters = new ArrayList<>();

    public void append(CharSequence sql) {
        sqlBuilder.append(sql).append(" ");
    }

    public void append(String sql, Object... params) {
        sqlBuilder.append(sql).append(" ");
        parameters.addAll(List.of(params));
    }

    public void appendIn(String sql, Object... params) {
        String replace = String.join(", ", "?".repeat(params.length).split(""));
        append(sql.replaceFirst("\\?", replace), params);
    }

    public String getSql() {
        return sqlBuilder.toString().strip();
    }

    public List<Object> getParameters() {
        return parameters;
    }
}

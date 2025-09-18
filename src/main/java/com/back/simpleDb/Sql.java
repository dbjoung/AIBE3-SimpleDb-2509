package com.back.simpleDb;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {

    private final MyJdbcTemplate jdbcTemplate;
    private final SqlBuilder sqlBuilder;

    public Sql(Connection connection, boolean devMode) {
        this.sqlBuilder = new SqlBuilder();
        this.jdbcTemplate = new MyJdbcTemplate(connection, devMode);
    }

    public Sql append(String sql) {
        sqlBuilder.append(sql);
        return this;
    }

    public Sql append(String sql, Object... params) {
        sqlBuilder.append(sql, params);
        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        sqlBuilder.appendIn(sql, params);
        return this;
    }

    public long insert() {
        return jdbcTemplate.executeInsert(getSql(), getParameters());
    }

    public int update() {
        return jdbcTemplate.executeUpdate(getSql(), getParameters());
    }

    public int delete() {
        return jdbcTemplate.executeUpdate(getSql(), getParameters());
    }

    public List<Map<String, Object>> selectRows() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> {
            List<Map<String, Object>> result = new ArrayList<>();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    map.put(columnName, rs.getObject(i));
                }

                result.add(map);
            }

            return result;
        });
    }

    public Map<String, Object> selectRow() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Map<String, Object> map = new HashMap<>();

            if (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    map.put(columnName, rs.getObject(i));
                }
            }

            return map;
        });
    }

    public <T> T selectRow(Class<T> cls) {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> {
            try {
                if (rs.next()) {
                    return getInstance(cls, rs);
                }
            } catch (Exception ignore) {
            }

            return null;
        });
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> {
            try {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(getInstance(cls, rs));
                }

                return result;
            } catch (Exception ignore) {
            }

            return null;
        });
    }

    public long selectLong() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> rs.next() ? rs.getLong(1) : -1L);
    }

    public LocalDateTime selectDatetime() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> rs.next() ? rs.getTimestamp(1).toLocalDateTime() : null);
    }

    public String selectString() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> rs.next() ? rs.getString(1) : null);
    }

    public Boolean selectBoolean() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> rs.next() ? rs.getBoolean(1) : null);
    }

    public List<Long> selectLongs() {
        return jdbcTemplate.query(getSql(), getParameters(), rs -> {
            List<Long> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }

            return result;
        });
    }

    private <T> T getInstance(Class<T> cls, ResultSet rs) {
        try {
            T instance = cls.getConstructor().newInstance();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);

                Field field = cls.getDeclaredField(columnName);
                field.setAccessible(true);
                field.set(instance, rs.getObject(i));
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getSql() {
        return sqlBuilder.getSql();
    }

    private List<Object> getParameters() {
        return sqlBuilder.getParameters();
    }
}

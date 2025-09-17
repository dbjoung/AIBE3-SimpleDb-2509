package com.back.simpleDb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// SQL 쿼리를 동적으로 생성, 파라미터 바인딩, DB에 실행하는 클래스.
public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder; // SQL 쿼리 문자열을 저장
    private final List<Object> params; // '?'에 바인딩될 파라미터들을 순서대로 저장

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        this.sqlBuilder = new StringBuilder();
        this.params = new ArrayList<>();
    }

    // SQL 쿼리문 한 줄, 파라미터 추가
    public Sql append(String sqlFragment, Object... params) {
        sqlBuilder.append(sqlFragment).append(" ");
        for (Object param : params) {
            this.params.add(param);
        }
        return this; // 자기자신 반환
    }

    // in (?, ?) 문 만들기
    public Sql appendIn(String sqlFragment, Object... params) {
        if (params.length == 0) {
            sqlBuilder.append(sqlFragment.replace("?", "null")).append(" ");
            return this;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            placeholders.append("?");
            if (i < params.length - 1) {
                placeholders.append(",");
            }
        }

        String newSqlFragment = sqlFragment.replace("?", placeholders.toString());

        sqlBuilder.append(newSqlFragment).append(" ");
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    // SQL 실행 전에 저장된 파라미터들을 '?' 위치에 순서대로 바인딩.
    private void setParams(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i)); // jdbc는 1부터 시작함
        }
    }

    // 개발 모드일 경우, 실행될 최종 SQL과 파라미터를 콘솔에 출력.
    private void logQuery() {
        if (simpleDb.isDevMode()) {
            System.out.println("==== rawSql ====");
            System.out.println(sqlBuilder.toString().trim());
            if (!params.isEmpty()) {
                System.out.println("==== params ====");
                System.out.println(params);
            }
        }
    }

    public long insert() {
        logQuery();
        Connection conn = simpleDb.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS);
            setParams(pstmt);
            pstmt.executeUpdate(); // 쿼리 실행
            rs = pstmt.getGeneratedKeys(); // DB가 돌려준 ID값

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally { // ResultSet, PreparedStatement 자원 역순 닫기
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return -1;
    }

    public int update() {
        logQuery();
        Connection conn = simpleDb.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            setParams(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    public int delete() {
        logQuery();
        Connection conn = simpleDb.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            setParams(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    public void run() {
        logQuery();
        Connection conn = simpleDb.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            setParams(pstmt);
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    // SELECT 쿼리를 실행 시 여러 행의 결과를 `List<Map<String, Object>>` 형태로 반환.
    public List<Map<String, Object>> selectRows() {
        logQuery();
        List<Map<String, Object>> rows = new ArrayList<>();
        Connection conn = simpleDb.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            setParams(pstmt);
            rs = pstmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    if (value instanceof Boolean) {
                        row.put(columnName, value);
                    } else if (value instanceof Short && metaData.getColumnTypeName(i).equals("BIT")) {
                        row.put(columnName, (Short) value == 1);
                    } else {
                        row.put(columnName, value);
                    }
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return rows;
    }

    // select 쿼리를 실행하여 단 하나의 행(row) 결과를 `Map<String, Object>` 형태로 반환.
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    // select 결과를 지정된 클래스의 객체 리스트로 변환.
    public <T> List<T> selectRows(Class<T> cls) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        List<Map<String, Object>> rows = selectRows();
        List<T> dtoList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            try {
                String jsonString = objectMapper.writeValueAsString(row);
                T dto = objectMapper.readValue(jsonString, cls);
                dtoList.add(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("DTO 변환 실패", e);
            }
        }
        return dtoList;
    }

    public <T> T selectRow(Class<T> cls) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Map<String, Object> row = selectRow();
        if (row == null) return null;

        try {
            String jsonString = objectMapper.writeValueAsString(row);
            return objectMapper.readValue(jsonString, cls);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DTO 변환 실패", e);
        }
    }

    private <T> T selectValue() {
        Map<String, Object> row = selectRow();
        if (row == null || row.isEmpty()) {
            return null;
        }
        return (T) row.values().iterator().next();
    }

    public Long selectLong() {
        Object value = selectValue();
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    public String selectString() {
        return selectValue();
    }

    public LocalDateTime selectDatetime() {
        return selectValue();
    }

    public Boolean selectBoolean() {
        Object value = selectValue();
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        return false;
    }

    public List<Long> selectLongs() {
        List<Map<String, Object>> rows = selectRows();
        List<Long> longList = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!row.isEmpty()) {
                Object value = row.values().iterator().next();
                if (value instanceof Long) {
                    longList.add((Long) value);
                } else if (value instanceof Number) {
                    longList.add(((Number) value).longValue());
                }
            }
        }
        return longList;
    }
}
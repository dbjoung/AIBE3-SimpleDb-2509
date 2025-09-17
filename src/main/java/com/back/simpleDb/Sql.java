package com.back.simpleDb;

import com.back.Article;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder rawSql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public <T> Sql append(T sqlPart) {
        if (!rawSql.isEmpty()) rawSql.append(" ");
        rawSql.append(sqlPart);
        return this;
    }

    @SafeVarargs
    public final <T> Sql append(String sqlPart, T... param) {
        if (!rawSql.isEmpty()) rawSql.append(" ");
        rawSql.append(sqlPart);

        params.addAll(Arrays.asList(param));
        return this;
    }

    @SafeVarargs
    public final <T> Sql appendIn(String sqlPart, T... param) {
        if (!rawSql.isEmpty()) rawSql.append(" ");

        // 플레이스홀더 (?, ?, ?) 만들기
        String placeholders = String.join(",", Collections.nCopies(param.length, "?"));
        // sqlPart 안의 ?를 (?, ?, ?)로 교체
        rawSql.append(sqlPart.replace("?", placeholders));

        // 파라미터 추가
        params.addAll(Arrays.asList(param));

        return this;
    }

    public void logSql() {
        if (simpleDb.isDevMode()) {
            System.out.println("[SQL]\n" + rawSql);
            if(!params.isEmpty())
                System.out.println("[PARAMS]\n" + params);
        }
    }

    public int executeUpdate(boolean returnId) {
        String sql = rawSql.toString();
        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql, returnId ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)
        ) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            int result = pstmt.executeUpdate();

            if(returnId) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * INSERT 실행 후 AUTO_INCREMENT 키 반환
     */
    public long insert() {
        return executeUpdate(true);
    }

    /**
     * Update 실행 후 변경된 raw 갯수 반환
     */
    public int update() {
        return executeUpdate(false);
    }

    /**
     * delete 실행 후 삭제된 raw 갯수 반환
     */
    public int delete() {
        return executeUpdate(false);
    }

    /**
     * select 실행 후 List<article>로 반환
     */
    public List<Map<String, Object>> selectRows() {
        String sql = rawSql.toString();
        List<Map<String, Object>> result = new ArrayList<>();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while(rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",rs.getLong("id"));
                row.put("title",rs.getString("title"));
                row.put("body",rs.getString("body"));
                row.put("createdDate",rs.getTimestamp("createdDate").toLocalDateTime());
                row.put("modifiedDate",rs.getTimestamp("modifiedDate").toLocalDateTime());
                row.put("isBlind",rs.getBoolean("isBlind"));
                result.add(row);
            }

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * select 실행 후 article 반환
     */
    public Map<String, Object> selectRow() {
        String sql = rawSql.toString();
        Map<String, Object> result = new HashMap<>();

        if (simpleDb.isDevMode()) {
            System.out.println("[SQL]\n" + sql);
        }

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while(rs.next()) {
                result.put("id",rs.getLong("id"));
                result.put("title",rs.getString("title"));
                result.put("body",rs.getString("body"));
                result.put("createdDate",rs.getTimestamp("createdDate").toLocalDateTime());
                result.put("modifiedDate",rs.getTimestamp("modifiedDate").toLocalDateTime());
                result.put("isBlind",rs.getBoolean("isBlind"));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * select 실행 후 DB에서 현재 LocalDateTime 반환
     */
    public LocalDateTime selectDatetime() {
        String sql = rawSql.toString();
        LocalDateTime result = LocalDateTime.now();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while(rs.next()) {
                result = rs.getTimestamp("createdDate").toLocalDateTime();
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * select 실행 후 id 반환
     */
    public Long selectLong() {
        String sql = rawSql.toString();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
        ) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();

            long result = -1L;

            while(rs.next()) {
                result = rs.getLong(1);
            }

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * select 실행 후 제목 String 으로 반환
     */
    public String selectString() {
        String sql = rawSql.toString();
        String result = "";

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while(rs.next()) {
                result = rs.getString("title");
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * select 실행 후 isBlind String 으로 반환
     */
    public Boolean selectBoolean() {
        String sql = rawSql.toString();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            boolean result = true;
            while(rs.next()) {
                result = rs.getBoolean(1);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * select 실행 후 isBlind String 으로 반환
     */
    public List<Long> selectLongs() {
        String sql = rawSql.toString();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
        ) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            List<Long> result = new ArrayList<>();
            while(rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * select 실행 후 List<article>로 반환
     */
    public List<Article> selectRows(Class<Article> articleClass) {
        String sql = rawSql.toString();
        List<Article> result = new ArrayList<>();

        logSql();

        try (
                PreparedStatement pstmt = simpleDb
                        .getConnection()
                        .prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while(rs.next()) {
                Article row = new Article();
                row.setId(rs.getLong("id"));
                row.setTitle(rs.getString("title"));
                row.setBody(rs.getString("body"));
                row.setCreatedDate(rs.getTimestamp("createdDate").toLocalDateTime());
                row.setModifiedDate(rs.getTimestamp("modifiedDate").toLocalDateTime());
                row.setBlind(rs.getBoolean("isBlind"));
                result.add(row);
            }

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * select 실행 후 List<article>로 반환
     */
    public <T> T selectRow(Class<T> clazz) {
        String sql = rawSql.toString();
        logSql();

        try (
                PreparedStatement pstmt = simpleDb.getConnection().prepareStatement(sql)
        ) {
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // alias or name
                    Object value = rs.getObject(i);

                    try {
                        Field field = clazz.getDeclaredField(columnName);
                        field.setAccessible(true);
                        field.set(obj, value);
                    } catch (NoSuchFieldException ignore) {
                        // DB 컬럼과 클래스 필드가 일치하지 않으면 무시
                    }
                }
                return obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
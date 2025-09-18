package com.back.simpleDb;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
public class MyJdbcTemplate {

    private final Connection connection;
    private final boolean devMode;

    public MyJdbcTemplate(Connection connection, boolean devMode) {
        this.connection = connection;
        this.devMode = devMode;
    }

    public int executeUpdate(String sql, List<Object> parameters) {
        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            setParameters(parameters, pstm);
            return pstm.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            logQuery(sql, parameters);
        }
    }

    public int executeInsert(String sql, List<Object> parameters) {
        try (PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(parameters, pstm);

            pstm.executeUpdate();
            ResultSet rs = pstm.getGeneratedKeys();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            logQuery(sql, parameters);
        }

        return -1;
    }

    public <T> T query(String sql, List<Object> parameters, ResultSetExtractor<T> rse) {
        try (PreparedStatement pstm = connection.prepareStatement(sql);
             ResultSet rs = executeQuery(pstm, parameters)) {

            return rse.extractData(rs);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            logQuery(sql, parameters);
        }
    }

    private ResultSet executeQuery(PreparedStatement pstm, List<Object> parameters) throws SQLException {
        setParameters(parameters, pstm);
        return pstm.executeQuery();
    }

    private void setParameters(List<Object> parameters, PreparedStatement pstm) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            pstm.setObject(i + 1, parameters.get(i));
        }
    }

    private void logQuery(String sql, List<Object> parameters) {
        if (devMode) {
            log.info("\n[Query] : {} \n[Parameters] : {}\n", sql, parameters);
        }
    }
}

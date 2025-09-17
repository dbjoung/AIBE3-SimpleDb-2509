package com.back.simpleDb;

import lombok.Getter;

import java.sql.*;

@Getter
public class SimpleDb implements AutoCloseable { // AutoCloseable 구현해서 try-with-resources에서도 사용 가능
    private final String driver;
    private final String url;
    private final String username;
    private final String password;
    private Boolean devMode = false;

    private final ThreadLocal<Connection> threadLocalConnection = ThreadLocal.withInitial(() -> null);

    public SimpleDb(String host, String user, String password, String database) {
        this.driver = "com.mysql.cj.jdbc.Driver";
        this.url = "jdbc:mysql://" + host + ":3306/" + database
                + "?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        this.username = user;
        this.password = password;

        loadingJdbcDriver();
        openConnection();
    }

    public Connection getConnection() {
        try {
            Connection conn = threadLocalConnection.get();
            if (conn == null || conn.isClosed()) {
                conn = openConnection();
                threadLocalConnection.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 가져오기 실패", e);
        }
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    private Connection openConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 실패", e);
        }
    }

    public void run(String sql) {
        if (devMode) {
            System.out.println("[SQL]\n" + sql);
        }

        try (Statement stmt = this.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 실패", e);
        }
    }

    public void run(String sql, Object... params) {
        if (devMode) {
            System.out.println("[SQL]\n" + sql);
        }

        try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            // ? 에 값 바인딩
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 실패", e);
        }
    }

    public boolean isDevMode() {
        return devMode;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    private void loadingJdbcDriver() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found", e);
        }
    }

    public void startTransaction() {
        Connection conn = this.getConnection();
        try {
            if (conn == null || conn.isClosed()) {
                openConnection();
            }
            conn.setAutoCommit(false);
            if (devMode) {
                System.out.println("트랜잭션 시작 (AutoCommit : " + conn.getAutoCommit() + ")");
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 실패", e);
        }
    }

    public void commit() {
        Connection conn = this.getConnection();
        try {
            if (conn != null && !conn.getAutoCommit()) {
                conn.commit();
                conn.setAutoCommit(true);
                if (devMode) {
                    System.out.println("트랜잭션 커밋");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 커밋 실패", e);
        }
    }

    public void rollback() {
        Connection conn = this.getConnection();
        try {
            if (conn != null && !conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
                if (devMode) {
                    System.out.println("트랜잭션 롤백");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 롤백 실패", e);
        }
    }

    @Override
    public void close() {
        try {
            Connection conn = threadLocalConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
                threadLocalConnection.remove();
                if (devMode) {
                    System.out.println("DB 연결이 닫혔습니다.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 종료 실패", e);
        }
    }
}
package com.back.simpleDb;
import lombok.Getter;
import lombok.Setter;

import java.sql.*;

public class SimpleDb {
    @Getter
    private final ThreadLocal<Connection> myConn = new ThreadLocal<>();
    @Getter
    @Setter
    private boolean devMode = false;

    private final String url;
    private final String user;
    private final String password;

    public SimpleDb(String host, String user, String pass, String db) {
        this(host, 3306, user, pass, db);
    }

    public SimpleDb(String host, int port, String user, String pass, String db) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + db;
        this.user = user;
        this.password = pass;
    }

    public Connection getConnection() {
        try {
            Connection conn = myConn.get();
            if (conn == null) {
                conn = DriverManager.getConnection(this.url, this.user, this.password);
                myConn.set(conn);
            }

            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("DB Connection 중 예외 발생", e);
        }
    }

    public void run(String sql, Object ...args) {
        Connection conn = getConnection();
        try (
            PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            for (int i=0 ; i< args.length ; i++) {
                ps.setObject(i+1, args[i]);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("run 실행 중 예외 발생", e);
        }
    }

    public void close() {
        try {
            if (myConn.get() != null) {
                myConn.get().close();
                myConn.remove();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 종료 중 연결 에러", e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("DB 접근 오류 - 트랜잭션 시작", e);
        }
    }

    public void rollback() {
        Connection conn = getConnection();

        try {
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("DB 접근 오류 - rollback", e);
        }
    }

    public void commit() {
        Connection conn = getConnection();

        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("DB 접근 오류 - 커밋", e);
        }
    }
}

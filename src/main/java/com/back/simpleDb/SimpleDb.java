package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {

    private static final ThreadLocal<Connection> CONNECTION_THREAD_LOCAL = new ThreadLocal<>();
    private boolean devMode;
    private final String host, user, password, database;

    public SimpleDb(String host, String user, String password, String database) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean mode) {
        this.devMode = mode;
    }

    public void run(String sql) {
        Connection connection = getConnection();
        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String sql, Object... args) {
        Connection connection = getConnection();
        try (PreparedStatement pstm = connection.prepareStatement(sql)) {

            for (int i = 0; i < args.length; i++) {
                pstm.setObject(i + 1, args[i]);
            }

            pstm.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(getConnection(), devMode);
    }

    public void close() {
        try {
            Connection connection = getConnection();
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            CONNECTION_THREAD_LOCAL.remove();
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        Connection connection = getConnection();
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    private Connection getConnection() {
        Connection connection = CONNECTION_THREAD_LOCAL.get();

        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        String.format("jdbc:mysql://%s:3306/%s", host, database),
                        user,
                        password
                );
                CONNECTION_THREAD_LOCAL.set(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return connection;
    }
}

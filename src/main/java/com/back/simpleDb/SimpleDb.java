package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// 데이터베이스 연결 생성 및 관리 클래스.
// 멀티스레드 환경에서 스레드별로 독립적인 연결을 제공.
public class SimpleDb {
    private boolean devMode; // 개발 모드 활성화 여부
    private final String host; // DB 서버 호스트 주소
    // DB 접속 사용자 이름, 비번, DB이름
    private final String user;
    private final String password;
    private final String dbName;
    // 스레드별 데이터베이스 연결을 저장하는 맵 (key: 스레드 ID, value: Connection)
    private final Map<Long, Connection> connectionMap = new HashMap<>();

    // DB 접속 정보를 초기화.
    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
        this.devMode = false;
    }

    // 개발 모드를 활성화/비활성화. (true로 설정 시 실행 SQL 로그 출력)
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    // 현재 스레드에 할당된 DB 커넥션을 반환 or 새로 생성.
    Connection getConnection() {
        long threadId = Thread.currentThread().getId(); // 현재 스레드 ID 가져옴
        Connection conn;

        synchronized (connectionMap) { // 한 번에 한 스레드만 connectionMap 접근
            conn = connectionMap.get(threadId);
        }

        try { // 커넥션 새로 생성, 다시 맵에 등록
            if (conn == null || conn.isClosed()) {
                String url = "jdbc:mysql://" + host + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&serverTimezone=Asia/Seoul";
                conn = DriverManager.getConnection(url, user, password);

                synchronized (connectionMap) {
                    connectionMap.put(threadId, conn);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return conn; // 커넥션 반환
    }

    // 트랜잭션 시작
    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false); // 자동커밋 off
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 트랜잭션 커밋(최종 저장)
    public void commit() {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 트랜잭션 롤백
    public void rollback() {
        try {
            getConnection().rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 현재 스레드가 사용하던 DB 커넥션을 닫고 맵에서 제거.
    public void close() {
        long threadId = Thread.currentThread().getId();
        Connection conn;

        synchronized (connectionMap) {
            conn = connectionMap.remove(threadId); // 커넥션 맵에서 제거
        }

        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) { // 트랜잭션 중이었으면 다시 오토커밋 설정
                    conn.setAutoCommit(true);
                }
                conn.close(); // 커넥션 종료
            } catch (SQLException e) {

            }
        }
    }

    // SQL 쿼리를 만들고 실행할 Sql 객체를 생성하여 반환.
    public Sql genSql() {
        return new Sql(this);
    }

    // 현재 개발 모드 상태를 반환. (Sql 클래스에서 로그 출력 여부 확인 시 사용)
    boolean isDevMode() {
        return this.devMode;
    }

    // 파라미터가 없는 SQL을 실행하는 메서드
    public void run(String sql) {
        genSql().append(sql).run();
    }

    // 파라미터가 있는 SQL을 실행하는 메서드
    public void run(String sql, Object... params) {
        genSql().append(sql, params).run();
    }
}
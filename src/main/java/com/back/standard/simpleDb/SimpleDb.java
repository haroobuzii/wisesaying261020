package com.back.standard.simpleDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;
    private boolean devMode = false;

    // 스레드마다 자기 커넥션을 따로 갖는다
    // 커넥션 하나를 여러 스레드가 같이 쓰면, 한 스레드의 close()·트랜잭션이 다른 스레드에 영향을 준다
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    // 과제 "SimpleDb 구현"(simple-db 저장소)에서 만든 클래스를 재사용. 포트를 지정할 수 있는 생성자만 추가했다
    public SimpleDb(String host, String username, String password, String dbName) {
        this(host, 3306, username, password, dbName);
    }

    public SimpleDb(String host, int port, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:%d/%s".formatted(host, port, dbName);
        this.username = username;
        this.password = password;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    // DDL(CREATE, DROP, TRUNCATE)·DML(INSERT, UPDATE, DELETE) 실행. 영향받은 행 수를 돌려준다
    public int run(String sql, Object... params) {
        return executeUpdate(sql, List.of(params));
    }

    public Sql genSql() {
        return new Sql(this);
    }

    // 트랜잭션 시작: 자동 커밋을 끈다 → 이후 SQL 은 commit() 전까지 확정되지 않는다
    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 트랜잭션 동안 실행한 SQL 을 확정하고, 다시 자동 커밋으로 돌아간다
    public void commit() {
        endTransaction(true);
    }

    // 트랜잭션 동안 실행한 SQL 을 모두 취소하고, 다시 자동 커밋으로 돌아간다
    public void rollback() {
        endTransaction(false);
    }

    private void endTransaction(boolean commit) {
        try {
            Connection connection = getConnection();

            if (commit) connection.commit();
            else connection.rollback();

            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 지금 스레드의 커넥션만 닫는다. 다음에 SQL 을 실행하면 새 커넥션이 열린다
    public void close() {
        Connection connection = connectionHolder.get();

        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionHolder.remove();
        }
    }

    // ---------- Sql 이 사용하는 실행 메서드 (같은 패키지에서만 사용) ----------

    // INSERT 실행 후 AUTO_INCREMENT 로 생성된 주키를 돌려준다
    long executeInsert(String sql, List<Object> params) {
        try (PreparedStatement stmt = prepare(sql, params, Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // UPDATE·DELETE 등 실행 후 영향받은 행 수를 돌려준다
    int executeUpdate(String sql, List<Object> params) {
        try (PreparedStatement stmt = prepare(sql, params, Statement.NO_GENERATED_KEYS)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // SELECT 실행 후, 각 행을 {컬럼이름: 값} Map 으로 바꿔 목록으로 돌려준다
    // 값의 타입은 MySQL 드라이버가 정해 준다: DATETIME → LocalDateTime, BIT(1) → Boolean, INT UNSIGNED → Long ...
    List<Map<String, Object>> executeSelect(String sql, List<Object> params) {
        try (PreparedStatement stmt = prepare(sql, params, Statement.NO_GENERATED_KEYS);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>(); // 컬럼 순서 유지

                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }

                rows.add(row);
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // SQL 의 ? 자리에 파라미터를 순서대로 채운 PreparedStatement 를 만든다
    // 값을 문자열로 이어 붙이지 않고 바인딩하므로 SQL 인젝션에 안전하다
    private PreparedStatement prepare(String sql, List<Object> params, int generatedKeys) throws SQLException {
        if (devMode) {
            System.out.println("== rawSql ==\n" + sql.trim() + "\n== params == " + params);
        }

        PreparedStatement stmt = getConnection().prepareStatement(sql, generatedKeys);

        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i)); // JDBC 는 1번부터 센다
        }

        return stmt;
    }

    // 지금 스레드의 커넥션. 없거나 닫혔으면 새로 연다
    private Connection getConnection() throws SQLException {
        Connection connection = connectionHolder.get();

        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
            connectionHolder.set(connection);
        }

        return connection;
    }
}

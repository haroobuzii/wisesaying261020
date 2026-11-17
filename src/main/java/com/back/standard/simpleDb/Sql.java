package com.back.standard.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// SQL 문장을 조각조각 이어 붙이고(append), 마지막에 실행하는 객체
// 예) simpleDb.genSql().append("SELECT * FROM article").append("WHERE id = ?", 1).selectRow()
public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    // SQL 조각과 그 조각의 ? 에 들어갈 값들을 추가한다. 자기 자신을 돌려주므로 .append().append() 로 이어 쓸 수 있다
    public Sql append(String sqlPart, Object... params) {
        sqlBuilder.append(sqlPart).append('\n');
        this.params.addAll(Arrays.asList(params));
        return this;
    }

    // IN (?) 처럼 ? 하나에 여러 값을 넣고 싶을 때. ? 를 값 개수만큼 "?, ?, ?" 로 펼친 뒤 append 한다
    // 예) appendIn("WHERE id IN (?)", 1, 2, 3) → "WHERE id IN (?, ?, ?)" + 파라미터 1, 2, 3
    public Sql appendIn(String sqlPart, Object... params) {
        String placeholders = String.join(", ", Collections.nCopies(params.length, "?"));

        return append(sqlPart.replace("?", placeholders), params);
    }

    public long insert() {
        return simpleDb.executeInsert(getSql(), params);
    }

    public int update() {
        return simpleDb.executeUpdate(getSql(), params);
    }

    public int delete() {
        return simpleDb.executeUpdate(getSql(), params);
    }

    public List<Map<String, Object>> selectRows() {
        return simpleDb.executeSelect(getSql(), params);
    }

    // 첫 번째 행만. 결과가 없으면 null
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();

        return rows.isEmpty() ? null : rows.getFirst();
    }

    // 각 행(Map)을 cls 객체로 바꿔서 돌려준다. 예) selectRows(Article.class)
    public <T> List<T> selectRows(Class<T> cls) {
        return selectRows()
                .stream()
                .map(row -> RowMapper.map(row, cls))
                .toList();
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();

        return row == null ? null : RowMapper.map(row, cls);
    }

    // ---------- 값 하나만 꺼내기: 첫 번째 행의 첫 번째 컬럼 ----------

    public LocalDateTime selectDatetime() {
        Object value = selectValue();

        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();

        return (LocalDateTime) value;
    }

    public Long selectLong() {
        return toLong(selectValue());
    }

    // 모든 행의 첫 번째 컬럼을 Long 목록으로 (행 순서 그대로)
    public List<Long> selectLongs() {
        return selectRows()
                .stream()
                .map(row -> toLong(row.values().iterator().next()))
                .toList();
    }

    public String selectString() {
        Object value = selectValue();

        return value == null ? null : value.toString();
    }

    // BIT(1) 컬럼은 Boolean 으로, "SELECT 1 = 1" 같은 비교식은 숫자(1/0)로 온다 → 둘 다 처리
    public Boolean selectBoolean() {
        Object value = selectValue();

        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.longValue() != 0;

        return Boolean.parseBoolean(value.toString());
    }

    private Object selectValue() {
        Map<String, Object> row = selectRow();

        if (row == null) return null;

        return row.values().iterator().next();
    }

    private static Long toLong(Object value) {
        if (value == null) return null;

        return ((Number) value).longValue(); // Integer·Long·BigInteger 등 숫자 타입을 모두 long 으로
    }

    private String getSql() {
        return sqlBuilder.toString();
    }
}

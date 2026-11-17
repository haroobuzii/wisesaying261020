package com.back.domain.wiseSaying.repository;

import com.back.WiseSaying;
import com.back.standard.dto.Page;
import com.back.standard.simpleDb.Sql;
import com.back.standard.simpleDb.SimpleDb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// MySQL 의 wise_saying 테이블에 저장하는 저장소 (테이블 구조: sql/schema.sql)
// SQL 은 이 클래스에만 있다. 컨트롤러·서비스는 WiseSayingRepository 인터페이스만 알고 있어서 바뀌지 않는다
public class WiseSayingDbRepository implements WiseSayingRepository {
    private final SimpleDb simpleDb;
    private final Path buildDirPath; // data.json 을 만들 폴더

    public WiseSayingDbRepository(SimpleDb simpleDb, String buildDirPath) {
        this.simpleDb = simpleDb;
        this.buildDirPath = Path.of(buildDirPath);
    }

    @Override
    public List<WiseSaying> findAll() {
        return simpleDb.genSql()
                .append("SELECT * FROM wise_saying")
                .append("ORDER BY id ASC")
                .selectRows()
                .stream()
                .map(WiseSayingDbRepository::toWiseSaying)
                .toList();
    }

    // 검색·페이징을 자바가 아니라 SQL 로 처리한다 (필요한 5개만 DB 에서 가져온다)
    @Override
    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        // 1. 검색 결과 전체 개수 → 페이지 메뉴 계산용
        Sql countSql = simpleDb.genSql().append("SELECT COUNT(*) FROM wise_saying");
        appendWhere(countSql, keywordType, keyword);
        int totalCount = countSql.selectLong().intValue();

        // 2. 현재 페이지 분량만: 최신순(id 내림차순)으로 정렬 → OFFSET 만큼 건너뛰고 LIMIT 개
        Sql listSql = simpleDb.genSql().append("SELECT * FROM wise_saying");
        appendWhere(listSql, keywordType, keyword);
        listSql.append("ORDER BY id DESC")
                .append("LIMIT ? OFFSET ?", pageSize, (page - 1) * pageSize);

        List<WiseSaying> content = listSql.selectRows()
                .stream()
                .map(WiseSayingDbRepository::toWiseSaying)
                .toList();

        return new Page<>(content, page, pageSize, totalCount);
    }

    // 검색어가 있을 때만 WHERE ... LIKE 를 붙인다. 컬럼 이름은 정해진 값 중에서만 고른다 (SQL 인젝션 방지)
    private static void appendWhere(Sql sql, String keywordType, String keyword) {
        if (keyword.isBlank()) return;

        switch (keywordType) {
            case "author" -> sql.append("WHERE author LIKE CONCAT('%', ?, '%')", keyword);
            case "content" -> sql.append("WHERE content LIKE CONCAT('%', ?, '%')", keyword);
            default -> sql.append("WHERE content LIKE CONCAT('%', ?, '%') OR author LIKE CONCAT('%', ?, '%')", keyword, keyword);
        }
    }

    @Override
    public int count() {
        return simpleDb.genSql()
                .append("SELECT COUNT(*) FROM wise_saying")
                .selectLong()
                .intValue();
    }

    @Override
    public void save(WiseSaying wiseSaying) {
        LocalDateTime now = LocalDateTime.now().withNano(0); // DATETIME 은 초 단위까지

        if (wiseSaying.isNew()) {
            // 번호는 MySQL AUTO_INCREMENT 가 붙인다 (삭제된 번호는 재사용하지 않는다)
            long newId = simpleDb.genSql()
                    .append("INSERT INTO wise_saying")
                    .append("SET createDate = ?, modifyDate = ?, content = ?, author = ?",
                            now, now, wiseSaying.getContent(), wiseSaying.getAuthor())
                    .insert();

            wiseSaying.setId((int) newId);
            wiseSaying.setCreateDate(now);
            wiseSaying.setModifyDate(now);
            return;
        }

        simpleDb.genSql()
                .append("UPDATE wise_saying")
                .append("SET modifyDate = ?, content = ?, author = ?", now, wiseSaying.getContent(), wiseSaying.getAuthor())
                .append("WHERE id = ?", wiseSaying.getId())
                .update();

        wiseSaying.setModifyDate(now);
    }

    @Override
    public WiseSaying findById(int id) {
        Map<String, Object> row = simpleDb.genSql()
                .append("SELECT * FROM wise_saying")
                .append("WHERE id = ?", id)
                .selectRow();

        return row == null ? null : toWiseSaying(row);
    }

    @Override
    public boolean deleteById(int id) {
        int deletedCount = simpleDb.genSql()
                .append("DELETE FROM wise_saying")
                .append("WHERE id = ?", id)
                .delete();

        return deletedCount > 0;
    }

    // 모든 명언을 data.json 으로 내보낸다 (형식은 파일 저장소와 같다)
    @Override
    public String build() {
        Path dataPath = buildDirPath.resolve("data.json");

        try {
            Files.createDirectories(buildDirPath);
            Files.writeString(dataPath, WiseSayingFileRepository.toJsonArray(findAll()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return dataPath.toString();
    }

    // 테이블을 비운다. AUTO_INCREMENT 도 1 로 돌아간다 (테스트 준비용)
    public void truncate() {
        simpleDb.run("TRUNCATE wise_saying");
    }

    // DB 한 행(Map) → WiseSaying 객체
    private static WiseSaying toWiseSaying(Map<String, Object> row) {
        WiseSaying wiseSaying = new WiseSaying((String) row.get("content"), (String) row.get("author"));
        wiseSaying.setId(((Number) row.get("id")).intValue());
        wiseSaying.setCreateDate((LocalDateTime) row.get("createDate"));
        wiseSaying.setModifyDate((LocalDateTime) row.get("modifyDate"));

        return wiseSaying;
    }
}

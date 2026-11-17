package com.back;

import com.back.standard.simpleDb.SimpleDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 15단계 MySQL 저장소 통합 테스트
// 테스트 DB(wiseSaying__test)를 쓰고, 매 테스트 전에 wise_saying 테이블을 TRUNCATE 한다
// (등록·목록·삭제·수정은 WiseSayingControllerTest, 검색·페이징은 WiseSayingListTest 가 같은 MySQL 저장소로 검증한다)
class WiseSayingDbTest {
    private SimpleDb simpleDb;

    @BeforeEach
    void beforeEach() {
        AppTestRunner.clearDb();
        simpleDb = AppContext.getSimpleDb();
    }

    private Map<String, Object> findRow(int id) {
        return simpleDb.genSql()
                .append("SELECT * FROM wise_saying WHERE id = ?", id)
                .selectRow();
    }

    @Test
    @DisplayName("기본 저장소는 MySQL 이고, 테스트는 테스트 DB 를 쓴다")
    void t1() {
        assertThat(AppConfig.getStorage()).isEqualTo(AppConfig.Storage.DB);
        assertThat(AppConfig.getDbName()).isEqualTo("wiseSaying__test");
        assertThat(simpleDb.genSql().append("SELECT DATABASE()").selectString()).isEqualTo("wiseSaying__test");
    }

    @Test
    @DisplayName("매 테스트 전에 테이블이 비워진다")
    void t2() {
        assertThat(simpleDb.genSql().append("SELECT COUNT(*) FROM wise_saying").selectLong()).isEqualTo(0L);
    }

    @Test
    @DisplayName("등록 - wise_saying 테이블에 한 행이 저장된다")
    void t3() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        Map<String, Object> row = findRow(1);

        assertThat(row.get("content")).isEqualTo("현재를 사랑하라.");
        assertThat(row.get("author")).isEqualTo("작자미상");
        assertThat(row.get("createDate")).isNotNull();
        assertThat(row.get("modifyDate")).isNotNull();
    }

    @Test
    @DisplayName("재시작 - 프로그램을 다시 시작해도 DB 에서 명언을 읽어 목록에 보여준다")
    void t4() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("""
                번호 / 작가 / 명언
                ----------------------
                2 / 작자미상 / 과거에 집착하지 마라.
                1 / 작자미상 / 현재를 사랑하라.
                """);
    }

    @Test
    @DisplayName("재시작 - 번호가 이어지고, 삭제한 번호는 재사용하지 않는다")
    void t5() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=2
                종료
                """);

        String out = AppTestRunner.run("""
                등록
                새 명언
                새 작가
                종료
                """);

        assertThat(out).contains("3번 명언이 등록되었습니다.");
        assertThat(findRow(2)).isNull();
    }

    @Test
    @DisplayName("수정 - DB 의 내용이 바뀌고, 재시작 후에도 수정된 내용이 보인다")
    void t6() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                종료
                """);

        Map<String, Object> row = findRow(1);
        assertThat(row.get("content")).isEqualTo("현재와 자신을 사랑하라.");
        assertThat(row.get("author")).isEqualTo("홍길동");

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 홍길동 / 현재와 자신을 사랑하라.");
    }

    @Test
    @DisplayName("삭제 - DB 에서 행이 지워진다")
    void t7() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                삭제?id=1
                종료
                """);

        assertThat(findRow(1)).isNull();
    }

    @Test
    @DisplayName("검색·페이징 - 샘플 10개 중 작가 검색 2페이지가 SQL 로 처리된다")
    void t8() {
        String out = AppTestRunner.runWithSampleData("""
                목록?keywordType=author&keyword=작자&page=2
                종료
                """);

        assertThat(simpleDb.genSql().append("SELECT COUNT(*) FROM wise_saying").selectLong()).isEqualTo(10L);
        assertThat(out).contains("검색타입 : author", "검색어 : 작자");
        assertThat(out).containsPattern("(?m)^5 / 작자미상 5 / 명언 5$");
        assertThat(out).containsPattern("(?m)^1 / 작자미상 1 / 명언 1$");
        assertThat(out).doesNotContainPattern("(?m)^6 / ");
        assertThat(out).contains("페이지 : 1 / [2]");
    }

    @Test
    @DisplayName("빌드 - DB 의 모든 명언으로 data.json 을 만든다")
    void t9() throws IOException {
        String out = AppTestRunner.run("""
                등록
                명언 1
                작가 1
                등록
                명언 2
                작가 2
                빌드
                종료
                """);

        assertThat(out).contains("data.json 파일의 내용이 갱신되었습니다.");
        assertThat(Files.readString(Path.of(AppConfig.getDbDirPath(), "data.json"))).isEqualTo("""
                [
                  {
                    "id": 1,
                    "content": "명언 1",
                    "author": "작가 1"
                  },
                  {
                    "id": 2,
                    "content": "명언 2",
                    "author": "작가 2"
                  }
                ]""");
    }
}

package com.back;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 13단계 검색, 14단계 페이징 통합 테스트
class WiseSayingListTest {
    // 목록의 한 줄을 정확히 찾기 위한 정규식 ("1 / ..." 이 "10 / ..." 에 섞여 걸리지 않도록 줄 단위로 비교)
    private static String line(String text) {
        return "(?m)^" + text + "$";
    }

    @Test
    @DisplayName("검색 - 내용(content)에 키워드가 들어간 명언만, 검색 조건을 목록 위에 보여준다")
    void t1() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                목록?keywordType=content&keyword=과거
                종료
                """);

        assertThat(out).contains("""
                ----------------------
                검색타입 : content
                검색어 : 과거
                ----------------------
                번호 / 작가 / 명언
                ----------------------
                2 / 작자미상 / 과거에 집착하지 마라.
                """);
        assertThat(out).doesNotContain("1 / 작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("검색 - 작가(author)에 키워드가 들어간 명언만")
    void t2() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                홍길동
                목록?keywordType=author&keyword=작자
                종료
                """);

        assertThat(out).contains("검색타입 : author", "검색어 : 작자");
        assertThat(out).contains("1 / 작자미상 / 현재를 사랑하라.");
        assertThat(out).doesNotContain("2 / 홍길동 / 과거에 집착하지 마라.");
    }

    @Test
    @DisplayName("샘플 - 시작할 때 명언이 0개면 샘플 10개를 만든다")
    void t3() {
        String out = AppTestRunner.runWithSampleData("""
                등록
                새 명언
                새 작가
                종료
                """);

        assertThat(out).contains("11번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("페이징 - page 생략 시 1페이지: 최신순 10~6번, 페이지 메뉴 [1] / 2")
    void t4() {
        String out = AppTestRunner.runWithSampleData("""
                목록
                종료
                """);

        for (int i = 10; i >= 6; i--) {
            assertThat(out).containsPattern(line(i + " / 작자미상 " + i + " / 명언 " + i));
        }
        assertThat(out).doesNotContainPattern(line("5 / 작자미상 5 / 명언 5"));
        assertThat(out.indexOf("명언 10\n")).isLessThan(out.indexOf("명언 6\n"));
        assertThat(out).contains("페이지 : [1] / 2");
    }

    @Test
    @DisplayName("페이징 - page=2: 5~1번, 페이지 메뉴 1 / [2]")
    void t5() {
        String out = AppTestRunner.runWithSampleData("""
                목록?page=2
                종료
                """);

        for (int i = 5; i >= 1; i--) {
            assertThat(out).containsPattern(line(i + " / 작자미상 " + i + " / 명언 " + i));
        }
        assertThat(out).doesNotContainPattern(line("6 / 작자미상 6 / 명언 6"));
        assertThat(out).contains("페이지 : 1 / [2]");
    }

    @Test
    @DisplayName("검색 + 페이징 - 내용에 1 검색: 10번·1번, 검색 결과 기준 1페이지")
    void t6() {
        String out = AppTestRunner.runWithSampleData("""
                목록?keywordType=content&keyword=1
                종료
                """);

        assertThat(out).containsPattern(line("10 / 작자미상 10 / 명언 10"));
        assertThat(out).containsPattern(line("1 / 작자미상 1 / 명언 1"));
        assertThat(out).doesNotContainPattern(line("2 / 작자미상 2 / 명언 2"));
        assertThat(out).contains("페이지 : [1]");
        assertThat(out).doesNotContain("페이지 : [1] / 2");
    }

    @Test
    @DisplayName("검색 + 페이징 - 검색 결과(10개)의 2페이지")
    void t7() {
        String out = AppTestRunner.runWithSampleData("""
                등록
                검색에 안 걸리는 명언
                홍길동
                목록?keywordType=author&keyword=작자&page=2
                종료
                """);

        for (int i = 5; i >= 1; i--) {
            assertThat(out).containsPattern(line(i + " / 작자미상 " + i + " / 명언 " + i));
        }
        assertThat(out).doesNotContain("홍길동");
        assertThat(out).contains("페이지 : 1 / [2]");
    }
}

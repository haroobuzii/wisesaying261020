package com.back;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WiseSayingControllerTest {
    @Test
    @DisplayName("종료")
    void t1() {
        String out = AppTestRunner.run("""
                종료
                """);

        assertThat(out).contains("== 명언 앱 ==", "명령) ", "프로그램이 종료합니다.");
    }

    @Test
    @DisplayName("등록 - 명언과 작가를 입력받는다")
    void t2() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        assertThat(out).contains("명언 :", "작가 :");
    }

    @Test
    @DisplayName("등록")
    void t3() {
        // App must create a new empty repository for this test.
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        assertThat(out).contains("명언 :", "작가 :", "1번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("등록 - 등록할 때마다 번호가 1씩 증가한다")
    void t4() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        assertThat(out).contains("1번 명언이 등록되었습니다.", "2번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("목록 - 최신 명언이 위에 나온다")
    void t5() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                목록
                종료
                """);

        assertThat(out).contains("번호 / 작성날짜 / 수정날짜 / 작가 / 명언", "----------------------");
        assertThat(out).containsPattern("2 / .+ / .+ / 작자미상 / 과거에 집착하지 마라.");
        assertThat(out).containsPattern("1 / .+ / .+ / 작자미상 / 현재를 사랑하라.");
        assertThat(out.indexOf("과거에 집착하지 마라.\n")).isLessThan(out.indexOf("현재를 사랑하라.\n"));
    }

    @Test
    @DisplayName("삭제")
    void t6() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=1
                목록
                종료
                """);

        assertThat(out).contains("1번 명언이 삭제되었습니다.");
        assertThat(out).doesNotContain("작자미상 / 현재를 사랑하라.");
        assertThat(out).contains("작자미상 / 과거에 집착하지 마라.");
    }

    @Test
    @DisplayName("삭제 - 없는 번호")
    void t7() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                삭제?id=1
                삭제?id=1
                종료
                """);

        assertThat(out).contains("1번 명언이 삭제되었습니다.", "1번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("수정")
    void t8() {
        String out = AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                목록
                종료
                """);

        assertThat(out).contains("명언(기존) : 현재를 사랑하라.", "작가(기존) : 작자미상");
        assertThat(out).containsPattern("1 / .+ / .+ / 홍길동 / 현재와 자신을 사랑하라.");
        assertThat(out).doesNotContain("작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("수정 - 없는 번호")
    void t9() {
        String out = AppTestRunner.run("""
                수정?id=3
                종료
                """);

        assertThat(out).contains("3번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("테스트마다 저장소가 초기화된다 - 앱을 새로 실행하면 목록이 비어 있고 번호가 1부터 시작한다")
    void t10() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        assertThat(out).doesNotContain("현재를 사랑하라.");
        assertThat(out).contains("1번 명언이 등록되었습니다.");
    }
}

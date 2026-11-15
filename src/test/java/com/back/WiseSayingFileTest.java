package com.back;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// 9단계 파일 영속성, 10단계 빌드 통합 테스트
// AppTestRunner.run 을 두 번 부르는 것 = 프로그램을 껐다가 다시 켜는 것
class WiseSayingFileTest {
    private Path dbDir;

    @BeforeEach
    void beforeEach() {
        AppTestRunner.clearDb();
        dbDir = Path.of(AppConfig.getDbDirPath());
    }

    private String read(String fileName) throws IOException {
        return Files.readString(dbDir.resolve(fileName));
    }

    @Test
    @DisplayName("테스트는 실제 db/ 가 아닌 테스트 전용 폴더를 쓴다")
    void t1() {
        assertThat(AppConfig.getDbDirPath()).isEqualTo("test_db/wiseSaying");
        assertThat(Files.exists(dbDir)).isFalse(); // 매 테스트 전에 비워져 있다
    }

    @Test
    @DisplayName("등록 - {번호}.json 에 id·content·author 가 JSON 으로, lastId.txt 에 마지막 번호가 저장된다")
    void t2() throws IOException {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        assertThat(read("1.json")).isEqualTo("""
                {
                  "id": 1,
                  "content": "현재를 사랑하라.",
                  "author": "작자미상"
                }""");
        assertThat(read("lastId.txt")).isEqualTo("1");
    }

    @Test
    @DisplayName("재시작 - 프로그램을 다시 시작해도 파일에서 명언을 읽어 목록에 보여준다")
    void t3() {
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
    @DisplayName("재시작 - 다음 번호가 이어진다")
    void t4() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        assertThat(out).contains("2번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("수정 - 파일이 갱신되고, 재시작 후에도 수정된 내용이 보인다")
    void t5() throws IOException {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                종료
                """);

        assertThat(read("1.json")).contains("\"content\": \"현재와 자신을 사랑하라.\"", "\"author\": \"홍길동\"");

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 홍길동 / 현재와 자신을 사랑하라.");
        assertThat(out).doesNotContain("현재를 사랑하라.");
    }

    @Test
    @DisplayName("삭제 - 파일이 지워지고, 재시작 후 목록에 없다")
    void t6() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=1
                종료
                """);

        assertThat(Files.exists(dbDir.resolve("1.json"))).isFalse();
        assertThat(Files.exists(dbDir.resolve("2.json"))).isTrue();

        String out = AppTestRunner.run("""
                목록
                삭제?id=1
                종료
                """);

        assertThat(out).doesNotContain("현재를 사랑하라.");
        assertThat(out).contains("2 / 작자미상 / 과거에 집착하지 마라.");
        assertThat(out).contains("1번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("삭제 - 삭제한 번호는 재사용하지 않는다 (재시작 후에도)")
    void t7() throws IOException {
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

        assertThat(read("lastId.txt")).isEqualTo("2");

        String out = AppTestRunner.run("""
                등록
                새 명언
                새 작가
                종료
                """);

        assertThat(out).contains("3번 명언이 등록되었습니다.");
        assertThat(read("lastId.txt")).isEqualTo("3");
    }

    @Test
    @DisplayName("빌드 - 모든 명언을 배열 형태의 data.json 으로 만들고 안내를 출력한다")
    void t8() throws IOException {
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
        assertThat(read("data.json")).isEqualTo("""
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

    @Test
    @DisplayName("JSON - 따옴표·역슬래시가 들어간 명언도 저장 후 그대로 읽어온다")
    void t9() {
        AppTestRunner.run("""
                등록
                그는 "괜찮아"라고 말했다 \\ 끝
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 작자미상 / 그는 \"괜찮아\"라고 말했다 \\ 끝");
    }
}

package com.back;

import com.back.standard.util.TestUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.function.Consumer;

public final class AppTestRunner {
    // 입력 문자열로 앱을 실행하고, 앱이 출력한 내용을 문자열로 돌려준다
    public static String run(String input, Consumer<Scanner> runApp) {
        AppConfig.setModeToTest(); // 실제 db/ 가 아닌 test_db/ 를 쓰도록

        var bytes = TestUtil.setOutToByteArray();
        try (Scanner scanner = TestUtil.genScanner(input)) {
            runApp.accept(scanner);
            System.out.flush();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            TestUtil.clearSetOutToByteArray();
        }
    }

    // 샘플 없이 시작 (CRUD 테스트용)
    public static String run(String input) {
        return run(input, scanner -> new App(scanner).run());
    }

    // 시작할 때 명언이 0개면 샘플 명언 10개를 만들고 시작 (검색·페이징 테스트용)
    public static String runWithSampleData(String input) {
        return run(input, scanner -> new App(scanner, true).run());
    }

    // 테스트 전용 저장 폴더(test_db/wiseSaying)를 비운다. 매 테스트 전에(@BeforeEach) 호출한다
    public static void clearDb() {
        AppConfig.setModeToTest();
        TestUtil.deleteDir(Path.of(AppConfig.getDbDirPath()));
    }
}

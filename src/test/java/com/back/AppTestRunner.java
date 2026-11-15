package com.back;

import com.back.standard.util.TestUtil;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

public final class AppTestRunner {
    // 입력 문자열로 앱을 실행하고, 앱이 출력한 내용을 문자열로 돌려준다
    public static String run(String input, Consumer<Scanner> runApp) {
        var bytes = TestUtil.setOutToByteArray();
        try (Scanner scanner = TestUtil.genScanner(input)) {
            runApp.accept(scanner);
            System.out.flush();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            TestUtil.clearSetOutToByteArray();
        }
    }

    // 샘플 없이 빈 저장소로 시작 (CRUD 테스트용)
    public static String run(String input) {
        return run(input, scanner -> new App(scanner).run());
    }

    // 시작할 때 샘플 명언 10개를 만들고 시작 (검색·페이징 테스트용)
    public static String runWithSampleData(String input) {
        return run(input, scanner -> new App(scanner, true).run());
    }
}

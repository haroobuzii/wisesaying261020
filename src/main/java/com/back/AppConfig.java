package com.back;

// 실행 환경 설정
// 실제 실행(prod)은 db/ 폴더를, 테스트(test)는 test_db/ 폴더를 쓴다 → 테스트가 실제 데이터를 건드리지 않는다
public class AppConfig {
    private static String mode = "prod";

    public static void setModeToTest() {
        mode = "test";
    }

    public static boolean isTestMode() {
        return mode.equals("test");
    }

    public static String getDbDirPath() {
        return isTestMode() ? "test_db/wiseSaying" : "db/wiseSaying";
    }
}

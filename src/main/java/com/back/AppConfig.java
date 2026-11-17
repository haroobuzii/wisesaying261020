package com.back;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// 실행 환경 설정
// - mode     : prod(실제 실행) / test(테스트)  → 테스트는 테스트 전용 DB·폴더를 쓴다
// - storage  : db(MySQL, 기본) / file(파일)   → 어떤 저장소 구현체를 쓸지
// - DB 접속 정보: 소스에 쓰지 않고 환경변수 또는 db.properties(git 에 안 올라감)에서 읽는다
public class AppConfig {
    public enum Storage { DB, FILE }

    private static String mode = "prod";
    private static Storage storage = Storage.DB;
    private static Properties dbProperties;

    public static void setModeToTest() {
        mode = "test";
    }

    public static boolean isTestMode() {
        return mode.equals("test");
    }

    public static Storage getStorage() {
        return storage;
    }

    public static void setStorage(Storage storage) {
        AppConfig.storage = storage;
    }

    // 파일 저장소 폴더, data.json 빌드 위치
    public static String getDbDirPath() {
        return isTestMode() ? "test_db/wiseSaying" : "db/wiseSaying";
    }

    // ---------- MySQL 접속 정보 ----------

    public static String getDbHost() {
        return getDbProperty("DB_HOST", "localhost");
    }

    public static int getDbPort() {
        return Integer.parseInt(getDbProperty("DB_PORT", "3306"));
    }

    public static String getDbUsername() {
        return getDbProperty("DB_USERNAME", "root");
    }

    public static String getDbPassword() {
        return getDbProperty("DB_PASSWORD", "");
    }

    // 개발 DB 와 테스트 DB 를 나눈다
    public static String getDbName() {
        return isTestMode()
                ? getDbProperty("DB_NAME_TEST", "wiseSaying__test")
                : getDbProperty("DB_NAME", "wiseSaying__dev");
    }

    // 1순위: 환경변수  2순위: db.properties  3순위: 기본값
    private static String getDbProperty(String key, String defaultValue) {
        String env = System.getenv(key);

        if (env != null && !env.isBlank()) return env;

        return loadDbProperties().getProperty(key, defaultValue);
    }

    private static Properties loadDbProperties() {
        if (dbProperties != null) return dbProperties;

        Properties properties = new Properties();
        Path path = Path.of("db.properties");

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        dbProperties = properties;

        return dbProperties;
    }
}

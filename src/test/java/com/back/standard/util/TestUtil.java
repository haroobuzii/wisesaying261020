package com.back.standard.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;
import java.util.stream.Stream;

public final class TestUtil {
    private static PrintStream originalOut;
    private static PrintStream captureOut;

    // 문자열을 키보드 입력처럼 읽는 Scanner 를 만든다
    public static Scanner genScanner(String input) {
        return new Scanner(input);
    }

    // System.out 출력을 화면 대신 메모리(ByteArrayOutputStream)에 쌓도록 바꾼다
    public static ByteArrayOutputStream setOutToByteArray() {
        if (captureOut != null) throw new IllegalStateException("capture already active");
        originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        captureOut = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        System.setOut(captureOut);
        return bytes;
    }

    // 폴더를 안의 파일까지 통째로 지운다 (없으면 아무것도 안 함)
    public static void deleteDir(Path dirPath) {
        if (!Files.exists(dirPath)) return;

        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.sorted(Comparator.reverseOrder()) // 안쪽 파일부터 지워야 폴더를 지울 수 있다
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // System.out 을 원래 화면 출력으로 되돌린다
    public static void clearSetOutToByteArray() {
        if (captureOut == null) return;
        System.setOut(originalOut);
        captureOut.close();
        captureOut = null;
        originalOut = null;
    }
}

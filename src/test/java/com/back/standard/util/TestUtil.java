package com.back.standard.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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

    // System.out 을 원래 화면 출력으로 되돌린다
    public static void clearSetOutToByteArray() {
        if (captureOut == null) return;
        System.setOut(originalOut);
        captureOut.close();
        captureOut = null;
        originalOut = null;
    }
}

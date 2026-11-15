package com.back;

import com.back.domain.system.controller.SystemController;
import com.back.domain.wiseSaying.controller.WiseSayingController;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.domain.wiseSaying.service.WiseSayingService;

import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class AppContext {
    public static final DateTimeFormatter forPrintDateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");

    public static Scanner scanner;
    public static WiseSayingRepository wiseSayingRepository;
    public static WiseSayingService wiseSayingService;
    public static WiseSayingController wiseSayingController;
    public static SystemController systemController;

    // 앱을 시작할 때마다 부품을 전부 새로 만든다
    // → 테스트마다 비어 있는 저장소(번호도 1부터)로 시작할 수 있다
    // 순서가 중요하다: 서비스는 저장소를, 컨트롤러는 스캐너·서비스를 AppContext 에서 가져간다
    public static void renew(Scanner scanner) {
        AppContext.scanner = scanner;
        wiseSayingRepository = new WiseSayingRepository();
        wiseSayingService = new WiseSayingService();
        wiseSayingController = new WiseSayingController();
        systemController = new SystemController();
    }
}

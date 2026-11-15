package com.back;

import com.back.domain.system.controller.SystemController;
import com.back.domain.wiseSaying.controller.WiseSayingController;
import com.back.domain.wiseSaying.service.WiseSayingService;

import java.util.Scanner;

public class App {
    private final Scanner scanner;
    private final SystemController systemController;
    private final WiseSayingController wiseSayingController;
    private final WiseSayingService wiseSayingService;
    private final boolean makeSampleData;

    // 입력을 받을 Scanner 를 밖에서 넣어준다
    // 실제 실행: 키보드(System.in) / 테스트: 문자열
    // makeSampleData: 시작할 때 명언이 0개면 샘플 10개를 만들지 여부
    public App(Scanner scanner, boolean makeSampleData) {
        AppContext.renew(scanner);

        this.scanner = AppContext.scanner;
        this.systemController = AppContext.systemController;
        this.wiseSayingController = AppContext.wiseSayingController;
        this.wiseSayingService = AppContext.wiseSayingService;
        this.makeSampleData = makeSampleData;
    }

    // 테스트(CRUD)용: 샘플 없이 빈 저장소로 시작한다
    public App(Scanner scanner) {
        this(scanner, false);
    }

    public void run() {
        if (makeSampleData) {
            wiseSayingService.makeSampleDataIfEmpty();
        }

        System.out.println("== 명언 앱 ==");

        while (true) {
            System.out.print("명령) ");
            String cmd = scanner.nextLine().trim();
            Rq rq = new Rq(cmd);

            switch (rq.getActionName()) {
                case "종료" -> {
                    systemController.actionExit();
                    return;
                }
                case "목록" -> wiseSayingController.actionList(rq);
                case "등록" -> wiseSayingController.actionWrite();
                case "삭제" -> wiseSayingController.actionDelete(rq);
                case "수정" -> wiseSayingController.actionModify(rq);
            }
        }
    }
}

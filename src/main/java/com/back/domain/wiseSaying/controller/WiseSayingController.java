package com.back.domain.wiseSaying.controller;

import com.back.AppContext;
import com.back.Rq;
import com.back.WiseSaying;
import com.back.domain.wiseSaying.service.WiseSayingService;
import com.back.standard.dto.Page;

import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WiseSayingController {
    private final Scanner scanner = AppContext.scanner;
    private final WiseSayingService wiseSayingService = AppContext.wiseSayingService;

    public void actionBuild() {
        wiseSayingService.build();

        System.out.println("data.json 파일의 내용이 갱신되었습니다.");
    }

    public void actionList(Rq rq) {
        String keywordType = rq.getParam("keywordType", "content");
        String keyword = rq.getParam("keyword", "");
        int page = Math.max(1, rq.getParamAsInt("page", 1));

        // 검색어가 있을 때만 검색 조건을 목록 위에 보여준다
        if (!keyword.isBlank()) {
            System.out.println("----------------------");
            System.out.println("검색타입 : " + keywordType);
            System.out.println("검색어 : " + keyword);
            System.out.println("----------------------");
        }

        System.out.println("번호 / 작가 / 명언");
        System.out.println("----------------------");

        Page<WiseSaying> wiseSayingPage = wiseSayingService.findForList(keywordType, keyword, page);

        for (WiseSaying wiseSaying : wiseSayingPage.content()) {
            System.out.printf(
                    "%d / %s / %s\n",
                    wiseSaying.getId(),
                    wiseSaying.getAuthor(),
                    wiseSaying.getContent()
            );
        }

        System.out.println("----------------------");
        System.out.println("페이지 : " + pageMenu(wiseSayingPage));
    }

    // 예) 전체 2페이지 중 1페이지 → "[1] / 2"
    private String pageMenu(Page<WiseSaying> wiseSayingPage) {
        return IntStream.rangeClosed(1, wiseSayingPage.totalPages())
                .mapToObj(i -> i == wiseSayingPage.page() ? "[" + i + "]" : String.valueOf(i))
                .collect(Collectors.joining(" / "));
    }

    public void actionWrite() {
        System.out.print("명언 : ");
        String content = scanner.nextLine().trim();
        System.out.print("작가 : ");
        String author = scanner.nextLine().trim();

        WiseSaying wiseSaying = wiseSayingService.write(content, author);

        System.out.println("%d번 명언이 등록되었습니다.".formatted(wiseSaying.getId()));
    }

    public void actionDelete(Rq rq) {
        int id = rq.getParamAsInt("id", -1);

        if (id == -1) {
            System.out.println("id를 숫자로 입력해주세요.");
            return;
        }

        boolean deleted = wiseSayingService.delete(id);

        if (!deleted) {
            System.out.println("%d번 명언은 존재하지 않습니다.".formatted(id));
            return;
        }

        System.out.println("%d번 명언이 삭제되었습니다.".formatted(id));
    }

    public void actionModify(Rq rq) {
        int id = rq.getParamAsInt("id", -1);

        if (id == -1) {
            System.out.println("id를 숫자로 입력해주세요.");
            return;
        }

        WiseSaying wiseSaying = wiseSayingService.findById(id);

        if (wiseSaying == null) {
            System.out.println("%d번 명언은 존재하지 않습니다.".formatted(id));
            return;
        }

        System.out.printf("명언(기존) : %s\n", wiseSaying.getContent());
        System.out.print("명언 : ");
        String content = scanner.nextLine().trim();

        System.out.printf("작가(기존) : %s\n", wiseSaying.getAuthor());
        System.out.print("작가 : ");
        String author = scanner.nextLine().trim();

        wiseSayingService.modify(wiseSaying, content, author);
    }
}
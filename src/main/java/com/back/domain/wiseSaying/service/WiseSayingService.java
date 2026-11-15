package com.back.domain.wiseSaying.service;


import com.back.AppContext;
import com.back.WiseSaying;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.standard.dto.Page;

public class WiseSayingService {
    private static final int PAGE_SIZE = 5;
    private static final int SAMPLE_COUNT = 10;

    private final WiseSayingRepository wiseSayingRepository = AppContext.wiseSayingRepository;

    public Page<WiseSaying> findForList(String keywordType, String keyword, int page) {
        return wiseSayingRepository.findForList(keywordType, keyword, page, PAGE_SIZE);
    }

    // 명언이 하나도 없을 때만 샘플 10개(명언 1~10 / 작자미상 1~10)를 만든다
    public void makeSampleDataIfEmpty() {
        if (wiseSayingRepository.count() > 0) return;

        for (int i = 1; i <= SAMPLE_COUNT; i++) {
            write("명언 " + i, "작자미상 " + i);
        }
    }

    public WiseSaying write(String content, String author) {
        WiseSaying wiseSaying = new WiseSaying(content, author);

        wiseSayingRepository.save(wiseSaying);

        return wiseSaying;
    }

    public WiseSaying findById(int id) {
        return wiseSayingRepository.findById(id);
    }

    public void modify(WiseSaying wiseSaying, String content, String author) {
        wiseSaying.setContent(content);
        wiseSaying.setAuthor(author);

        wiseSayingRepository.save(wiseSaying);
    }

    public boolean delete(int id) {
        return wiseSayingRepository.deleteById(id);
    }

    public String build() {
        return wiseSayingRepository.build();
    }
}

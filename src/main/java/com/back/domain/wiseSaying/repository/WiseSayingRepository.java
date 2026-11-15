package com.back.domain.wiseSaying.repository;

import com.back.WiseSaying;
import com.back.standard.dto.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class WiseSayingRepository {
    private int lastId = 0;
    private final List<WiseSaying> wiseSayings = new ArrayList<>();

    // 검색 조건에 맞는 명언을 최신순으로 정렬한 뒤, 요청한 페이지 분량만 잘라서 돌려준다
    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        List<WiseSaying> filtered = wiseSayings
                .reversed()
                .stream()
                .filter(wiseSaying -> matches(wiseSaying, keywordType, keyword))
                .toList();

        List<WiseSaying> content = filtered
                .stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .toList();

        return new Page<>(content, page, pageSize, filtered.size());
    }

    // 검색어가 없으면 전부 통과. 있으면 검색타입에 따라 내용 또는 작가에 검색어가 들어 있는지 본다
    private boolean matches(WiseSaying wiseSaying, String keywordType, String keyword) {
        if (keyword.isBlank()) return true;

        return switch (keywordType) {
            case "author" -> wiseSaying.getAuthor().contains(keyword);
            case "content" -> wiseSaying.getContent().contains(keyword);
            default -> wiseSaying.getContent().contains(keyword) || wiseSaying.getAuthor().contains(keyword);
        };
    }

    public int count() {
        return wiseSayings.size();
    }

    public void save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            // 최초 저장
            wiseSaying.setId(++lastId);
            LocalDateTime now = LocalDateTime.now();
            wiseSaying.setCreateDate(now);
            wiseSaying.setModifyDate(now);
            wiseSayings.add(wiseSaying);
        } else {
            // 수정
            wiseSaying.setModifyDate(LocalDateTime.now());
        }
    }

    public WiseSaying findById(int id) {
        return wiseSayings
                .stream()
                .filter(wiseSaying -> wiseSaying.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public boolean deleteById(int id) {
        int index = findIndexById(id);

        if (index == -1) return false;

        wiseSayings.remove(index);

        return true;
    }

    private int findIndexById(int id) {
        return IntStream.range(0, wiseSayings.size())
                .filter(i -> wiseSayings.get(i).getId() == id)
                .findFirst()
                .orElse(-1);
    }
}

package com.back.domain.wiseSaying.repository;

import com.back.WiseSaying;
import com.back.standard.dto.Page;

import java.util.List;

// 저장소가 해야 할 일의 "약속"
// 메모리에 저장하든(WiseSayingMemoryRepository) 파일에 저장하든(WiseSayingFileRepository)
// 서비스는 이 약속만 보고 쓰기 때문에, 구현체만 갈아 끼우면 된다
public interface WiseSayingRepository {
    // 모든 명언 (번호 오름차순)
    List<WiseSaying> findAll();

    // 새 명언이면 번호를 붙여 저장하고, 기존 명언이면 덮어쓴다
    void save(WiseSaying wiseSaying);

    WiseSaying findById(int id);

    boolean deleteById(int id);

    // 모든 명언을 하나로 모아 내보내고(빌드), 만들어진 파일 경로를 돌려준다
    String build();

    default int count() {
        return findAll().size();
    }

    // 검색 조건에 맞는 명언을 최신순으로 정렬한 뒤, 요청한 페이지 분량만 잘라서 돌려준다
    // 저장 방식과 상관없는 로직이라 인터페이스에 한 번만 작성한다
    default Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        List<WiseSaying> filtered = findAll()
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
    private static boolean matches(WiseSaying wiseSaying, String keywordType, String keyword) {
        if (keyword.isBlank()) return true;

        return switch (keywordType) {
            case "author" -> wiseSaying.getAuthor().contains(keyword);
            case "content" -> wiseSaying.getContent().contains(keyword);
            default -> wiseSaying.getContent().contains(keyword) || wiseSaying.getAuthor().contains(keyword);
        };
    }
}

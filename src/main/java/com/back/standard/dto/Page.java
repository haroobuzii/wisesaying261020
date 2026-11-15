package com.back.standard.dto;

import java.util.List;

// 한 페이지 분량의 결과
// content: 이 페이지에 보여줄 항목들 / page: 현재 페이지 번호(1부터) / pageSize: 한 페이지 최대 개수 / totalCount: 전체(검색 결과) 개수
public record Page<T>(List<T> content, int page, int pageSize, int totalCount) {
    // 전체 페이지 수. 결과가 0개여도 1페이지는 있다고 본다
    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}

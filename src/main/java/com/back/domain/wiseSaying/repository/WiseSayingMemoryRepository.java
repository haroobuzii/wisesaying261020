package com.back.domain.wiseSaying.repository;

import com.back.WiseSaying;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

// 메모리(List)에만 저장하는 저장소. 프로그램을 끄면 사라진다
public class WiseSayingMemoryRepository implements WiseSayingRepository {
    private int lastId = 0;
    private final List<WiseSaying> wiseSayings = new ArrayList<>();

    @Override
    public List<WiseSaying> findAll() {
        return List.copyOf(wiseSayings);
    }

    @Override
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

    @Override
    public WiseSaying findById(int id) {
        return wiseSayings
                .stream()
                .filter(wiseSaying -> wiseSaying.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean deleteById(int id) {
        int index = findIndexById(id);

        if (index == -1) return false;

        wiseSayings.remove(index);

        return true;
    }

    @Override
    public String build() {
        throw new UnsupportedOperationException("메모리 저장소는 빌드를 지원하지 않습니다.");
    }

    private int findIndexById(int id) {
        return IntStream.range(0, wiseSayings.size())
                .filter(i -> wiseSayings.get(i).getId() == id)
                .findFirst()
                .orElse(-1);
    }
}

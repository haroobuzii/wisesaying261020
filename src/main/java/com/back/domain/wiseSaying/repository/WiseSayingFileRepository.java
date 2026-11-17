package com.back.domain.wiseSaying.repository;

import com.back.WiseSaying;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 명언 하나를 파일 하나({번호}.json)로 저장하는 저장소. 프로그램을 껐다 켜도 남는다
//   {dirPath}/1.json, 2.json ...  : 명언
//   {dirPath}/lastId.txt          : 마지막으로 만든 번호 (삭제해도 줄지 않는다 → 번호 재사용 안 함)
//   {dirPath}/data.json           : 빌드 결과 (모든 명언 배열)
public class WiseSayingFileRepository implements WiseSayingRepository {
    private static final Pattern WISE_SAYING_FILE_NAME = Pattern.compile("\\d+\\.json");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private final Path dirPath;

    public WiseSayingFileRepository(String dirPath) {
        this.dirPath = Path.of(dirPath);
    }

    @Override
    public List<WiseSaying> findAll() {
        if (!Files.exists(dirPath)) return List.of();

        try (Stream<Path> files = Files.list(dirPath)) {
            return files
                    .filter(path -> WISE_SAYING_FILE_NAME.matcher(path.getFileName().toString()).matches())
                    .map(this::readWiseSaying)
                    .sorted(Comparator.comparingInt(WiseSaying::getId))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void save(WiseSaying wiseSaying) {
        LocalDateTime now = LocalDateTime.now();

        if (wiseSaying.isNew()) {
            // 최초 저장: 마지막 번호 + 1 을 붙이고, 마지막 번호를 갱신한다
            int newId = getLastId() + 1;
            wiseSaying.setId(newId);
            wiseSaying.setCreateDate(now);
            writeString(dirPath.resolve("lastId.txt"), String.valueOf(newId));
        }

        wiseSaying.setModifyDate(now);
        writeString(filePathOf(wiseSaying.getId()), toJson(wiseSaying));
    }

    @Override
    public WiseSaying findById(int id) {
        Path path = filePathOf(id);

        if (!Files.exists(path)) return null;

        return readWiseSaying(path);
    }

    @Override
    public boolean deleteById(int id) {
        try {
            return Files.deleteIfExists(filePathOf(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String build() {
        Path dataPath = dirPath.resolve("data.json");
        writeString(dataPath, toJsonArray(findAll()));

        return dataPath.toString();
    }

    public int getLastId() {
        Path path = dirPath.resolve("lastId.txt");

        if (!Files.exists(path)) return 0;

        return Integer.parseInt(readString(path).trim());
    }

    private Path filePathOf(int id) {
        return dirPath.resolve(id + ".json");
    }

    private WiseSaying readWiseSaying(Path path) {
        return fromJson(readString(path));
    }

    // ---------- 파일 읽기/쓰기 ----------

    private String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeString(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- JSON 문자열 직접 만들기/읽기 (라이브러리 없이) ----------

    // {
    //   "id": 1,
    //   "content": "명언 1",
    //   "author": "작가 1"
    // }
    static String toJson(WiseSaying wiseSaying) {
        return """
                {
                  "id": %d,
                  "content": "%s",
                  "author": "%s"
                }""".formatted(wiseSaying.getId(), escape(wiseSaying.getContent()), escape(wiseSaying.getAuthor()));
    }

    static WiseSaying fromJson(String json) {
        Matcher idMatcher = ID_PATTERN.matcher(json);

        if (!idMatcher.find()) throw new IllegalArgumentException("id 가 없는 JSON 입니다: " + json);

        WiseSaying wiseSaying = new WiseSaying(readStringField(json, "content"), readStringField(json, "author"));
        wiseSaying.setId(Integer.parseInt(idMatcher.group(1)));

        return wiseSaying;
    }

    // "필드이름": "값" 에서 값을 꺼낸다. 값 안의 \" 같은 이스케이프도 처리한다
    private static String readStringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) throw new IllegalArgumentException(fieldName + " 가 없는 JSON 입니다: " + json);

        return unescape(matcher.group(1));
    }

    // 문자열 안의 " 나 \ 가 JSON 을 깨뜨리지 않도록 앞에 \ 를 붙인다
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescape(String value) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c != '\\' || i == value.length() - 1) {
                sb.append(c);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                default -> sb.append(next); // \" → ", \\ → \
            }
        }

        return sb.toString();
    }

    // 명언 목록 → data.json 형식의 JSON 배열 (DB 저장소의 빌드에서도 재사용)
    static String toJsonArray(List<WiseSaying> wiseSayings) {
        return wiseSayings
                .stream()
                .map(wiseSaying -> indent(toJson(wiseSaying)))
                .collect(Collectors.joining(",\n", "[\n", "\n]"));
    }

    // data.json 배열 안에 넣을 때 한 단계(공백 2칸) 들여쓴다
    private static String indent(String json) {
        return json.lines()
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
    }
}

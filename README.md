# IT·기술 영상 만들기

## 유튜브 채널

- 채널 URL: https://www.youtube.com/@%EA%B9%9C%EA%B9%9C%EC%BD%94%EB%94%A9

## 제출 영상

- 주제: 자바 시작 코드 배우기
- 영상 URL: https://www.youtube.com/shorts/BUeXEeZko0s

---

# 명언 앱 — MySQL 전환 (15단계)

명언 저장소를 **파일 → MySQL** 로 바꿨습니다. 컨트롤러·서비스는 그대로 두고, 리포지터리 구현체만 갈아 끼웠습니다.

## 1. MySQL 띄우기

```bash
docker run --name mysql-84 -p 127.0.0.1:3306:3306 -e TZ=Asia/Seoul -e MYSQL_ROOT_PASSWORD=<비밀번호> -d mysql:8.4
docker exec mysql-84 mysqladmin ping -uroot -p
```

## 2. DB·테이블 만들기

테이블 생성 SQL: [`sql/schema.sql`](sql/schema.sql) — 개발용 `wiseSaying__dev`, 테스트용 `wiseSaying__test` DB 와 `wise_saying` 테이블을 만듭니다.

```bash
docker exec -i mysql-84 mysql -uroot -p < sql/schema.sql
```

```sql
CREATE TABLE IF NOT EXISTS wise_saying (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT, -- 삭제한 번호는 재사용하지 않음
    createDate DATETIME     NOT NULL,
    modifyDate DATETIME     NOT NULL,
    content    TEXT         NOT NULL,
    author     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);
```

## 3. 접속 정보 설정 (소스에 비밀번호를 쓰지 않음)

`db.properties.example` 을 `db.properties` 로 복사해 값을 채웁니다. `db.properties` 는 `.gitignore` 에 있어 git 에 올라가지 않습니다.
같은 이름의 **환경변수**(`DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `DB_NAME_TEST`)가 있으면 환경변수가 우선합니다.

```bash
cp db.properties.example db.properties   # 그리고 DB_PASSWORD 채우기
./gradlew test                            # 테스트는 wiseSaying__test DB 사용
```

## 4. 리포지터리를 어떻게 갈아 끼웠나

```
WiseSayingController → WiseSayingService → WiseSayingRepository (인터페이스)
                                              ├─ WiseSayingMemoryRepository  (List, 11단계)
                                              ├─ WiseSayingFileRepository    (db/wiseSaying/*.json, 9단계)
                                              └─ WiseSayingDbRepository      (MySQL, 15단계) ← 기본
```

- 서비스는 `WiseSayingRepository` **인터페이스**만 알고 있습니다. 어떤 구현체를 쓸지는 `AppContext.renew()` 한 곳에서 정합니다 (`AppConfig.getStorage()`, 기본값 DB).
- `WiseSayingDbRepository` 는 앞 과제에서 만든 **SimpleDb**(`com.back.standard.simpleDb`, JDBC 래퍼)를 재사용합니다. SQL 은 이 클래스에만 있습니다.
- 검색·페이징은 SQL 로 처리합니다: `WHERE content|author LIKE CONCAT('%', ?, '%')`, `ORDER BY id DESC`, `LIMIT ? OFFSET ?`, 전체 개수는 `COUNT(*)`.
- 번호는 MySQL `AUTO_INCREMENT` 가 붙이므로 재시작해도 이어지고, 삭제한 번호는 재사용하지 않습니다.
- `빌드` 는 DB 의 모든 명언을 `db/wiseSaying/data.json` 으로 내보냅니다 (형식은 파일 저장소와 동일).

## 5. 테스트

- 테스트는 **테스트 DB(`wiseSaying__test`)** 를 쓰고, 매 테스트 전에 `TRUNCATE wise_saying` 으로 비웁니다 (`AppTestRunner.clearDb()`).
- 기존 통합 테스트(등록·목록·삭제·수정·검색·페이징)가 그대로 MySQL 저장소로 통과합니다.
- `WiseSayingDbTest`: 테이블 저장, 재시작 후 목록·번호 유지, 삭제 번호 재사용 안 함, 수정·삭제 반영, SQL 검색·페이징, data.json 빌드.
- `WiseSayingFileTest`: 파일 저장소 구현체는 그대로 남아 있어, 이 테스트 동안만 파일 저장소로 바꿔 검증합니다.

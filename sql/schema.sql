-- 명언 앱 MySQL 스키마
-- 개발용(wiseSaying__dev)과 테스트용(wiseSaying__test) DB 를 따로 만든다
-- 실행: docker exec -i mysql-84 mysql -uroot -p < sql/schema.sql

CREATE DATABASE IF NOT EXISTS wiseSaying__dev CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS wiseSaying__test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 개발 DB
USE wiseSaying__dev;

CREATE TABLE IF NOT EXISTS wise_saying (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT, -- 명언 번호. 삭제해도 재사용하지 않는다
    createDate DATETIME     NOT NULL,                -- 작성 날짜
    modifyDate DATETIME     NOT NULL,                -- 수정 날짜
    content    TEXT         NOT NULL,                -- 명언 내용
    author     VARCHAR(100) NOT NULL,                -- 작가
    PRIMARY KEY (id)
);

-- 테스트 DB (구조는 개발 DB 와 같다)
USE wiseSaying__test;

CREATE TABLE IF NOT EXISTS wise_saying (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME     NOT NULL,
    modifyDate DATETIME     NOT NULL,
    content    TEXT         NOT NULL,
    author     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

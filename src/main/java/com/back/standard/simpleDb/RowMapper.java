package com.back.standard.simpleDb;

import java.lang.reflect.Field;
import java.util.Map;

// DB 한 행(Map: 컬럼이름 → 값)을 원하는 클래스의 객체로 바꿔 준다
// 규칙: 컬럼 이름과 같은 이름의 필드에 값을 넣는다 (예: 컬럼 "title" → 필드 title)
class RowMapper {
    static <T> T map(Map<String, Object> row, Class<T> cls) {
        try {
            // 1. 기본 생성자로 빈 객체를 만든다 (Article 에는 @NoArgsConstructor 가 있다)
            T obj = cls.getDeclaredConstructor().newInstance();

            // 2. 컬럼마다 같은 이름의 필드를 찾아 값을 넣는다 (리플렉션)
            for (Map.Entry<String, Object> column : row.entrySet()) {
                Field field = findField(cls, column.getKey());

                if (field == null) continue; // 클래스에 없는 컬럼은 무시

                field.setAccessible(true); // private 필드도 값을 넣을 수 있게
                field.set(obj, convert(column.getValue(), field.getType()));
            }

            return obj;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(cls.getName() + " 객체로 변환하지 못했습니다.", e);
        }
    }

    // 부모 클래스까지 올라가며 필드를 찾는다
    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }

        return null;
    }

    // DB 에서 온 값의 타입을 필드 타입에 맞춘다 (예: Integer → Long, 숫자 1 → true)
    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            return type == boolean.class ? false : null; // 기본형 boolean 에는 null 을 넣을 수 없다
        }

        if (type == Long.class || type == long.class) {
            return Long.valueOf(((Number) value).longValue());
        }

        if (type == Integer.class || type == int.class) {
            return ((Number) value).intValue();
        }

        if (type == Boolean.class || type == boolean.class) {
            if (value instanceof Number number) return number.longValue() != 0;
            return (Boolean) value;
        }

        if (type == String.class) {
            return value.toString();
        }

        return value; // LocalDateTime 등은 드라이버가 준 타입 그대로
    }
}

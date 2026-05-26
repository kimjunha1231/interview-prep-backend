package com.junha.interview.common;

/**
 * 이메일 주소 마스킹 유틸리티.
 * 로그 출력 시 개인정보를 보호하기 위해 로컬 파트의 앞 2글자만 노출합니다.
 * 예: "example@gmail.com" → "ex***@gmail.com"
 */
public final class EmailMaskUtils {

    private EmailMaskUtils() {
        // Utility class — prevent instantiation
    }

    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}

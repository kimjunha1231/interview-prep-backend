package com.junha.interview.common;

/**
 * HTML 특수문자 이스케이프 유틸리티.
 * XSS 공격 방지를 위해 사용자 입력을 HTML 엔티티로 변환합니다.
 */
public final class HtmlEscapeUtils {

    private HtmlEscapeUtils() {
        // Utility class — prevent instantiation
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * HTML 이스케이프 + 개행 문자를 &lt;br/&gt; 태그로 변환.
     * 이메일 본문 등에서 줄바꿈을 유지하기 위해 사용합니다.
     */
    public static String escapeWithLineBreaks(String text) {
        return escape(text).replace("\n", "<br/>");
    }
}

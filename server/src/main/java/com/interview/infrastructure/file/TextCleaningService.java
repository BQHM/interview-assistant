package com.interview.infrastructure.file;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 文本清理服务：AI 的“初滤网”
 * 作用：去除解析文档时产生的杂质（乱码、控制字符、多余空行等）
 */
@Service
public class TextCleaningService {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("(\\n)\\s*(\\n)+");

    public String clean(String rawText){

        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String cleaned = rawText.replace("\r\n", "\n").replace('\r', '\n');
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("");
        cleaned = MULTI_BLANK_LINES.matcher(cleaned).replaceAll("\n\n");

        return cleaned.trim();

    }

}

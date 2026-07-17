package com.interview.infrastructure.file;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 文件功能说明
 * <p>负责清洗解析后的文本。</p>
 *
 * @author NobuNo
 * @date 2026-04-08
 */
@Service
public class TextCleaningService {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("(\\n)\\s*(\\n)+");

    /**
     * 功能说明
     * <p>清洗文本内容。</p>
     *
     * @param rawText 原始文本
     * @return 清洗后的文本
     * @author NobuNo
     * @date 2026-04-08
     */
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

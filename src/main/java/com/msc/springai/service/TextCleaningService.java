package com.msc.springai.service;

import org.springframework.stereotype.Service;

@Service
public class TextCleaningService {

    public String clean(String text) {
        System.out.println("[TextCleaningService] Start cleaning text.");

        if (text == null) {
            System.out.println("[TextCleaningService] Input text is null.");
            return "";
        }

        System.out.println("[TextCleaningService] Original text length: " + text.length());

        String cleaned = text
                // 去掉 null 字符
                .replace("\u0000", " ")

                // tab、垂直 tab、换页符、回车统一处理成空格
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")

                // 多个空格压缩成一个空格
                .replaceAll(" {2,}", " ")

                // 3 个以上换行压缩成 2 个换行，保留段落感
                .replaceAll("\\n{3,}", "\n\n")

                // 每一行去掉首尾空格
                .replaceAll("(?m)^\\s+|\\s+$", "")

                .trim();

        System.out.println("[TextCleaningService] Cleaned text length: " + cleaned.length());

        if (!cleaned.isBlank()) {
            int previewLength = Math.min(200, cleaned.length());
            System.out.println("[TextCleaningService] Cleaned preview: "
                    + cleaned.substring(0, previewLength));
        }

        return cleaned;
    }

    public boolean isUsable(String text) {
        if (text == null) {
            System.out.println("[TextCleaningService] Text is not usable: null");
            return false;
        }

        String cleaned = clean(text);

        boolean usable = cleaned.length() >= 50;

        System.out.println("[TextCleaningService] Is usable: " + usable
                + ", length: " + cleaned.length());

        return usable;
    }
}
package com.interview.enums;

/**
 * 题目难度：EASY/MEDIUM/HARD，兼容中文入参归一化。
 */
public enum Difficulty {
    EASY("EASY", "简单"),
    MEDIUM("MEDIUM", "中等"),
    HARD("HARD", "困难");

    private final String code;
    private final String label;

    Difficulty(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 入参归一化：空/非法值默认 MEDIUM，支持 EASY/简单、HARD/困难、MEDIUM/中等。
     */
    public static Difficulty normalize(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return switch (value.trim().toUpperCase()) {
            case "EASY", "简单" -> EASY;
            case "HARD", "困难" -> HARD;
            case "MEDIUM", "中等" -> MEDIUM;
            default -> MEDIUM;
        };
    }
}

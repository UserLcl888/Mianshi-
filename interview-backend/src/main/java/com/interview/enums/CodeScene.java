package com.interview.enums;

/**
 * 邮箱验证码场景。
 */
public enum CodeScene {
    LOGIN("login"),
    RESET("reset");

    private final String value;

    CodeScene(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CodeScene fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CodeScene scene : values()) {
            if (scene.value.equals(value.trim())) {
                return scene;
            }
        }
        return null;
    }
}

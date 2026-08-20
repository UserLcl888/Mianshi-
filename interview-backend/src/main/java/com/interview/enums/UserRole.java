package com.interview.enums;

/**
 * 用户角色。
 */
public enum UserRole {
    USER("USER"),
    ADMIN("ADMIN");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

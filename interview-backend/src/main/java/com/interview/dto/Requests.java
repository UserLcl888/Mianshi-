package com.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class Requests {

    @Data
    public static class LoginDTO {
        @NotBlank(message = "请输入用户名")
        private String username;
        @NotBlank(message = "请输入密码")
        private String password;
    }

    @Data
    public static class RegisterDTO {
        @NotBlank(message = "请输入用户名")
        @Size(min = 3, max = 20, message = "用户名长度为 3~20 位")
        private String username;
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 32, message = "密码长度为 6~32 位")
        private String password;
        private String nickname;
    }

    @Data
    public static class ChangePasswordDTO {
        @NotBlank(message = "请输入旧密码")
        private String oldPassword;
        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 32, message = "密码长度为 6~32 位")
        private String newPassword;
    }

    @Data
    public static class ArticleSaveDTO {
        @NotBlank(message = "请输入标题")
        private String title;
        private String slug;
        private String summary;
        @NotNull(message = "请选择所属分类")
        private Long categoryId;
        private String difficulty = "MEDIUM";
        private List<String> tags;
        private String contentMd;
    }

    @Data
    public static class UserUpdateDTO {
        private String nickname;
        private String email;
        private String role;
    }

    @Data
    public static class UserCreateDTO {
        @NotBlank(message = "请输入用户名")
        private String username;
        @NotBlank(message = "请输入密码")
        private String password;
        private String nickname;
        private String role;
    }

    @Data
    public static class StatusDTO {
        @NotNull(message = "请传入状态")
        private Integer status;
    }

    @Data
    public static class ResetPasswordDTO {
        @NotBlank(message = "请输入新密码")
        private String newPassword;
    }

    @Data
    public static class CategorySaveDTO {
        @NotBlank(message = "请输入分类名")
        private String name;
        @NotBlank(message = "请输入 slug")
        private String slug;
        private Long parentId = 0L;
        private Integer sortOrder = 0;
        private String description;
    }

    @Data
    public static class TagSaveDTO {
        @NotBlank(message = "请输入标签名")
        private String name;
    }
}

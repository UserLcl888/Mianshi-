package com.interview.enums;

/**
 * 管理操作日志动作，动作名与对象类型一一绑定，避免调用方传错。
 */
public enum AdminLogAction {
    ARTICLE_CREATE(AdminLogTargetType.ARTICLE),
    ARTICLE_UPDATE(AdminLogTargetType.ARTICLE),
    ARTICLE_DELETE(AdminLogTargetType.ARTICLE),
    CATEGORY_CREATE(AdminLogTargetType.CATEGORY),
    CATEGORY_UPDATE(AdminLogTargetType.CATEGORY),
    CATEGORY_DELETE(AdminLogTargetType.CATEGORY),
    TAG_CREATE(AdminLogTargetType.TAG),
    TAG_UPDATE(AdminLogTargetType.TAG),
    TAG_DELETE(AdminLogTargetType.TAG),
    USER_CREATE(AdminLogTargetType.USER),
    USER_UPDATE(AdminLogTargetType.USER),
    USER_DISABLE(AdminLogTargetType.USER),
    USER_ENABLE(AdminLogTargetType.USER),
    USER_RESET_PASSWORD(AdminLogTargetType.USER),
    USER_DELETE(AdminLogTargetType.USER),
    UPLOAD_REPLY(AdminLogTargetType.UPLOAD),
    UPLOAD_DELETE(AdminLogTargetType.UPLOAD);

    private final AdminLogTargetType targetType;

    AdminLogAction(AdminLogTargetType targetType) {
        this.targetType = targetType;
    }

    public AdminLogTargetType getTargetType() {
        return targetType;
    }
}

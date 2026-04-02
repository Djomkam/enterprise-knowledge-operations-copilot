package io.innovation.ekoc.audit.domain;

public enum AuditAction {
    // Auth
    LOGIN,
    LOGOUT,
    REGISTER,
    PASSWORD_CHANGE,

    // Document
    DOCUMENT_UPLOAD,
    DOCUMENT_VIEW,
    DOCUMENT_DELETE,
    DOCUMENT_SHARE,

    // Chat
    CHAT_CREATE,
    CHAT_MESSAGE,
    CHAT_DELETE,

    // Admin
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    ROLE_ASSIGN,
    TEAM_CREATE,
    TEAM_UPDATE,
    TEAM_DELETE,

    // System
    CONFIG_CHANGE,
    EXPORT_DATA
}

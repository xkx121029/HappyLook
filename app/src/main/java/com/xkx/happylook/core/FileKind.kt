package com.xkx.happylook.core

/**
 * 文件类别。判定顺序：先看扩展名，再看 MIME，最后按首字节嗅探。
 * 之所以扩展名优先，是因为大量国产 App 分享文件时 MIME 一律给 application/octet-stream。
 */
enum class FileKind {
    TEXT,
    MARKDOWN,
    CODE,
    PDF,
    DOCX,
    LEGACY_DOC,
    ODT,
    RTF,
    XLSX,
    LEGACY_XLS,
    PPTX,
    IMAGE,
    ANIMATED_IMAGE,
    VIDEO,
    AUDIO,
    ZIP,
    EPUB,
    STL,
    FONT,
    UNKNOWN,
}

/** 一次查看任务的输入描述。contentUri 是唯一权威来源，本应用不持有任何存储权限。 */
data class ViewerTarget(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val kind: FileKind,
    /** 来源应用包名，仅用于“来自哪个应用”的展示，不落盘。 */
    val sourcePackage: String? = null,
)

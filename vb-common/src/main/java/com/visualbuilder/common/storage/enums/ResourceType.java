package com.visualbuilder.common.storage.enums;

import lombok.Getter;

@Getter
public enum ResourceType {
    IMAGE("image", "图片"),
    VIDEO("video", "视频"),
    AUDIO("audio", "音频"),
    FILE("file", "文件");

    private final String code;
    private final String desc;

    ResourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package com.visualbuilder.publish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RabbitMQ 发布任务消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishTaskMessage implements Serializable {

    /** 发布记录 ID */
    private Long recordId;

    /** 项目 ID */
    private Long projectId;

    /** 页面 ID */
    private Long pageId;

    /** 发布版本号 */
    private Integer versionNo;
}

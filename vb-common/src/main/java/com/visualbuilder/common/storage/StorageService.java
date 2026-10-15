package com.visualbuilder.common.storage;

import java.io.InputStream;

public interface StorageService {

    /**
     * 上传文件
     * @param stream 文件输入流
     * @param path 存储路径（如 image/20261010/uuid.png）
     * @param contentType MIME类型
     * @return 文件访问URL
     */
    String upload(InputStream stream, String path, String contentType);

    /**
     * 删除文件
     * @param path 存储路径
     */
    void delete(String path);

    /**
     * 获取预签名URL（临时访问）
     * @param path 存储路径
     * @param expireMinutes 有效期（分钟）
     * @return 预签名URL
     */
    String getPresignedUrl(String path, int expireMinutes);
}

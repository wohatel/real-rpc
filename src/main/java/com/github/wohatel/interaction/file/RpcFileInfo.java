package com.github.wohatel.interaction.file;

import lombok.Data;

/**
 * description
 *
 * @author yaochuang 2025/07/04 14:16
 */
@Data
public class RpcFileInfo {
    /**
     * 文件hash值
     */
    private String fileMd5;
    /**
     * 源文件名称
     */
    private String fileName;
    /**
     * 文件大小
     */
    private long length;

}

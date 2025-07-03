package com.murong.rpc.interaction.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
@RequiredArgsConstructor
public class RpcFileRemoteWrapper {
    /**
     * 文件位置
     */
    private final String filePath;
    /**
     * 文件是否追加或续传
     */
    private final RpcFileTransModel transModel;

}

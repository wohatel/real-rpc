package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
@RequiredArgsConstructor
public class RpcFileSenderWrapper {

    @Getter
    private final RpcSession rpcSession;

    @Getter
    private final File localFile;

    @Getter
    private final RpcFileTransModel transModel;

}

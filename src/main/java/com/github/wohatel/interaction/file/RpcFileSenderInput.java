package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.constant.NumberConstant;
import lombok.Builder;
import lombok.Getter;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
@Builder
@Getter
public class RpcFileSenderInput {
    @Builder.Default
    private RpcSession rpcSession = new RpcSession(NumberConstant.TEN_EIGHT_K);
    private RpcSessionContext context;
    private RpcFileTransConfig rpcFileTransConfig;
    private RpcFileSenderListener rpcFileSenderListener;
}

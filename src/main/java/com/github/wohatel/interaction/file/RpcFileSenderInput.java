package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RpcFileSenderInput {
    @Builder.Default
    private RpcSession rpcSession = new RpcSession(RpcNumberConstant.K_TEN_EIGHT);
    private RpcSessionContext context;
    private RpcFileTransConfig rpcFileTransConfig;
    private RpcFileSenderListener rpcFileSenderListener;
}

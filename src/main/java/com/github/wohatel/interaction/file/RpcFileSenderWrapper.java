package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class RpcFileSenderWrapper {

    @Getter
    private final RpcSession rpcSession;

    @Getter
    private final File localFile;

    @Getter
    private final RpcFileTransModel transModel;

}

package com.github.wohatel.util;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.util.internal.StringUtil;
import lombok.Data;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * description
 *
 * @author yaochuang 2025/03/21 15:16
 */
@Data
public class RpcInputStreamWrapper implements Closeable {
    private InputStream inputStream;
    private String name;
    private String usage;
    private String id;

    public void verify() {
        if (inputStream == null) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "inputStream is null or empty");
        }
        if (StringUtil.isNullOrEmpty(name)) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "name can't be null or empty");
        }
        if (StringUtil.isNullOrEmpty(id)) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "id is null or empty");
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}

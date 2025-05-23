package com.murong.rpc.util;

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
            throw new RuntimeException("inputStream 为null");
        }
        if (StringUtil.isNullOrEmpty(name)) {
            throw new RuntimeException("name");
        }
        if (StringUtil.isNullOrEmpty(id)) {
            throw new RuntimeException("id不能为空");
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}

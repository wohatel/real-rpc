package com.github.wohatel.interaction.base;

import com.github.wohatel.tcp.RpcDataReceiver;
import lombok.Data;

/**
 * description
 *
 * @author yaochuang 2025/06/09 10:07
 */
@Data
public abstract class RpcRelay {
    protected String origin = RpcDataReceiver.NODEID;
    protected boolean needCompress;
    protected String contentType; // 内容类型
    protected String header;  //  头部信息
    private String body;    //  body体

}

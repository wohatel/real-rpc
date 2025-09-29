package com.github.wohatel.interaction.base;

import com.github.wohatel.tcp.RpcDataReceiver;
import lombok.Data;

/**
 * description
 *
 * @author yaochuang 2025/06/09 10:07
 */
@Data
public class RpcRelay {
    protected String origin = RpcDataReceiver.NODEID;
    protected boolean enableCompress; // 数据传输过长中是否压缩
    protected String contentType; // 内容类型
    protected String header;  //  头部信息
    protected String body;    //  body体
}

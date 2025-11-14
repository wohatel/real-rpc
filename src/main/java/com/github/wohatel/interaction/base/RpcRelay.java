package com.github.wohatel.interaction.base;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author yaochuang 2025/06/09 10:07
 */
@Data
@Accessors(chain = true)
public class RpcRelay {
    protected String nodeId;
    protected boolean enableCompress; // 数据传输过长中是否压缩
    protected String contentType; // 内容类型
    protected String header;  //  头部信息
    protected String body;    //  body体
}

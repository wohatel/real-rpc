package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * @author yaochuang
 */
@Data
public class RpcSessionContext {
    private String topic;
    private long startTime;
    private List<String> matters;
    private JSONObject body;
}

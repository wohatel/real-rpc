package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yaochuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcSessionContext {
    private String topic;
    private long startTime;
    private List<String> matters;
    private String destPath;
    private JSONObject body;
}

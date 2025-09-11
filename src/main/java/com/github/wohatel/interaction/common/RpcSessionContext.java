package com.github.wohatel.interaction.common;

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
    /**
     * 会话主题
     */
    private String topic;
    /**
     * 会话事项
     */
    private List<String> matters;
    /**
     * 回话正文信息
     */
    private JSONObject parameters;
}

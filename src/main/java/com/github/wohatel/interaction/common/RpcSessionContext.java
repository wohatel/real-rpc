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

    private String topic;

    private List<String> matters;

    private JSONObject parameters;
}

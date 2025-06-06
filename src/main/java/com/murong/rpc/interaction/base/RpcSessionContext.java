package com.murong.rpc.interaction.base;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @author yaochuang
 */
@Getter
@RequiredArgsConstructor
public class RpcSessionContext {

    /**
     * 会话主题
     */
    private final String sessionTopic;

    /**
     * 会话类别
     */
    private final String sessionType;

    /**
     * 会话事项
     */
    private final List<String> sessionMetters;


    private final JSONObject context;
}

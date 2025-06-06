package com.murong.rpc.interaction.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yaochuang
 */
@Getter
@RequiredArgsConstructor
public class RpcSessionContext {

    public RpcSessionContext(String sessionTopic, String... metters) {
        this.sessionTopic = sessionTopic;
        this.sessionMetters = metters == null ? null : Arrays.stream(metters).toList();
    }

    /**
     * 会话主题
     */
    private final String sessionTopic;

    /**
     * 会话事项
     */
    private final List<String> sessionMetters;

}

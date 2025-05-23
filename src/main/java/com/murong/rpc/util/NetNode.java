package com.murong.rpc.util;

import lombok.Data;

import java.util.Objects;

/**
 * description
 *
 * @author yaochuang 2025/04/21 13:57
 */
@Data
public class NetNode<T> {
    private String host;
    private int port;
    /**
     * 节点的携带的数据信息
     */
    private T data;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NetNode<?> netNode)) {
            return false;
        }
        return getPort() == netNode.getPort() && Objects.equals(getHost(), netNode.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getPort());
    }
}

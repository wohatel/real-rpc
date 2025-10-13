package com.github.wohatel.constant;

/**
 *
 * @author yaochuang 2025/09/15 15:32
 */
public enum RpcUdpAction {
    PING, PONG;

    public static RpcUdpAction fromString(String action) {
        if (action == null) {
            return null;
        }
        for (RpcUdpAction actionEnum : RpcUdpAction.values()) {
            if (actionEnum.name().equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        return null;
    }

}

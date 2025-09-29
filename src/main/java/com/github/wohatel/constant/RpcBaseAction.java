package com.github.wohatel.constant;

/**
 *
 * @author yaochuang 2025/09/15 15:32
 */
public enum RpcBaseAction {
    INQUIRY_SESSION, INQUIRY_NODE_ID, PING, PONG;

    public static RpcBaseAction fromString(String action) {
        if (action == null) {
            return null;
        }
        for (RpcBaseAction actionEnum : RpcBaseAction.values()) {
            if (actionEnum.name().equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        return null;
    }

}

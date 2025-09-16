package com.github.wohatel.interaction.common;

/**
 * description
 *
 * @author yaochuang 2025/09/15 15:32
 */
public enum RpcBaseAction {
    BASE_INQUIRY_SESSION, BASE_INQUIRY_NODE_ID;

    public static RpcBaseAction fromString(String action) {
        for (RpcBaseAction actionEnum : RpcBaseAction.values()) {
            if (actionEnum.name().equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        return null;
    }

}

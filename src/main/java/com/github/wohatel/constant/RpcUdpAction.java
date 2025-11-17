package com.github.wohatel.constant;

/**
 * Enum representing RPC UDP actions with PING and PONG options.
 *
 * @author yaochuang 2025/09/15 15:32
 */
public enum RpcUdpAction {
    // Enum constants representing different UDP actions
    PING, PONG;

    /**
     * Converts a string representation to its corresponding RpcUdpAction enum value.
     *
     * @param action The string representation of the action to convert
     * @return The corresponding RpcUdpAction enum value, or null if no match is found or input is null
     */
    public static RpcUdpAction fromString(String action) {
        // Check if input string is null, return null if true
        if (action == null) {
            return null;
        }
        // Iterate through all enum values to find a match (case-insensitive comparison)
        for (RpcUdpAction actionEnum : RpcUdpAction.values()) {
            // Compare enum name with input string ignoring case
            if (actionEnum.name().equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        // Return null if no matching enum value is found
        return null;
    }

}

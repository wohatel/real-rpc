package com.github.wohatel.interaction.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A utility class containing various numeric constants used in RPC (Remote Procedure Call) operations.
 * This class is designed to hold static constant values that are commonly used throughout the RPC system.
 * The class is made non-instantiable by having a private no-arg constructor.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcNumberConstant {
    /**
     * Default timeout value in milliseconds for RPC operations
     */
    public static final long OVER_TIME = 3000L;
    /**
     * Ten thousand (10,000) - often used as a threshold or limit value
     */
    public static final int K_TEN = 10_000;
    /**
     * Basic numeric constant for ten (10)
     */
    public static final int TEN = 10;
    /**
     * Basic numeric constant for eight (8)
     */
    public static final int EIGHT = 8;
    /**
     * Eighteen thousand (18,000) - used for various thresholds or limits
     */
    public static final long K_TEN_EIGHT = 18_000;
    /**
     * Seventy-five (75) - often used for percentage or ratio calculations
     */
    public static final int SEVENTY_FIVE = 75;
    /**
     * One thousand five hundred (1,500) - used for various thresholds or limits
     */
    public static final int ONE_POINT_FILE_K = 1_500;
    /**
     * Data limit of 16 megabytes (16 * 1024 * 1024 bytes)
     * Used to set maximum data size limits for RPC operations
     */
    public static final int DATA_LIMIT_M_16 = 16 * 1024 * 1024;
}

package com.github.wohatel.interaction.file;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration class for RPC file transfer settings.
 * This class uses Lombok annotations for boilerplate code generation.
 *
 * @author yaochuang
 */
@Data // Generates getters, setters, toString, equals and hashCode methods
@Builder // Provides a builder pattern for object construction
public class RpcFileTransConfig {


    /**
     * Default speed limit for file transfer in bytes per second.
     * Default value is 5MB (5 * 1024 * 1024 bytes).
     */
    @Builder.Default // Specifies the default value for the builder
    private long speedLimit = 5 * 1024 * 1024L;

    /**
     * Size of each data chunk in bytes during file transfer.
     * Default value is 512KB (512 * 1024 bytes).
     */
    @Builder.Default
    private long chunkSize = 512 * 1024L;

    /**
     * Number of cache blocks to use for file transfer.
     * Default value is 5.
     */
    @Builder.Default
    private int cacheBlock = 5;

    /**
     * Flag indicating whether to try compression during file transfer.
     * Default value is true.
     */
    @Builder.Default
    private boolean tryCompress = true;

    /**
     * Compression rate percentage (0-100).
     * Default value is 75.
     */
    @Builder.Default
    private int compressRatePercent = 75;

    /**
     * Static builder method to create a new RpcFileTransConfig instance.
     *
     * @param speedLimit          Transfer speed limit in bytes per second
     * @param chunkSize           Size of each data chunk in bytes
     * @param cacheBlock          Number of cache blocks
     * @param tryCompress         Whether to attempt compression
     * @param compressRatePercent Compression rate percentage (0-100)
     * @return A new RpcFileTransConfig instance with the specified parameters
     */
    @Builder
    public static RpcFileTransConfig build(
            long speedLimit,
            long chunkSize,
            int cacheBlock,
            boolean tryCompress,
            int compressRatePercent
    ) {
        // Validate input parameters before creating the instance
        validate(speedLimit, chunkSize, cacheBlock, compressRatePercent);
        return new RpcFileTransConfig(speedLimit, chunkSize, cacheBlock, tryCompress, compressRatePercent);
    }

    /**
     * Validates the configuration parameters.
     *
     * @param speedLimit          Transfer speed limit in bytes per second
     * @param chunkSize           Size of each data chunk in bytes
     * @param cacheBlock          Number of cache blocks
     * @param compressRatePercent Compression rate percentage (0-100)
     * @throws RpcException if any parameter is invalid
     */
    private static void validate(long speedLimit, long chunkSize, int cacheBlock, int compressRatePercent) {
        // Validate speed limit
        if (speedLimit <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the speed limit cannot be <=0");
        }
        // Validate chunk size
        if (chunkSize <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the file size cannot be <=0 per send");
        }
        // Ensure speed limit is at least as large as chunk size
        if (speedLimit < chunkSize) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the rate limit cannot be less than the block size of each transmission");
        }
        // Validate cache block count
        if (cacheBlock <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "cacheBlock must > 0");
        }
        // Validate compression rate percentage
        if (compressRatePercent < 0 || compressRatePercent > 100) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "compression rate must be 0-100");
        }
    }
}
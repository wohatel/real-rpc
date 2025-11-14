package com.github.wohatel.interaction.file;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import lombok.Builder;
import lombok.Data;

/**
 * @author yaochuang
 */
@Data
@Builder
public class RpcFileTransConfig {

    @Builder.Default
    private long speedLimit = 5 * 1024 * 1024L;

    @Builder.Default
    private long chunkSize = 512 * 1024L;

    @Builder.Default
    private int cacheBlock = 5;

    @Builder.Default
    private boolean tryCompress = true;

    @Builder.Default
    private int compressRatePercent = 75;

    @Builder
    public static RpcFileTransConfig build(
            long speedLimit,
            long chunkSize,
            int cacheBlock,
            boolean tryCompress,
            int compressRatePercent
    ) {
        validate(speedLimit, chunkSize, cacheBlock, compressRatePercent);
        return new RpcFileTransConfig(speedLimit, chunkSize, cacheBlock, tryCompress, compressRatePercent);
    }

    private static void validate(long speedLimit, long chunkSize, int cacheBlock, int compressRatePercent) {
        if (speedLimit <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the speed limit cannot be <=0");
        }
        if (chunkSize <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the file size cannot be <=0 per send");
        }
        if (speedLimit < chunkSize) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the rate limit cannot be less than the block size of each transmission");
        }
        if (cacheBlock <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "cacheBlock must > 0");
        }
        if (compressRatePercent < 0 || compressRatePercent > 100) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "compression rate must be 0-100");
        }
    }
}
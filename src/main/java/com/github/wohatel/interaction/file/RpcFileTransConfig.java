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

    private RpcFileTransConfig(long speedLimit, long chunkSize, int cacheBlock, boolean tryCompress, int compressRatePercent) {
        if (speedLimit <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the speed limit cannot be <=0");
        }
        if (speedLimit < chunkSize) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the rate limit cannot be less than the block size of each transmission");
        }
        if (chunkSize <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the file size cannot be <=0 per send");
        }
        if (cacheBlock <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the difference between the number of file blocks allowed to be processed by the sender and the receiver cannot be <=0");
        }
        this.speedLimit = speedLimit;
        this.chunkSize = chunkSize;
        this.cacheBlock = cacheBlock;
        this.tryCompress = tryCompress;
        this.compressRatePercent = compressRatePercent;
    }

    /**     
     * Speed limit value, default speed limit 5M
     */
    @Builder.Default
    private long speedLimit = 5 * 1024 * 1024;

    /**     
     * The transfer size per block (512K)
     * should not be set too large
     */
    @Builder.Default
    private long chunkSize = 512 * 1024;

    /**     
     * Limit: The number of blocks cached locally and remotely,
     * when sending is paused, default 5
     */
    @Builder.Default
    private int cacheBlock = 5;

    @Builder.Default
    private boolean tryCompress = true;

    /**     
     * Compression is attempted only when the compression efficiency is at that value
     * (0-100), the smaller the compression ratio, the better the compression effect
     * Default is 75
     */
    @Builder.Default
    private int compressRatePercent = 75;
}
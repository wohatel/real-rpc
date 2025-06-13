package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.constant.NumberConstant;
import lombok.Data;

@Data
public class RpcFileTransConfig {

    public RpcFileTransConfig() {
        this(NumberConstant.M_10, NumberConstant.K_512, NumberConstant.EIGHT, true, NumberConstant.SEVENTY_FIVE);
    }

    public RpcFileTransConfig(long speedLimit, boolean tryCompress) {
        this(speedLimit, NumberConstant.K_512, NumberConstant.EIGHT, tryCompress, NumberConstant.SEVENTY_FIVE);
    }

    public RpcFileTransConfig(long speedLimit, long chunkSize, boolean tryCompress) {
        this(speedLimit, chunkSize, NumberConstant.EIGHT, tryCompress, NumberConstant.SEVENTY_FIVE);
    }

    public RpcFileTransConfig(long speedLimit, long chunkSize, int cacheBlock, boolean tryCompress, int compressRatePercent) {
        if (speedLimit <= 0) {
            throw new RuntimeException("限速不能<=0");
        }
        if (speedLimit < chunkSize) {
            throw new RuntimeException("限速不能小于每次发送块大小");
        }
        if (chunkSize <= 0) {
            throw new RuntimeException("文件每次发送大小不能<=0");
        }
        if (cacheBlock <= 0) {
            throw new RuntimeException("允许发送端和接受端处理文件块数差距不能<=0");
        }
        this.speedLimit = speedLimit;
        this.chunkSize = chunkSize;
        this.cacheBlock = cacheBlock;
        this.compressRatePercent = compressRatePercent;
        this.tryCompress = tryCompress;
    }

    /**
     * 限速值,限速
     */
    private final long speedLimit;

    /**
     * 每块传输大小
     */
    private final long chunkSize;

    /**
     * 限制: 本地和远端缓存的块数,时会暂停发送,默认8
     */
    private final int cacheBlock;

    /**
     * 尝试压缩
     */
    private final boolean tryCompress;

    /**
     * 当压缩率效率该值的时候才尝试压缩
     * (0-100), 压缩率越小,表示压缩效果越好
     * 默认为70
     */
    private final int compressRatePercent;


}
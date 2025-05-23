package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.constant.NumberConstant;
import lombok.Data;

@Data
public class RpcFileTransConfig {

    public RpcFileTransConfig() {
        this(NumberConstant.M_10, NumberConstant.M_1, NumberConstant.EIGHT, NumberConstant.TEN_EIGHT_K, true, NumberConstant.SEVENTY_FIVE);
    }

    public RpcFileTransConfig(long speedLimit, long chunkSize, int cacheBlock, long chunkHandleTimeOut, boolean tryCompress, int compressRatePercent) {
        if (speedLimit <= 0) {
            throw new RuntimeException("限速不能<=0");
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
        this.chunkHandleTimeOut = chunkHandleTimeOut;
        this.compressRatePercent = compressRatePercent;
        this.tryCompress = tryCompress;
    }

    /**
     * 限速值,限速不超过128M每秒
     */
    private long speedLimit;

    /**
     * 每块传输大小,默认1024Kb
     */
    private long chunkSize;

    /**
     * 限制: 本地和远端缓存的块数,时会暂停发送,默认8
     */
    private int cacheBlock;
    /**
     * 单个小块消息处理超时
     */
    private long chunkHandleTimeOut;

    /**
     * 尝试压缩
     */
    private boolean tryCompress;

    /**
     * 当压缩率效率该值的时候才尝试压缩
     * (0-100), 压缩率越小,表示压缩效果越好
     * 默认为70
     */
    private int compressRatePercent;


}
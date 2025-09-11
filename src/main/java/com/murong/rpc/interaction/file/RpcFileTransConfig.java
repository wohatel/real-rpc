package com.murong.rpc.interaction.file;

import com.murong.rpc.constant.RpcErrorEnum;
import com.murong.rpc.constant.RpcException;
import lombok.Builder;
import lombok.Data;

/**
 * @author yaochuang
 */
@Data
@Builder
public class RpcFileTransConfig {

    public RpcFileTransConfig(long speedLimit, long chunkSize, int cacheBlock, boolean tryCompress, int compressRatePercent, boolean sendFileHash) {
        if (speedLimit <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "限速不能<=0");
        }
        if (speedLimit < chunkSize) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "限速不能小于每次发送块大小");
        }
        if (chunkSize <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "文件每次发送大小不能<=0");
        }
        if (cacheBlock <= 0) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "允许发送端和接受端处理文件块数差距不能<=0");
        }
        this.speedLimit = speedLimit;
        this.chunkSize = chunkSize;
        this.cacheBlock = cacheBlock;
        this.tryCompress = tryCompress;
        this.compressRatePercent = compressRatePercent;
        this.sendFileHash = sendFileHash;
    }

    /**
     * 限速值,限速5M
     */
    @Builder.Default
    private long speedLimit = 5 * 1024 * 1024;

    /**
     * 每块传输大小(512K)
     */
    @Builder.Default
    private long chunkSize = 512 * 1024;

    /**
     * 限制: 本地和远端缓存的块数,时会暂停发送,默认5
     */
    @Builder.Default
    private int cacheBlock = 5;

    /**
     * 尝试压缩
     */
    @Builder.Default
    private boolean tryCompress = false;

    /**
     * 当压缩率效率该值的时候才尝试压缩
     * (0-100), 压缩率越小,表示压缩效果越好
     * 默认为70
     */
    @Builder.Default
    private int compressRatePercent = 70;

    /**
     * 是否计算文件Md5
     */
    @Builder.Default
    private boolean sendFileHash = false;


}
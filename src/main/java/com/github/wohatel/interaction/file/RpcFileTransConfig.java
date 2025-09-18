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

    public RpcFileTransConfig(long speedLimit, long chunkSize, int cacheBlock, boolean tryCompress, int compressRatePercent, boolean sendFileMd5) {
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
        this.sendFileMd5 = sendFileMd5;
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
    private boolean sendFileMd5 = false;


}
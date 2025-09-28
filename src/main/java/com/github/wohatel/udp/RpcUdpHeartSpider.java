package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * description
 *
 * @author yaochuang 2025/09/28 09:44
 */
@Slf4j
public class RpcUdpHeartSpider {
    @Getter
    private final Long thresholdTimeMillis;
    @Getter
    private final Long pingInterval;
    @Getter
    private final RpcUdpSpider<RpcRequest> rpcUdpSpider;
    @Getter
    private final Map<InetSocketAddress, TimingHandler> timingHandlerMap = new ConcurrentHashMap<>();

    @Data
    public static class TimingHandler {
        private Long lastPingTime;
        private Long lastPongTime;
        private Long thresholdTimeMillis;

        public TimingHandler(Long thresholdTimeMillis) {
            this.thresholdTimeMillis = thresholdTimeMillis;
        }

        public boolean isAlive() {
            if (lastPingTime == null || lastPongTime == null) {
                return false;
            }
            return System.currentTimeMillis() - lastPongTime < thresholdTimeMillis;
        }
    }

    public RpcUdpHeartSpider(MultithreadEventLoopGroup eventLoopGroup, Long pingInterval, Long thresholdTimeMillis) {
        this(eventLoopGroup, null, pingInterval, thresholdTimeMillis, null);
    }

    /**
     *
     * @param eventLoopGroup      eventGroup
     * @param channelOptions      连接参数
     * @param pingInterval        ping的间隔
     * @param thresholdTimeMillis 超时阈值判断
     * @param consumer            执行其它ping,pong之外的消息逻辑
     */
    public RpcUdpHeartSpider(MultithreadEventLoopGroup eventLoopGroup, List<ChannelOptionAndValue<Object>> channelOptions, Long pingInterval, Long thresholdTimeMillis, BiConsumer<ChannelHandlerContext, RpcUdpPacket<RpcRequest>> consumer) {
        this.thresholdTimeMillis = thresholdTimeMillis;
        this.pingInterval = pingInterval;
        this.rpcUdpSpider = RpcUdpSpider.buildSpider(new TypeReference<RpcRequest>() {
        }, eventLoopGroup, channelOptions, new SimpleChannelInboundHandler<RpcUdpPacket<RpcRequest>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<RpcRequest> packet) throws Exception {
                RpcRequest request = packet.getMsg();
                String contentType = request.getContentType();
                RpcBaseAction action = RpcBaseAction.fromString(contentType);
                if (action == RpcBaseAction.PING) {
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setContentType(RpcBaseAction.PONG.name());
                    // 对方是ping,则直接pong回去
                    RpcUdpSpider.sendGeneralMsg(channelHandlerContext.channel(), rpcRequest, packet.getSender());
                } else if (action == RpcBaseAction.PONG) {
                    // 如果对方是pong,则记录pong时间
                    TimingHandler handler = timingHandlerMap.get(packet.getSender());
                    if (handler != null) {
                        handler.lastPongTime = System.currentTimeMillis();
                    }
                } else {
                    if (consumer != null) {
                        consumer.accept(channelHandlerContext, packet);
                    }
                }
            }
        });
    }

    /**
     * bind端口
     */
    public ChannelFuture bind(int port) {
        ChannelFuture future = rpcUdpSpider.bind(port);
        future.addListener((ChannelFutureListener) connectFuture -> {
            Channel newChannel = connectFuture.channel();
            if (connectFuture.isSuccess()) {
                ScheduledFuture<?> pingFuture = newChannel.eventLoop().scheduleAtFixedRate(() -> {
                    Set<Map.Entry<InetSocketAddress, TimingHandler>> entries = timingHandlerMap.entrySet();
                    for (Map.Entry<InetSocketAddress, TimingHandler> entry : entries) {
                        RpcRequest rpcRequest = new RpcRequest();
                        rpcRequest.setContentType(RpcBaseAction.PING.name());
                        RunnerUtil.execSilent(() -> {
                            TimingHandler value = entry.getValue();
                            value.lastPingTime = System.currentTimeMillis();
                            this.rpcUdpSpider.sendMsg(rpcRequest, entry.getKey());
                        });
                    }
                }, 0, pingInterval, TimeUnit.MILLISECONDS);
                newChannel.closeFuture().addListener(f -> {
                    pingFuture.cancel(false);
                });
            } else {
                Throwable cause = connectFuture.cause();
                log.error("connection bind failed: port" + ":" + port, cause);
            }
        });
        return future;
    }

    /**
     * 关闭服务
     */
    public ChannelFuture close() {
        return rpcUdpSpider.close();
    }

    /**
     * 发送消息到对方
     *
     * @param rpcRequest 请求体
     * @param to         发送目标
     */
    public void sendMsg(RpcRequest rpcRequest, InetSocketAddress to) {
        rpcUdpSpider.sendMsg(rpcRequest, to);
    }

    /**
     * 添加远端的socket 检测
     *
     */
    public TimingHandler addRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.computeIfAbsent(socketAddress, key -> new TimingHandler(thresholdTimeMillis));
    }

    /**
     * 删除远端的socket 检测
     *
     */
    public void removeRemoteSocket(InetSocketAddress socketAddress) {
        timingHandlerMap.remove(socketAddress);
    }

    /**
     * 获取远端的socket 检测
     *
     */
    public TimingHandler getRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.get(socketAddress);
    }

    /**
     * 获取远端的socket 检测
     *
     */
    public boolean containsRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.containsKey(socketAddress);
    }

    /**
     * 清理掉未联通的socket
     */
    public void releaseUnAliveSockets() {
        Iterator<Map.Entry<InetSocketAddress, TimingHandler>> iterator = timingHandlerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InetSocketAddress, TimingHandler> next = iterator.next();
            TimingHandler value = next.getValue();
            // 如果么有向对方发送过ping,并且断连
            if (value.lastPingTime != null && !value.isAlive()) {
                iterator.remove();
            }
        }
    }

}

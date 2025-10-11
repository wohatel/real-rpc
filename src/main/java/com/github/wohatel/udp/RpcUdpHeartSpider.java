package com.github.wohatel.udp;

import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** * A simple UDP heartbeat proxy class can be used to detect heartbeats between services
 *
 * @author yaochuang 2025/09/28 09:44
 */
@Slf4j
public class RpcUdpHeartSpider extends RpcDefaultUdpSpider {

    @Getter
    private final Map<InetSocketAddress, TimingHandler> timingHandlerMap = new ConcurrentHashMap<>();
    @Getter
    private final BroadCaster broadCaster = new BroadCaster();
    @Getter
    private final UdpHeartConfig udpHeartConfig;


    public RpcUdpHeartSpider(RpcEventLoopManager rpcEventLoopManager) {
        this(rpcEventLoopManager, null, null, null);
    }

    /**     * @param eventLoopGroup eventGroup
     * @param channelOptions Connection channel options
     */
    public RpcUdpHeartSpider(RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, UdpHeartConfig config, BiConsumer<ChannelHandlerContext, RpcUdpPacket<RpcRequest>> simpleMsgConsumer) {
        super(rpcEventLoopManager, channelOptions, null);
        this.udpHeartConfig = Objects.requireNonNullElseGet(config, () -> new UdpHeartConfig(NumberConstant.OVER_TIME, NumberConstant.TEN_EIGHT_K));
        super.setRpcMsgConsumer((ctx, packet) -> {
            RpcRequest request = packet.getMsg();
            String contentType = request.getContentType();
            RpcBaseAction action = RpcBaseAction.fromString(contentType);
            if (action == RpcBaseAction.PING) {
                RpcRequest rpcRequest = new RpcRequest();
                rpcRequest.setContentType(RpcBaseAction.PONG.name());
                // 对方是ping,则直接pong回去
                RpcUdpSpider.sendGeneralMsg(ctx.channel(), rpcRequest, packet.getSender());
            } else if (action == RpcBaseAction.PONG) {
                TimingHandler handler = timingHandlerMap.get(packet.getSender());
                if (handler != null) {
                    handler.lastPongTime = System.currentTimeMillis();
                }
            } else {
                if (simpleMsgConsumer != null) {
                    simpleMsgConsumer.accept(ctx, packet);
                }
            }
        });
    }

    /**     * set broadcast address
     *
     * @param broadcastAddress broadcast address
     */
    public void setBroadcastAddress(InetSocketAddress broadcastAddress) {
        this.broadCaster.setBroadcastAddress(broadcastAddress);
    }

    /**     * Turn off the broadcast
     */
    public void stopBroadcast() {
        this.broadCaster.setEnable(false);
    }

    /**     * Turn on the broadcast
     */
    public void enableBroadcast() {
        this.broadCaster.setEnable(true);
    }

    /**     * Bind the port that the UDP service starts
     */
    @Override
    public ChannelFuture bind(int port) {
        ChannelFuture future = super.bind(port);
        // 开始监听绑定,并添加任务
        future.addListener((ChannelFutureListener) connectFuture -> {
            Channel newChannel = connectFuture.channel();
            if (connectFuture.isSuccess()) {
                // 如果链接成功,每隔pingInterval 毫秒就触发一次ping
                ScheduledFuture<?> pingFuture = newChannel.eventLoop().scheduleAtFixedRate(() -> {
                    // 在启用广播的情况下,采用广播协议
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setContentType(RpcBaseAction.PING.name());
                    if (this.broadCaster.isReady()) {
                        // 广播发送ping
                        RunnerUtil.execSilent(() -> this.rpcUdpSpider.sendMsg(rpcRequest, broadCaster.broadcastAddress));
                    } else {
                        Set<Map.Entry<InetSocketAddress, TimingHandler>> entries = timingHandlerMap.entrySet();
                        for (Map.Entry<InetSocketAddress, TimingHandler> entry : entries) {
                            RunnerUtil.execSilent(() -> {
                                TimingHandler value = entry.getValue();
                                value.lastPingTime = System.currentTimeMillis();
                                this.rpcUdpSpider.sendMsg(rpcRequest, entry.getKey());
                            });
                        }
                    }
                }, 0, this.udpHeartConfig.pingInterval, TimeUnit.MILLISECONDS);

                // 如果检测到channel关闭了,就注销掉pingFuture的任务
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

    /**     * Add remote socket detection
     */
    public TimingHandler addRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.computeIfAbsent(socketAddress, key -> new TimingHandler(this.udpHeartConfig.thresholdTimeMillis));
    }

    /**     * Delete remote socket detection
     */
    public void removeRemoteSocket(InetSocketAddress socketAddress) {
        timingHandlerMap.remove(socketAddress);
    }

    /**     * Get remote socket detection
     */
    public TimingHandler getRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.get(socketAddress);
    }

    /**     * is remote socket detection exists
     */
    public boolean containsRemoteSocket(InetSocketAddress socketAddress) {
        return timingHandlerMap.containsKey(socketAddress);
    }

    /**     * Clean up unconnected sockets
     */
    public void releaseUnAliveSockets() {
        timingHandlerMap.entrySet().removeIf(entry -> {
            TimingHandler value = entry.getValue();
            return value.lastPingTime != null && !value.isAlive();
        });
    }


    @Data
    @AllArgsConstructor
    public static class UdpHeartConfig {
        @Getter
        private final Long pingInterval;

        @Getter
        private final Long thresholdTimeMillis;
    }


    @Data
    public static class TimingHandler {
        private Long lastPingTime;
        private Long lastPongTime;
        private Long thresholdTimeMillis;

        public TimingHandler(Long thresholdTimeMillis) {
            this.thresholdTimeMillis = thresholdTimeMillis;
        }

        public boolean isAlive() {
            if (lastPongTime == null) {
                return false;
            }
            return System.currentTimeMillis() - lastPongTime < thresholdTimeMillis;
        }
    }

    @Data
    public static class BroadCaster {
        private InetSocketAddress broadcastAddress;
        private boolean enable;

        public boolean isReady() {
            return broadcastAddress != null && enable;
        }
    }

}

package com.github.wohatel.udp;

import com.github.wohatel.constant.RpcUdpAction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcUdpEventLoopManager;
import com.github.wohatel.interaction.common.RpcUdpPacket;
import com.github.wohatel.interaction.common.RpcUdpWaiter;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

/**
 * A simple UDP heartbeat proxy class can be used to detect heartbeats between services
 *
 * @author yaochuang 2025/09/28 09:44
 */
@Slf4j
public class RpcUdpHeartSpider extends RpcDefaultUdpSpider {

    // Map to store remote socket addresses and their corresponding timing handlers
    @Getter
    private final Map<InetSocketAddress, TimingHandler> timingHandlerMap = new ConcurrentHashMap<>();
    // Configuration for UDP heartbeat settings
    @Getter
    private final UdpHeartConfig udpHeartConfig;

    /**
     * Constructor with default configuration
     */
    public RpcUdpHeartSpider() {
        this(RpcUdpEventLoopManager.of());
    }

    /**
     * Constructor with default configuration
     *
     * @param eventLoopManager The event loop manager for RPC operations
     */
    public RpcUdpHeartSpider(RpcUdpEventLoopManager eventLoopManager) {
        this(eventLoopManager, null, null);
    }

    /**
     * Constructor with custom channel options and heartbeat configuration
     * @param eventLoopManager The event loop manager for RPC operations
     * @param channelOptions List of channel options and values to configure
     * @param config Heartbeat configuration settings
     */
    public RpcUdpHeartSpider(RpcUdpEventLoopManager eventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, UdpHeartConfig config) {
        super(eventLoopManager, channelOptions, null);
        this.udpHeartConfig = Objects.requireNonNullElseGet(config, () -> new UdpHeartConfig(RpcNumberConstant.OVER_TIME, RpcNumberConstant.K_TEN_EIGHT));
        this.onMsgReceive(null);
    }

    /**
     * Override method to handle incoming messages, if msgConsumer is null
     * the default heart ping pong will worked
     * if msgConsumer is not null, u can use the msgConsumer to process the ping pong logic
     * @param msgConsumer Consumer for processing received messages
     */
    @Override
    public void onMsgReceive(BiConsumer<RpcUdpWaiter<RpcRequest>, RpcUdpPacket<RpcRequest>> msgConsumer) {
        super.onMsgReceive((waiter, packet) -> {
            if (msgConsumer == null) {
                RpcRequest request = packet.getMsg();
                String contentType = request.getContentType();
                RpcUdpAction action = RpcUdpAction.fromString(contentType);
                // Handle PING action by sending PONG response
                if (action == RpcUdpAction.PING) {
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setContentType(RpcUdpAction.PONG.name());
                    // 对方是ping,则直接pong回去
                    waiter.sendMsg(rpcRequest, packet.getSender());
                } else if (action == RpcUdpAction.PONG) {
                    TimingHandler handler = timingHandlerMap.get(packet.getSender());
                    if (handler != null) {
                        handler.lastPongTime = System.currentTimeMillis();
                    }
                }
            } else {
                msgConsumer.accept(waiter, packet);
            }
        });
    }

    /**
     * Bind the server to the specified port and set up heartbeat ping mechanism
     * @param port The port number to bind the server to
     * @return ChannelFuture representing the asynchronous bind operation
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
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setContentType(RpcUdpAction.PING.name());
                    Set<Map.Entry<InetSocketAddress, TimingHandler>> entries = timingHandlerMap.entrySet();
                    for (Map.Entry<InetSocketAddress, TimingHandler> entry : entries) {
                        RunnerUtil.execSilent(() -> {
                            TimingHandler value = entry.getValue();
                            value.lastPingTime = System.currentTimeMillis();
                            this.rpcUdpSpider.sendMsg(rpcRequest, entry.getKey());
                        });
                    }
                }, 0, this.udpHeartConfig.pingInterval, TimeUnit.MILLISECONDS);
                // 如果检测到channel关闭了,就注销掉pingFuture的任务
                newChannel.closeFuture().addListener(f -> pingFuture.cancel(false));
            } else {
                Throwable cause = connectFuture.cause();
                log.error("connection bind failed: port:{}", port, cause);
            }
        });
        return future;
    }


    /**
     * Adds a remote socket to the timing handler map if it doesn't already exist.
     * If the socket address is not present in the map, a new TimingHandler is created
     * with the specified threshold time in milliseconds.
     *
     * @param socketAddress The InetSocketAddress representing the remote socket to be added
     * @return The existing TimingHandler if present, or a new one created with the threshold time
     */
    public TimingHandler addRemoteSocket(InetSocketAddress socketAddress) {
        // Compute the value if the key is not present, using the provided threshold time
        return timingHandlerMap.computeIfAbsent(socketAddress, key -> new TimingHandler(this.udpHeartConfig.thresholdTimeMillis));
    }


    /**
     * Removes a remote socket and its associated timing handler from the mapping.
     *
     * @param socketAddress The InetSocketAddress representing the remote socket to be removed
     */
    public void removeRemoteSocket(InetSocketAddress socketAddress) {
        // Remove the timing handler associated with the given socket address from the map
        timingHandlerMap.remove(socketAddress);
    }


    /**
     * Retrieves a TimingHandler instance associated with the specified socket address.
     * This method looks up the timing handler in the internal map using the provided InetSocketAddress.
     *
     * @param socketAddress The InetSocketAddress key used to retrieve the TimingHandler
     * @return The TimingHandler instance associated with the given socket address,
     * or null if no mapping exists for this address
     */
    public TimingHandler getRemoteSocket(InetSocketAddress socketAddress) {
        // Retrieve and return the TimingHandler from the map using the provided socket address
        return timingHandlerMap.get(socketAddress);
    }


    /**
     * Checks if the given socket address is present in the timing handler map.
     *
     * @param socketAddress The InetSocketAddress to be checked for existence in the map
     * @return true if the map contains the specified socket address, false otherwise
     */
    public boolean containsRemoteSocket(InetSocketAddress socketAddress) {
        // Check if the timingHandlerMap contains the specified socket address as a key
        return timingHandlerMap.containsKey(socketAddress);
    }


    /**
     * Removes all inactive sockets from the timing handler map.
     * This method iterates through the entries in the timing handler map and removes those
     * where the associated TimingHandler is not alive based on its last ping time.
     */
    public void releaseUnAliveSockets() {
        // Remove entries from the timing handler map where the value (TimingHandler) is not alive
        timingHandlerMap.entrySet().removeIf(entry -> {
            // Get the TimingHandler value from the map entry
            TimingHandler value = entry.getValue();
            // Check if the lastPingTime is not null and the handler is not alive
            return value.lastPingTime != null && !value.isAlive();
        });
    }


    /**
     * Configuration class for UDP heartbeat mechanism
     * This static class contains parameters for UDP heartbeat configuration
     * It uses Lombok annotations for automatic generation of getters, setters, and constructors
     */
    @Data
    @AllArgsConstructor
    public static class UdpHeartConfig {
        /**
         * The interval at which ping messages are sent
         * This value determines how frequently heartbeat messages are transmitted
         * It is a final field, meaning its value cannot be changed after initialization
         */
        @Getter
        private final Long pingInterval;

        /**
         * The threshold time in milliseconds for response timeout
         * If a response is not received within this time frame after sending a ping,
         * the connection is considered to be lost
         * It is a final field, meaning its value cannot be changed after initialization
         */
        @Getter
        private final Long thresholdTimeMillis;
    }


    /**
     * A static inner class that handles timing-related operations for connection health monitoring.
     * It tracks ping and pong timestamps to determine if the connection is still alive.
     */
    @Data
    public static class TimingHandler {
        private Long lastPingTime;      // Timestamp of the last ping message received
        private Long lastPongTime;      // Timestamp of the last pong message received
        private Long thresholdTimeMillis; // Time threshold in milliseconds to consider connection alive

        /**
         * Constructor to initialize the TimingHandler with a specific threshold time.
         *
         * @param thresholdTimeMillis The time threshold in milliseconds to determine if connection is alive
         */
        public TimingHandler(Long thresholdTimeMillis) {
            this.thresholdTimeMillis = thresholdTimeMillis;
        }

        /**
         * Checks if the connection is still alive based on the last pong time.
         * A connection is considered alive if the time elapsed since the last pong is less than the threshold.
         *
         * @return true if connection is alive, false otherwise
         */
        public boolean isAlive() {
            if (lastPongTime == null) {
                return false;
            }
            return System.currentTimeMillis() - lastPongTime < thresholdTimeMillis;
        }
    }
}

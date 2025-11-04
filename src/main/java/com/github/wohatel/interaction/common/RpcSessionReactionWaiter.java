package com.github.wohatel.interaction.common;


import com.github.wohatel.interaction.base.RpcReaction;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcSessionReactionWaiter extends RpcReactionWaiter {

    @Getter
    private final String sessionId;

    public RpcSessionReactionWaiter(ChannelHandlerContext ctx, String sessionId) {
        super(ctx);
        this.sessionId = sessionId;
    }

    @Override
    public void sendReaction(RpcReaction reaction) {
        if (reaction == null) {
            return;
        }
        if (!this.sessionId.equals(reaction.getReactionId())) {
            return;
        }
        if (RpcSessionTransManger.isRunning(sessionId)) {
            super.sendReaction(reaction);
        }
    }

    /**
     * 中止session
     */
    public void forceInterruptSession() {
        RpcSessionTransManger.release(sessionId);
    }
}

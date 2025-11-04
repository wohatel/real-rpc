package com.github.wohatel.interaction.common;


import com.github.wohatel.interaction.base.RpcReaction;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcSessionReactionWaiter extends RpcReactionWaiter {


    public RpcSessionReactionWaiter(ChannelHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public void sendReaction(RpcReaction reaction) {
        if (RpcSessionTransManger.isRunning(reaction.getReactionId())) {
            super.sendReaction(reaction);
        }
    }

}

package com.github.wohatel.interaction.common;

import io.netty.channel.ChannelOption;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * description
 *
 * @author yaochuang 2025/09/17 17:50
 */
@Data
@AllArgsConstructor
public class ChannelOptionAndValue<T> {
    ChannelOption<T> channelOption;
    T value;
}

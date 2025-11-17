package com.github.wohatel.interaction.common;

import io.netty.channel.ChannelOption;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A class representing a channel option and its corresponding value.
 * This class is used to configure channel options with their specific values.
 *
 * @author yaochuang 2025/09/17 17:50
 */
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
@AllArgsConstructor // Lombok annotation to generate a constructor with all fields
public class ChannelOptionAndValue<T> {
    /**
     * The channel option to be configured.
     * This is typically a predefined option from the Netty framework.
     */
    ChannelOption<T> channelOption;
    /**
     * The value to be set for the channel option.
     * The type of this value matches the type parameter T of the ChannelOption.
     */
    T value;
}

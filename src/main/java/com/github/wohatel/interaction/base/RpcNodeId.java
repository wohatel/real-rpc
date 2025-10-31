package com.github.wohatel.interaction.base;


import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


/**
 * @author yaochuang
 */
@Data
public class RpcNodeId {
    @Getter
    @Setter
    private static String NODEID = NanoIdUtils.randomNanoId() + ":" + System.currentTimeMillis();
}

package com.github.wohatel.interaction.common;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the context of an RPC session, containing session-specific data.
 * This class is a POJO (Plain Old Java Object) used to store and transfer session-related information.
 *
 * @author yaochuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcSessionContext {

    /**
     * The topic or subject of the RPC session.
     * This field typically indicates the purpose or category of the session.
     */
    private String topic;

    /**
     * A list of matters or topics related to this RPC session.
     * This can be used to specify multiple subjects or items of interest within the session.
     */
    private List<String> matters;

    /**
     * A JSONObject containing additional parameters for the RPC session.
     * This allows for flexible, key-value pair parameters to be included in the session context.
     */
    private JSONObject parameters;
}

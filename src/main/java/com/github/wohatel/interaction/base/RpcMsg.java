package com.github.wohatel.interaction.base;

import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.util.Objects;

/**
 * @author yaochuang
 */
@Data  // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
public class RpcMsg {
    // Enum representing the type of RPC command
    private RpcCommandType rpcCommandType;
    // The actual payload/data being sent in the RPC message
    private Object payload;
    // ByteBuf for efficient byte buffer operations
    private ByteBuf byteBuffer;
    // Flag to indicate if compression is needed for this message
    private boolean needCompress;

    // Default constructor
    public RpcMsg() {
    }

    /**
     * Constructor to create a new RpcMsg with specified type and payload
     *
     * @param type    The type of RPC command
     * @param payload The data to be sent
     */
    public RpcMsg(RpcCommandType type, Object payload) {
        this.rpcCommandType = type;
        this.payload = payload;
    }

    /**
     * Factory method to create a RpcMsg from a RpcRequest
     *
     * @param request The RpcRequest to convert
     * @return A new RpcMsg instance
     * @throws NullPointerException if request is null
     */
    public static RpcMsg fromRequest(RpcRequest request) {
        Objects.requireNonNull(request);
        if (request instanceof RpcFileRequest rpcFileRequest) {
            return fromFileRequest(rpcFileRequest);
        }
        if (request instanceof RpcSessionRequest rpcSessionRequest) {
            return fromSessionRequest(rpcSessionRequest);
        }
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.request, request);
        rpcMsg.setNeedCompress(request.isEnableCompress());
        return rpcMsg;
    }

    /**
     * Factory method to create a RpcMsg from a RpcSessionRequest
     *
     * @param rpcSessionRequest The session request to convert
     * @return A new RpcMsg instance
     * @throws NullPointerException if rpcSessionRequest is null
     */
    public static RpcMsg fromSessionRequest(RpcSessionRequest rpcSessionRequest) {
        Objects.requireNonNull(rpcSessionRequest);
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.session, rpcSessionRequest);
        rpcMsg.setNeedCompress(rpcSessionRequest.isEnableCompress());
        return rpcMsg;
    }

    /**
     * Creates an RpcMsg from an RpcReaction object.
     * This is a static factory method that converts a reaction into a message.
     *
     * @param reaction The RpcReaction object to be converted into an RpcMsg
     * @return A new RpcMsg instance with the command type set to 'reaction' and containing the provided reaction
     * @throws NullPointerException if the reaction parameter is null
     */
    public static RpcMsg fromReaction(RpcReaction reaction) {
        // Ensure the reaction parameter is not null
        Objects.requireNonNull(reaction);

        // Create and return a new RpcMsg with reaction command type and the reaction payload
        return new RpcMsg(RpcCommandType.reaction, reaction);
    }

    /**
     * Creates an RpcMsg from a file request.
     *
     * @param fileRequest The file request to be converted into an RpcMsg
     * @return A new RpcMsg instance containing the file request
     * @throws NullPointerException if fileRequest is null
     */
    public static RpcMsg fromFileRequest(RpcFileRequest fileRequest) {
        Objects.requireNonNull(fileRequest);  // Ensure fileRequest is not null
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.file, fileRequest);  // Create new RpcMsg with file command type
        rpcMsg.setNeedCompress(fileRequest.isEnableCompress());  // Set compression flag based on file request setting


        return rpcMsg;  // Suppress unchecked cast warning
    }

    /**
     * Retrieves the payload as the specified type.
     *
     * @param <T>   the type to which the payload should be cast
     * @param clazz the Class object representing the desired type
     * @return the payload cast to the specified type, or null if the payload is null
     * @throws IllegalStateException if the payload is not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        // Check if payload is null and return null if true
        if (payload == null) {
            return null;
        }
        // Verify that the payload is an instance of the specified class
        if (!clazz.isInstance(payload)) {
            throw new IllegalStateException("Payload is not of type " + clazz.getName());
        }
        // Return the payload cast to the specified type
        return (T) payload;
    }

}

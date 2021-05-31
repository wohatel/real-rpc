package com.murong.rpc.interaction;

public class RpcCommand {
    protected RpcCommandType commandType; // response or request

    public RpcCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(RpcCommandType commandType) {
        this.commandType = commandType;
    }
}

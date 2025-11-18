package com.github.wohatel.interaction.base;


import com.github.wohatel.interaction.constant.RpcSessionType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author yaochuang
 */
@EqualsAndHashCode(callSuper = true)  // Generates equals and hashCode methods that include superclass fields
@Accessors(chain = true)              // Enables method chaining for setters
public class RpcSessionFuture extends RpcFuture {  // Extends RpcFuture for session-specific functionality

    @Getter  // Automatically generates getter method for this field
    @Setter  // Automatically generates setter method for this field
    private volatile RpcSessionProcess rpcSessionProcess;  // Session process handler, volatile for thread safety

    @Getter  // Automatically generates getter method for this field
    @Setter
    private RpcSessionType rpcSessionType;

    /**
     * Constructor for RpcSessionFuture
     *
     * @param timeOut Timeout value for the future operation
     */
    public RpcSessionFuture(long timeOut) {
        super(timeOut);  // Calls parent class constructor with timeout parameter
    }

}

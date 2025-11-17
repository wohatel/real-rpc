package com.github.wohatel.interaction.file;

import lombok.Data;

/**
 * This class represents information about a file in RPC (Remote Procedure Call) context.
 * It contains metadata about the file such as its name and size.
 * The @Data annotation from Lombok library is used to automatically generate getters, setters,
 * toString(), equals() and hashCode() methods for this class.
 */
@Data
public class RpcFileInfo {

    // The name of the file
    private String fileName;

    // The size/length of the file in bytes
    private long length;

}

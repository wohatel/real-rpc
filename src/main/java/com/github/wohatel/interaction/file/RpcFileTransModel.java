package com.github.wohatel.interaction.file;


/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
public enum RpcFileTransModel {

    /**     
     * If it exists, it is notified of success
     */
    SKIP,

    /**     
     * Application scenario: Delete old files,
     * copy sender files, and generate new files
     */
    REBUILD,

    /**     
     * Application scenario: The last time the file was not transferred, merge multiple files into one, such as a text file
     * If the old file does not exist, create the file first, and then append the entire sender's file contents after the local file
     * If the old file exists, then continue appending, and then append the entire sender's file contents to the local file
     */
    APPEND,

    /**     
     * Application scenario: The file was not finished last time, and this time it will be transmitted
     * If the old file does not exist, create the file and append the entire sender's file contents after the local file
     * If the old file exists, calculate the length of the local file so that the sender only sends the contents after the length
     */
    RESUME;


    public static RpcFileTransModel nameOf(String name) {
        try {
            return RpcFileTransModel.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }
}

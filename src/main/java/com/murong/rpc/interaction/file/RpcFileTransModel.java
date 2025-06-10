package com.murong.rpc.interaction.file;


/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
public enum RpcFileTransModel {

    /**
     * 如果存在,就通知成功
     */
    SKIP,

    /**
     * 应用场景: 删除旧的文件,将发送方文件拷贝过来,生成新文件
     */
    REBUILD,

    /**
     * 应用场景: 上次文件没传完,将多个文件合并成一个,比如文本文件
     * 如果旧文件不存在,则先创建文件,然后将整个发送方的文件内容追加到本地文件后面
     * 如果旧文件存在,然后继续追加,然后将整个发送方的文件内容追加到本地文件后面
     */
    APPEND,

    /**
     * 应用场景: 上次文件没传完,这次接着传
     * 如果旧文件不存在,则创建文件,然后将整个发送方的文件内容追加到本地文件后面
     * 如果旧文件存在,则计算下本地文件的长度,让发送发只发送长度后边的内容
     */
    RESUME;


    public static RpcFileTransModel nameOf(String name) {
        try{
            return RpcFileTransModel.valueOf(name);
        }catch (Exception e){
            return null;
        }
    }
}

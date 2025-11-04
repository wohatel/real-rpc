# 项目介绍
本项目主要解决服务器之间的数据传输,以netty为技术底层:
1. 普通消息传输, 可以实现rpc请求,响应
2. 会话消息传输, 可以实现比如,类似远程执行shell命令的功能,或者聊天群的功能
3. 文件传输, 服务间文件传输; 可以控制传输速率,传输方式(支持断电续传,重建....)
   4: 支持udp协议, 可以做类似心跳功能,判断通信是否中断

测试用例请点进项目test目录下: src/test/java/com/github/wohatel 查看

# 关键类使用

- RpcDefaultClient:
    - rpcEventLoopManager: 连接池,可以选择nio,epoll等模式
    - channelOptions: 连接参数
    - localAddress: 本地网卡连接一般为空
    - connect(): 连接到服务端,构造里面已经设置了服务端的端口和地址

    - onFileReceive(): 当接收到文件请求时,如何处理
    - onRequestReceive() : 当接收到普通消息请求如何处理
    - ~~onSessionRequestReceive()~~ : client端暂不支持接收会话消息


- RpcAutoReconnectClient
    - autoReconnect(): 支持断线自动重连的client,其它与RpcDefaultClient无差别


- RpcServer
    - start(): 启动本地服务
    - onFileReceive(): 当接收到文件请求时,如何处理
    - onRequestReceive(): 当接收到普通消息请求如何处理
    - onSessionRequestReceive(): 服务端接受到client发来的sessionRequest处理逻辑

    
- RpcReaction
    - 此类即源端请求后,处理完逻辑返回的结果,用的时候请注意RpcRequest中的needReaction参数,如果需要返回,业务处理返回
    - 返回方式: 规范的编码如下只有sendSynRequest的时候可以读取结果

```
    远端发送请求等待结果:
    RpcFuture future = client.sendSynRequest(reqeust);
    future.get();
    
    接收端判断:
    if (req.isNeedReaction()) {
        RpcReaction reaction = req.toReaction();
        reaction.setBody("thanks, got it");
        // 响应结果
        RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
    }
```

- RpcFileReceiverHandler
```java
        /**
     * ::: 在文件接收之前,设定本地接收文件位置,以及本地采用断点续传? 重建? 已存在跳过? 等处理,同时将本地处理方式告知发送端
     * rpcSession: 文件接收期间保持的会话标识,内置超时属性,当会话在一定时间内没有数据交互,就会报超时
     * context : 源端发来的context上下文信息,主要用来描述发来文件的意图和附加说明信息
     * fileInfo: 源端文件信息,描述文件的大小,名称,hash
     * @return: 经过一些列判断, 告知源端以什么方式发送数据, 比如.断点续传, 重建, 跳过, 如果返回null,则标识拒绝此次文件接收 
     */
    RpcFileLocal getTargetFile(final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo);
    
    /**
     * ::: 文件接收的进度
     * rpcFileWrapper: 文件参数的封装,包含rpcSession,context,fileInfo,总共需要传输的大小
     *                 interrupter.forceInterruptSession(): 表示接收端单方面停止接收文件
     *                 不会触发onFailure -- 不认为是失败了,只是传输被中止
     *
     * receiveSize: 表示已经接收文件的大小 
     */
    default void onProcess(final RpcFileReceiveWrapper rpcFileWrapper, long receiveSize,RpcFileInterrupter interrupter);
    
    /**
     * ::: 文件接收时,本地出现错误后调用逻辑
     */
    default void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e);
    
    /**
     * ::: 成功接收文件后处理逻辑
     */
    default void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper);

    /**
     * 如果 getTargetFile 方法返回结果不为null,那么onFinally 最后会被执行
     */
    default void onFinally(final RpcFileReceiveWrapper rpcFileWrapper);

```

- RpcSessionRequestMsgHandler

```java
        /**
     * ::: client端发来的开启一个会话的请求
     * contextWrapper 回话的上下文
     *   
     *   RpcSessionReactionWaiter waiter: 会话服务员
     *   waiter.forceInterruptSession(): 单方面强制终止回话
     *   @return 返回false,表示不同意开启会话
     */
    default boolean onSessionStart(final RpcSessionContextWrapper contextWrapper, RpcSessionReactionWaiter waiter);
    
    /**
     * ::: 会话期间,接收到客户端的请求,如何处理
     * *               contextWrapper.forceInterruptSession(): 中断此次会话
     */
    void channelRead(ChannelHandlerContext ctx, final RpcSessionContextWrapper contextWrapper, final RpcSessionRequest request);
    
    /**
     * ::: 客户端发送请求,结束会话,服务端的逻辑处理
     */
    default void sessionStop(ChannelHandlerContext ctx, final RpcSessionContextWrapper contextWrapper);

    /**
     * 如果 onSessionStart 会议开启(也就是返回值为true),那么onFinally 最后会被执行
     * ::: 客户端发送请求,结束会话,服务端的逻辑处理
     */
    default onFinally(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter);
```

- RpcSimpleRequestMsgHandler

```java
    /**
 * ::: 接收到源端发来当前请求处理逻辑
 * /
 void channelRead(ChannelHandlerContext ctx, final RpcRequest request);
```

- RpcUdpHeartSpider

```
 udp 模式下的一个心跳服务,每隔节点启动一个即可
 主要完成向不同的主机: 发送消息(继承父类),心跳逻辑(检测节点是否断连)
```



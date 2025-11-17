# 项目介绍
本项目主要解决服务器之间的数据传输,以netty为技术底层:
1. 普通消息传输, 可以实现rpc请求,响应
2. 会话消息传输, 可以实现比如,类似远程执行shell命令的功能,或者聊天群的功能
3. 文件传输, 服务间文件传输; 可以控制传输速率,传输方式(支持断电续传,重建....)
4. 支持udp协议, 可以做类似心跳功能,判断通信是否中断
5. 基本要求: java21或以上
6. 测试用例请点进项目test目录下: src/test/java/com/github/wohatel 查看


```angular2html
3.*.* 之前版本由于旧版内存模型,融合较多的预加载内存,对于内访问消耗较大,不再建议使用
从4.0.0版本后,代码进行重构和优化,更多采用懒加载机制,和数据结构封装优化以及极端情况下的防御处理,对于文件和session的处理添加了更多的入口,可以更加方便上手
建议使用4.0.0或以上版本
```


# 关键类使用

- RpcDefaultClient:
    - rpcEventLoopManager: 连接池,可以选择nio,epoll等模式,一般使用默认即可(nio)
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

- RpcFileReceiverHandler
```java

// 对方请求发送文件,本地服务是否同意接收,接收的位置是否是续传等操作
RpcFileSignature getTargetFile();

// 文件接收进度
void onProcess();

// 失败了如何处理(接收端主动中断接收文件不会触发失败)
void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e);

// 成功如何处理(接收端主动中断接收文件不会触发onSuccess)
void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper);

// 一旦getTargetFile返回值不为null,就会最终触发(onFinally内部使用异步执行,所以并不保证onSuccess或者onFailure的逻辑执行完毕后才调用onFinally)
void onFinally(final RpcFileReceiveWrapper rpcFileWrapper);

```

- RpcSessionRequestMsgHandler

```java
// 对方请求开启会话,本地服务是否同意开启
RpcSessionSignature onSessionStart();

void onReceiveRequest();

void sessionStop();
// 一旦onSessionStart返回值不为null,就会最终触发(onFinally内部使用异步执行)
void onFinally();
```

- RpcSimpleRequestMsgHandler

```java
// 接收到普通消息请求的逻辑处理,如果对方要求有返回值,注意予以返回 waiter.sendReaction(reaction);
void onReceiveRequest();
```
- RpcUdpHeartSpider
```
 udp 模式下的一个心跳服务,每隔节点启动一个即可
 主要完成向不同的主机: 发送消息(继承父类),心跳逻辑(检测节点是否断连)
```

    

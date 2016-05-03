package com.yhl;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 16-4-28
 * Time: 下午5:01
 * To change this template use File | Settings | File Templates.
 */
public class ZKInfo {

    /**
     -------
     client
        ZooKeeperMain.connectToZK()
            new ZooKeeper()  创建客户端对象
                .getClientCnxnSocket 创建通信对象ClientCnxnSocketNIO
                new ClientCnxn() 创建连接对象
                    new SendThread（） // 发送线程
                    new EventThread（）
                Zookeeper.cnxn.start(){
                    sendThread.start();
                    eventThread.start();
                }

     SendThread:
        run(){
            if(没建立连接){
                .startConnect
                clientCnxnSocket.connect
                createSock()
                registerAndConnect(sock, addr);
                primeConnection()
            }

             if(建立连接){
                 if(如果心跳间隔过长 ){
                    sendPing
                    queuePacket
                    outgoingQueue.add(packet);
                    sendThread.getClientCnxnSocket().wakeupCnxn();
                 }
             }
             clientCnxnSocket.doTransport(to,
     pendingQueue, outgoingQueue, ClientCnxn.this);

             doIo()
        }


     ClientCnxnSocket  ClientCnxnSocketNIO一个使用socket作为底层通信的实现，可以使用netty替换

     ClientCnxn 为客户端管理网路io,其中包含一个可连接的servers的列表，需要的时候进行连接。此类代表了一个socket i/o connction。
     1 建立一个connection对象，实际的连接只有需要的时候建立。顺序的调用start()

     zookeeper
     1 建立一个zookeeper客户端对象，包含一个ClientCnxn .需要一个逗号分隔的ip:端口列表，每一个对应一个zk server.
     2 session与每个server是异步建立的。
     3 watcher可以在构造前 构造后触发
     4 zkclient实例会随机选取一个server去连接，如果建立失败了，会选取另一个，直到成功，这个选取是完全无序的，客户端会继续尝试，直到session显示的关闭了。






     *
     *
     *
     *
     */
}

package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ReceivePacket extends Thread{

    private Connection connection;
    private boolean deBugging;
    private String fileDirectory;
    private SocketAddress router;
    private long sequenceNum;
    private SlidingWindow slidingWindow;

    public ReceivePacket(Connection connection, boolean deBugging, String fileDirectory, SocketAddress router, long sequenceNum){
        this.router = router;
        this.connection = connection;
        this.deBugging = deBugging;
        this.fileDirectory = fileDirectory;
        this.sequenceNum = sequenceNum;
    }

    @Override
    public void run() {
        while(!connection.isFinished()){
            try{
                connection.getChannel().configureBlocking(false);
                Selector selector = Selector.open();
                connection.getChannel().register(selector, OP_READ);
                selector.select(2000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if(keys.isEmpty()){
                    System.out.println("No response after timeout");
                    //String msg = connection.handleRequest();
                    //connection.sendRequest(msg);
                    continue;
                }

                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = connection.getChannel().receive(buf);
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();
                packetHandle(packet);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * handle packet
     * @param packet
     */
    private void packetHandle(Packet packet){
        if(packet.getType()==3&&packet.getSequenceNumber()==sequenceNum+1){
            System.out.println("Server has received the handshake ack packet.");
            connection.setConnected(true);
            return;
        }
        if(connection.isConnected()){
            if(packet.getType()==0){
                System.out.println("Server has received the data packet.");
                Packet resPacket = new Packet.Builder()
                        .setType(3)
                        .setSequenceNumber(packet.getSequenceNumber())
                        .setPortNumber(packet.getPeerPort())
                        .setPeerAddress(packet.getPeerAddress())
                        .setPayload("".getBytes())
                        .create();

                System.out.println("Server has sent the data ack back to the client");
                try {
                    connection.getChannel().send(resPacket.toBuffer(),connection.getRouter());
                    ParseRequest parseRequest = new ParseRequest(deBugging,fileDirectory,connection,router);

                    //这里再测试一下
                    synchronized (ParseRequest.class){
                        String payload = new String(packet.getPayload(), UTF_8).trim();
                        Request request = parseRequest.parseRequest(payload);
                        Response response = parseRequest.createResponse(request);
                        String handledResponse = parseRequest.handleResponse(response);
                        connection.makePackets(handledResponse,packet.getSequenceNumber()+1,new InetSocketAddress(packet.getPeerAddress(),packet.getPeerPort()));
                        slidingWindow = new SlidingWindow(connection);
                    }
                    //试试这样在方法前面加上synchronized行不行
                }catch(Exception e){
                    e.printStackTrace();
                }

            }else if(packet.getType()==4||packet.getType()==5||packet.getType()==6){
                System.out.println("Server has received the data packet. The sequence number is "+packet.getSequenceNumber());
                connection.addReceivePackets(packet);
                Packet resPacket = new Packet.Builder()
                        .setType(3)
                        .setSequenceNumber(packet.getSequenceNumber())
                        .setPortNumber(packet.getPeerPort())
                        .setPeerAddress(packet.getPeerAddress())
                        .setPayload("".getBytes())
                        .create();
                System.out.println("Server has sent the ack back to the client. The sequence number is "+resPacket.getSequenceNumber());
                try {
                    connection.getChannel().send(resPacket.toBuffer(),connection.getRouter());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(packet.getType()==4){
                    connection.setStartNum(packet.getSequenceNumber());
                } else if(packet.getType()==6){
                    connection.setEndNum(packet.getSequenceNumber());
                }
                if(connection.getStartNum()==-1||connection.getEndNum()==-1){
                    return;
                }
                if(connection.receiveAllPackets()){
                    // 加上多线程
                    ParseRequest parseRequest = new ParseRequest(deBugging,fileDirectory,connection,router);
                    String payload = "";
                    Iterator<Map.Entry<Long, Packet>> it = connection.getReceivePackets().entrySet().iterator();
                    while(it.hasNext()) {
                        Map.Entry<Long, Packet> entry = it.next();
                        payload += new String(entry.getValue().getPayload(), UTF_8).trim();
                    }
                    try {
                        synchronized (ParseRequest.class){
                            Request request = parseRequest.parseRequest(payload);
                            Response response = parseRequest.createResponse(request);
                            String handledResponse = parseRequest.handleResponse(response);
                            connection.makePackets(handledResponse,connection.getEndNum()+1,new InetSocketAddress(packet.getPeerAddress(),packet.getPeerPort()));
                            slidingWindow = new SlidingWindow(connection);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }else if(packet.getType()==7){
                System.out.println("A data packet ack has been received. The sequence num is "+packet.getSequenceNumber());
                slidingWindow.getWindow().put(packet.getSequenceNumber(),true);
                slidingWindow.sendNextPacket(packet.getSequenceNumber());
                for(boolean value: slidingWindow.getWindow().values()){
                    if(!value) return;
                }
                connection.setFinished(true);
            }
        }
    }

    /**
     * print deBugging Msg
     * @param string
     */
    public void printDebuggingMsg(String string){
        if(deBugging){
            System.out.println("Debugging Message: "+string);
        }
    }
}

package server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ReceivePacket extends Thread{

    private Connection connection;
    private boolean deBugging;
    private String fileDirectory;
    private SocketAddress router;
    private long sequenceNum;

    public ReceivePacket(Connection connection, boolean deBugging, String fileDirectory, SocketAddress router, long sequenceNum){
        this.router = router;
        this.connection = connection;
        this.deBugging = deBugging;
        this.fileDirectory = fileDirectory;
        this.sequenceNum = sequenceNum;
    }

    @Override
    public void run() {
        ByteBuffer buf = ByteBuffer
                .allocate(Packet.MAX_LEN)
                .order(ByteOrder.BIG_ENDIAN);
        while(!connection.isReceiveAll()){
            try{
                buf.clear();
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
                connection.setReceiveAll(true);
                System.out.println("Server has received the data packet.");

                Packet resPacket = new Packet.Builder()
                        .setType(4)
                        .setSequenceNumber(packet.getSequenceNumber())
                        .setPortNumber(packet.getPeerPort())
                        .setPeerAddress(packet.getPeerAddress())
                        .setPayload("".getBytes())
                        .create();

                try {
                    connection.getChannel().send(resPacket.toBuffer(),connection.getRouter());
                    ParseRequest parseRequest = new ParseRequest(packet,deBugging,fileDirectory,connection,router);

                    synchronized (ParseRequest.class){
                        String payload = new String(packet.getPayload(), UTF_8).trim();
                        Request request = parseRequest.parseRequest(payload);
                        Response response = parseRequest.createResponse(request);
                        parseRequest.sendResponse(response);
                    }
                    //试试这样在方法前面加上synchronized行不行
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else if(packet.getType()==4||packet.getType()==5||packet.getType()==6){
                System.out.println("Server has received the data packet. The sequence number is "+packet.getSequenceNumber());

                connection.addReceivePackets(packet);
                Packet resPacket = new Packet.Builder()
                        .setType(4)
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
                    ParseRequest parseRequest = new ParseRequest(packet,deBugging,fileDirectory,connection,router);
                    String payload = "";
                    Iterator<Map.Entry<Long, Packet>> it = connection.getReceivePackets().entrySet().iterator();
                    while(it.hasNext()) {
                        Map.Entry<Long, Packet> entry = it.next();
                        payload += new String(entry.getValue().getPayload(), UTF_8).trim();
                    }
                    try {
                        //System.out.println(payload.substring(0,30));
                        Request request = parseRequest.parseRequest(payload);
                        Response response = parseRequest.createResponse(request);
                        parseRequest.sendResponse(response);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
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

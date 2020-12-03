package client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ReceivePacket extends Thread{

    private Connection connection;
    private List<Long> hasGot;
    private Integer count;

    ReceivePacket(Connection connection){
        count = 0;
        this.connection = connection;
        hasGot = new ArrayList<>();
    }

    @Override
    public void run() {
        try{
            while(!connection.isFinished()){
                List<String> res;
                // Try to receive a packet within timeout.
                connection.getChannel().configureBlocking(false);
                Selector selector = Selector.open();
                connection.getChannel().register(selector, OP_READ);
                selector.select(2000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if(keys.isEmpty()){
                    System.out.println("No response after timeout");
                    //String msg = connection.handleRequest();
                    //connection.sendRequest(msg);
                    if(connection.isLastPacket()){
                        count++;
                        if(count>=4){
                            break;
                        }
                    }
                    continue;
                }

                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = connection.getChannel().receive(buf);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                if(resp.getType()==2&&Integer.parseInt(payload)==connection.getSequenceNum()+1){
                    connection.sendACK();
                }else if(resp.getType()==3){
                    System.out.println("A data packet ack has been received. The sequence num is "+resp.getSequenceNumber());
                    SlidingWindow slidingWindow = connection.getSlidingWindow();
                    slidingWindow.getWindow().put(resp.getSequenceNumber(),true);
                    slidingWindow.sendNextPacket(resp.getSequenceNumber());
                }else if(resp.getType()==0){
                    System.out.println("Client has received the data packet.");
                    Packet resPacket = new Packet.Builder()
                            .setType(7)
                            .setSequenceNumber(resp.getSequenceNumber())
                            .setPortNumber(resp.getPeerPort())
                            .setPeerAddress(resp.getPeerAddress())
                            .setPayload("".getBytes())
                            .create();
                    System.out.println("Data packet has been received");
                    //connection.setFinished(true);
                    if(!hasGot.contains(resp.getSequenceNumber())){
                        hasGot.add(resp.getSequenceNumber());
                        connection.getChannel().send(resPacket.toBuffer(),connection.getRouterAddr());
                        res = Arrays.asList(payload.split("\r\n"));
                        keys.clear();
                        connection.parseResponse(res);
                    }
                }else if(resp.getType()==4||resp.getType()==5||resp.getType()==6){
                    System.out.println("Client has received the data packet. The sequence number is "+resp.getSequenceNumber());
                    connection.addReceivePackets(resp);
                    Packet resPacket = new Packet.Builder()
                            .setType(7)
                            .setSequenceNumber(resp.getSequenceNumber())
                            .setPortNumber(resp.getPeerPort())
                            .setPeerAddress(resp.getPeerAddress())
                            .setPayload("".getBytes())
                            .create();
                    System.out.println("Client has sent the ack back to the server. The sequence number is "+resPacket.getSequenceNumber());
                    try {
                        connection.getChannel().send(resPacket.toBuffer(),connection.getRouterAddr());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(resp.getType()==4){
                        connection.setStartNum(resp.getSequenceNumber());
                    } else if(resp.getType()==6){
                        connection.setEndNum(resp.getSequenceNumber());
                    }
                    if(connection.getStartNum()==-1||connection.getEndNum()==-1){
                        continue;
                    }

                    if(connection.receiveAllPackets()){
                        String resPayload = "";
                        Iterator<Map.Entry<Long, Packet>> it = connection.getReceivePackets().entrySet().iterator();
                        while(it.hasNext()) {
                            Map.Entry<Long, Packet> entry = it.next();
                            resPayload += new String(entry.getValue().getPayload(), UTF_8).trim();
                        }
                        res = Arrays.asList(resPayload.split("\r\n"));
                        keys.clear();
                        connection.parseResponse(res);
                        //connection.setFinished(true);
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

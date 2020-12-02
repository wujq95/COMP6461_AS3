package client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class ReceivePacket extends Thread{

    private Connection connection;

    ReceivePacket(Connection connection){
        this.connection = connection;
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
                    String msg = connection.handleRequest();
                    //connection.sendRequest(msg);
                    continue;
                }

                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = connection.getChannel().receive(buf);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                if(resp.getType()==2&&Integer.parseInt(payload)==connection.getSequenceNum()+1){
                    connection.sendACK();
                }else if(resp.getType()==4){
                    System.out.println("A data packet ack has been received. The sequence num is "+resp.getSequenceNumber());
                    SlidingWindow slidingWindow = connection.getSlidingWindow();
                    slidingWindow.getWindow().put(resp.getSequenceNumber(),true);
                    slidingWindow.sendNextPacket(resp.getSequenceNumber());
                }else if(resp.getType()==5){
                    System.out.println("Data packet has been received");
                    connection.setFinished(true);
                    res = Arrays.asList(payload.split("\r\n"));
                    keys.clear();
                    connection.parseResponse(res);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

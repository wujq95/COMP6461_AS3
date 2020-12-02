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

public class PacketReceive extends Thread{

    private Connection connection;

    PacketReceive(Connection connection){
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
                selector.select(5000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if(keys.isEmpty()){
                    System.out.println("No response after timeout");
                    String msg = connection.handleRequest();
                    connection.sendRequest(msg);
                    continue;
                }
                // We just want a single response.
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = connection.getChannel().receive(buf);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                if(resp.getType()==2&&Integer.parseInt(payload)==connection.getSequenceNum()+1){
                    connection.sendACK();
                }else if(resp.getType()==4){
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

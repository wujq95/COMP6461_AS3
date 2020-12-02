package server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PacketReceive extends Thread{

    private Connection connection;
    private boolean deBugging;
    private String fileDirectory;
    private SocketAddress router;
    private long sequenceNum;

    public PacketReceive(Connection connection, boolean deBugging, String fileDirectory, SocketAddress router, long sequenceNum){
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
            System.out.println("Server has received the handshake step3 packet.");
            connection.setConnected(true);
            return;
        }
        if(connection.isConnected()){
            connection.setReceiveAll(true);
            System.out.println("Server has received the data packet.");
            ParseRequest parseRequest = new ParseRequest(packet,deBugging,fileDirectory,connection,router);
            /*try{
                synchronized (ParseRequest.class){
                    String payload = new String(packet.getPayload(), UTF_8).trim();
                    Request request = parseRequest.parseRequest(payload);
                    printDebuggingMsg("Request has been parsed: ");
                    printDebuggingMsg("Request Method: "+request.getMethod());
                    printDebuggingMsg("Request Path: "+request.getPath());
                    Response response = parseRequest.createResponse(request);
                    printDebuggingMsg("Response has been created: ");
                    printDebuggingMsg("Response Code: "+response.getCode());
                    printDebuggingMsg("Response Phrase: "+response.getPhrase());
                    parseRequest.sendResponse(response,connection,router);
                    printDebuggingMsg("Response has been sent to the client: \n");
                }
            }catch(Exception e){
                e.printStackTrace();
            }*/
            try {
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

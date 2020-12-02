package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class Connection extends Thread{

    private Packet packet;
    private boolean deBugging;
    private String fileDirectory;
    private List<Packet> packets;
    private  SocketAddress router;
    private Integer newPort;
    private long sequenceNum;
    private long ackNum;
    private Map<Long,String> receiveWindow;
    private Map<Long,String> sendWindow;
    private DatagramChannel channel;
    private boolean connected;
    private boolean receiveAll;
    private long startNum;
    private long endNum;


    public Connection(Packet packet,boolean deBugging,String fileDirectory, SocketAddress router){
        this.router = router;
        this.packet = packet;
        this.deBugging = deBugging;
        this.fileDirectory = fileDirectory;
        packets = new ArrayList<>();
        newPort = (int)(Math.random()*1000 + 9000);
        ackNum = packet.getSequenceNumber();
        sequenceNum = (long)(Math.random()*100000000);
        receiveWindow = new TreeMap<>();
        sendWindow = new HashMap<>();
        startNum = -1;
        endNum = -1;
        try{
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(newPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        new PacketReceive(this,deBugging,fileDirectory,router,sequenceNum).start();
        sendSynAndAck();
    }

    private void sendSynAndAck(){
        Packet synAck = new Packet.Builder()
                .setType(2)
                .setSequenceNumber(sequenceNum)
                .setPortNumber(packet.getPeerPort())
                .setPeerAddress(packet.getPeerAddress())
                .setPayload(String.valueOf(ackNum+1).getBytes())
                .create();
        do{
            try {
                channel.send(synAck.toBuffer(),router);
                System.out.println("Server has sent handshake step2 packet back to the client. ");
                Thread.sleep(500);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }while (!connected);
    }


    public DatagramChannel getChannel() {
        return channel;
    }

    public void setChannel(DatagramChannel channel) {
        this.channel = channel;
    }

    public boolean isReceiveAll() {
        return receiveAll;
    }

    public void setReceiveAll(boolean receiveAll) {
        this.receiveAll = receiveAll;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}


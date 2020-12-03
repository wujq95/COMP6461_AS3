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
    private ArrayList<Packet> packets;
    private  SocketAddress router;
    private Integer newPort;
    private long sequenceNum;
    private long ackNum;
    //private Map<Long,String> receiveWindow;
    //private Map<Long,String> sendWindow;
    TreeMap<Long, Packet> receivePackets;
    private DatagramChannel channel;
    private boolean connected;
    private boolean receiveAll;
    private long startNum;
    private long endNum;
    private boolean isFinished;
    private long packetNum;


    public Connection(Packet packet,boolean deBugging,String fileDirectory, SocketAddress router){
        this.router = router;
        this.packet = packet;
        this.deBugging = deBugging;
        this.fileDirectory = fileDirectory;
        packets = new ArrayList<>();
        newPort = (int)(Math.random()*1000 + 9000);
        ackNum = packet.getSequenceNumber();
        sequenceNum = (long)(Math.random()*100000000);
        receivePackets = new TreeMap<>();
        //receiveWindow = new TreeMap<>();
        //sendWindow = new HashMap<>();
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
        new ReceivePacket(this,deBugging,fileDirectory,router,sequenceNum).start();
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
                System.out.println("Server has sent handshake ack and syn back to the client. ");
                Thread.sleep(500);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }while (!connected);
    }

    public boolean receiveAllPackets(){
        if(endNum==-1||startNum==-1) return  false;
        if(endNum-startNum+1==receivePackets.size()) return true;
        return false;
    }

    public void makePackets(String msg,long packetNum,InetSocketAddress serverAddr){
        this.packetNum = packetNum;
        if(msg.length()<=1000){
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(packetNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            //sendWindow.put(packetNum,false);
            packets.add(p);
            return;
        }
        String firstData = msg.substring(0,1000);
        msg = msg.substring(1000);
        Packet firstP = new Packet.Builder()
                .setType(4)
                .setSequenceNumber(packetNum++)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(firstData.getBytes())
                .create();
        //sendWindow.put(packetNum,false);
        packets.add(firstP);
        while(msg.length()>1000){
            String majorData = msg.substring(0,1000);
            msg = msg.substring(1000);
            Packet majorP = new Packet.Builder()
                    .setType(5)
                    .setSequenceNumber(packetNum++)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(majorData.getBytes())
                    .create();
            //sendWindow.put(packetNum,false);
            packets.add(majorP);
        }
        Packet lastP = new Packet.Builder()
                .setType(6)
                .setSequenceNumber(packetNum++)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(msg.getBytes())
                .create();
        //sendWindow.put(packetNum,false);
        packets.add(lastP);
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

    public SocketAddress getRouter() {
        return router;
    }

    public void setRouter(SocketAddress router) {
        this.router = router;
    }

    public ArrayList<Packet> getPackets() {
        return packets;
    }

    public void setPackets(ArrayList<Packet> packets) {
        this.packets = packets;
    }

    public TreeMap<Long, Packet> getReceivePackets() {
        return receivePackets;
    }

    public void setReceivePackets(TreeMap<Long, Packet> receivePackets) {
        this.receivePackets = receivePackets;
    }

    public void addReceivePackets(Packet packet) {
        receivePackets.put(packet.getSequenceNumber(),packet);
    }

    public long getStartNum() {
        return startNum;
    }

    public void setStartNum(long startNum) {
        this.startNum = startNum;
    }

    public long getEndNum() {
        return endNum;
    }

    public void setEndNum(long endNum) {
        this.endNum = endNum;
    }


    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }


    public long getPacketNum() {
        return packetNum;
    }

    public void setPacketNum(long packetNum) {
        this.packetNum = packetNum;
    }
}


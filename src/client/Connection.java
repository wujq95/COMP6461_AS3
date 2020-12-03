package client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;

public class Connection {

    private DatagramChannel channel;
    private long ackNum;
    private long sequenceNum;
    private long packetNum;
    private long startNum;
    private long endNum;
    private SocketAddress routerAddr;
    private InetSocketAddress serverAddr;
    private Request request;
    private boolean isFinished;
    private ArrayList<Packet> packets;
    private SlidingWindow slidingWindow;
    private TreeMap<Long, Packet> receivePackets;
    private boolean hasHandledPackets;


    /**
     * constructor method
     */
    public Connection(){
        sequenceNum = (long) (Math.random() * 100000000);
        routerAddr = new InetSocketAddress("localhost", 3000);
        packets = new ArrayList<>();
        receivePackets = new TreeMap<>();
        packetNum = 0;
        startNum = -1;
        endNum = -1;

        try {
            channel = DatagramChannel.open();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * handle method
     * @param request
     * @throws IOException
     */
    public void requestHandle(Request request) throws Exception {
        this.request = request;
        serverAddr = new InetSocketAddress(request.getHost(), request.getPort());

        //hand shake process
        handshake();

        //start the packet receive thread
        new ReceivePacket(this).start();

        //divide the packets
        makePackets(handleRequest());

        //start the sliding window
        slidingWindow = new SlidingWindow(this,packets);
    }

    /**
     * handshake process
     * @throws Exception
     */
    private void handshake() throws Exception {
        System.out.println("Handshake start");

        //send syn to the server
        sendSYN();

        //send ack to the server
        sendACK();
    }

    /**
     * hand shake first step: send syn
     * @throws Exception
     */
    private void sendSYN() throws Exception {
        Packet packet = new Packet.Builder()
                .setType(1)
                .setSequenceNumber(sequenceNum)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload("".getBytes())
                .create();

        do{
            channel.send(packet.toBuffer(), routerAddr);
            Thread.sleep(500);
            System.out.println("Client has sent handshake syn to the server");
        } while(!receiveSYN_ACK());
    }

    /**
     * handshake second step: receive syn and ack
     * @return
     * @throws Exception
     */
    public boolean receiveSYN_ACK() throws Exception{
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        selector.select(2000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if(keys.isEmpty()){
            System.out.println("No handshake syn back after timeout");
            return false;
        }

        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
        if(resp.getType()==2&&Integer.parseInt(payload)==sequenceNum+1){
            System.out.println("Client has received a handshake syn and ack from the sever");
            serverAddr = new InetSocketAddress(resp.getPeerAddress(), resp.getPeerPort());
            ackNum = resp.getSequenceNumber()+1;
            keys.clear();
            return true;
        }
        return false;
    }

    /**
     * handshake third step: send ack
     * @throws IOException
     */
    public void sendACK() throws IOException{
        Packet packet = new Packet.Builder()
                .setType(3)
                .setSequenceNumber(ackNum)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload("".getBytes())
                .create();
        channel.send(packet.toBuffer(), routerAddr);
        System.out.println("Client has sent handshake ack back to the server. ");
    }

    /**
     * handle request to have a String Message
     * @return
     */
    public String handleRequest(){
        String msg = "";
        String data = "";
        if(request.isPost()){
            data = "{";
            for(int i=0;i<request.getInlineData().size();i++){
                data += request.getInlineData().get(i);
                if(i!=request.getInlineData().size()-1){
                    data += ",";
                }
            }
            data += "}";
        }
        if(request.isPost()){
            msg += "POST " + request.getPath() + " "+request.getVersion()+"\r\n";
            msg += "Host: "  + request.getHost() + "\r\n";
            msg += "Content-Length: " + data.length() + "\r\n";
        }else{
            msg += "GET " + request.getPath() + " "+request.getVersion()+"\r\n";
            msg += "Host: " + request.getHost() + "\r\n";
        }
        if(request.getHeader()!=null&&request.getHeader().size()>0){
            for(String str:request.getHeader()){
                msg += str+"\r\n";
            }
        }
        msg +="\r\n";
        msg += data;
        return msg;
    }

    /**
     * divide the message into packets
     * @param msg
     */
    private void makePackets(String msg){
        if(msg.length()<=1000){
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(packetNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
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
            packets.add(majorP);
        }
        Packet lastP = new Packet.Builder()
                .setType(6)
                .setSequenceNumber(packetNum++)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(msg.getBytes())
                .create();
        packets.add(lastP);
    }


    /**
     * parse the response
     * @param res
     * @throws IOException
     */
    public void parseResponse(List<String> res) throws IOException{
        if(res.size()==0){
            System.out.println("No response");
            return;
        }
        if(res.get(0).split(" ")[1].startsWith("3")){
            if(request.getRedirect()>5) {
                System.out.println("Redirect too many times error");
                return;
            }
            for(String str: res){
                if(str.startsWith("Location: ")){
                    String redirectAddress = str.split(" ")[1];
                    if(!checkURL(redirectAddress)){
                        System.out.println("Incorrect redirect address");
                        return;
                    }
                    System.out.println("Redirect to New Location: "+redirectAddress);
                    String url = redirectAddress.substring(7);
                    if(redirectAddress.startsWith("https")){
                        url = redirectAddress.substring(8);
                    }
                    String host = url.split("/")[0];
                    String path = url.substring(host.length());
                    if(path.length()==0){
                        path = "/";
                    }
                    request.setPath(path.trim());
                    request.setHost(host.trim());
                    request.setRedirect(request.getRedirect()+1);
                    serverAddr = new InetSocketAddress(request.getHost(), request.getPort());
                    try {
                        handshake();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    slidingWindow = new SlidingWindow(this,packets);
                }
            }
        }else{
            outputResult(res);
        }
    }

    /**
     * print the response or output it to the file
     * @param res
     * @throws IOException
     */
    public void outputResult(List<String> res) throws IOException {
        if(request.getOutPutFile()!=null){
            FileWriter fileWriter = new FileWriter(request.getOutPutFile());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            if(request.isVerbose()){
                for(String line:res){
                    printWriter.println(line);
                }
            }else{
                boolean flag = false;
                for(String line:res){
                    if(line.equals("{")||(line.startsWith("<!doctype html>"))){
                        flag = true;
                    }
                    if(flag){
                        printWriter.println(line);
                    }
                }
            }
            printWriter.close();
            fileWriter.close();
            System.out.println("Response has been stored into the file: "+request.getOutPutFile());
        }else{
            if(request.isVerbose()){
                for(String line:res){
                    System.out.println(line);
                }
            }else{
                boolean flag = false;
                for(String line:res){
                    if(line.equals("{")||(line.startsWith("<!doctype html>"))){
                        flag = true;
                    }
                    if(flag){
                        System.out.println(line);
                    }
                }
            }
        }
        isFinished = true;
    }

    /**
     * check is all packets have been received
     * @return
     */
    public boolean receiveAllPackets(){
        if(endNum==-1||startNum==-1) return  false;
        if(endNum-startNum+1==receivePackets.size()) return true;
        return false;
    }

    /**
     * add received packets to the map
     * @param packet
     */
    public void addReceivePackets(Packet packet) {
        receivePackets.put(packet.getSequenceNumber(),packet);
    }

    /**
     * check url format
     * @param url
     * @return
     */
    public boolean checkURL(String url) {
        return url.length()>6&&(url.startsWith("http://")||url.startsWith("https://"));
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public void setChannel(DatagramChannel channel) {
        this.channel = channel;
    }

    public long getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(long sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public SocketAddress getRouterAddr() {
        return routerAddr;
    }

    public void setRouterAddr(SocketAddress routerAddr) {
        this.routerAddr = routerAddr;
    }

    public SlidingWindow getSlidingWindow() {
        return slidingWindow;
    }

    public void setSlidingWindow(SlidingWindow slidingWindow) {
        this.slidingWindow = slidingWindow;
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

    public TreeMap<Long, Packet> getReceivePackets() {
        return receivePackets;
    }

    public void setReceivePackets(TreeMap<Long, Packet> receivePackets) {
        this.receivePackets = receivePackets;
    }

    public boolean isHasHandledPackets() {
        return hasHandledPackets;
    }

    public void setHasHandledPackets(boolean hasHandledPackets) {
        this.hasHandledPackets = hasHandledPackets;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}

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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class Connection {

    private DatagramChannel channel;
    private long ackNum;
    private boolean connected;
    private long sequenceNum;
    private SocketAddress routerAddr;
    private InetSocketAddress serverAddr;
    private Request request;
    private boolean finished;
    private boolean handshakeFinished;

    public Connection(){
        sequenceNum = (long) (Math.random() * 100000000);
        routerAddr = new InetSocketAddress("localhost", 3000);

        try {
            channel = DatagramChannel.open();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * send request and receive response
     * @param request
     * @throws IOException
     */
    public void requestHandle(Request request) throws Exception {
        this.request = request;
        serverAddr = new InetSocketAddress(request.getHost(), request.getPort());
        handshake();
        String msg = handleRequest();
        new PacketReceive(this).start();
        sendRequest(msg);
    }

    /**
     * handshake process
     * @throws Exception
     */
    private void handshake() throws Exception {
        sendSYN();
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
            System.out.println("Client has sent handshake step1 syn to the server");
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
            System.out.println("No syn back after timeout");
            return false;
        }

        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
        if(resp.getType()==2&&Integer.parseInt(payload)==sequenceNum+1){
            System.out.println("Client has received a handshake step2 syn and ack from the sever");
            serverAddr = new InetSocketAddress(resp.getPeerAddress(), resp.getPeerPort());
            ackNum = resp.getSequenceNumber()+1;
            connected = true;
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
        System.out.println("Client has sent handshake step3 ack back to the server. ");
    }

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


    public void sendRequest(String msg){
        Packet p = new Packet.Builder()
                .setType(4)
                .setSequenceNumber(1L)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(msg.getBytes())
                .create();
        try {
            channel.send(p.toBuffer(), routerAddr);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void receiveRequest(){
        try{
            List<String> res;
            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                System.out.println("No response after timeout");
                return;
            }
            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            if(resp.getType()==2&&Integer.parseInt(payload)==sequenceNum+1){
                sendACK();
            }else if(resp.getType()==4){
                finished = true;
                res = Arrays.asList(payload.split("\r\n"));
                keys.clear();
                parseResponse(res);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void parseResponse(List<String> res) throws IOException{
        if(res.size()==0){
            System.out.println("No response");
            return;
        }
        if(res.get(0).split(" ")[1].startsWith("3")){
            if(request.getRedirect()>5) return;
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
                    String msg = handleRequest();
                    sendRequest(msg);
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

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}

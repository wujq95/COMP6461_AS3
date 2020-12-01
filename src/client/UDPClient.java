package client;

import java.io.*;
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

public class UDPClient {

    private long ackNum;
    private boolean connected;

    /**
     * main method
     * @param args
     * @throws IOException
     */
    public static void main(String [] args) throws Exception {
        UDPClient client = new UDPClient();
        if(args.length<=1||!args[0].equalsIgnoreCase("httpc")){
            System.out.println("Wrong Syntax.\n");
        } else if(args[1].equalsIgnoreCase("help")){
            client.parseHelp(args);
        } else if(args[1].equalsIgnoreCase("get")){
            Request request = new Request("get");
            client.setRequest(request,args);
        } else if(args[1].equalsIgnoreCase("post")){
            Request request = new Request("post");
            client.setRequest(request,args);
        } else {
            System.out.println("Wrong Syntax");
        }
    }

    /**
     * help function
     * @param args
     */
    public void parseHelp(String [] args){
        if(args.length == 2){
            System.out.println("httpc is a curl-like application but supports HTTP protocol only.\n" +
                    "Usage:\n" +
                    "   httpc command [arguments]\n" +
                    "The commands are:\n" +
                    "   get     executes a HTTP GET request and prints the response.\n" +
                    "   post    executes a HTTP POST request and prints the response.\n" +
                    "   help    prints this screen.\n" +
                    "Use \"httpc help [command]\" for more information about a command.\n");
        }else if(args.length == 3 && args[2].equals("get")){
            System.out.println("httpc help get\n" +
                    "usage: httpc get [-v] [-h key:value] URL\n" +
                    "Get executes a HTTP GET request for a given URL.\n" +
                    "   -v              Prints the detail of the response such as protocol, status, and headers.\n" +
                    "   -h key:value    Associates headers to HTTP Request with the format 'key:value'.\n");
        }else if(args.length == 3 && args[2].equals("post")){
            System.out.println("httpc help post\n" +
                    "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                    "Post executes a HTTP POST request for a given URL with inline data or from file.\n" +
                    "   -v              Prints the detail of the response such as protocol, status, and headers.\n" +
                    "   -h key:value    Associates headers to HTTP Request with the format 'key:value'.\n" +
                    "   -d              string Associates an inline data to the body HTTP POST request.\n" +
                    "   -f              file Associates the content of a file to the body HTTP POST request.\n" +
                    "Either [-d] or [-f] can be used but not both.\n");
        }else {
            System.out.println("Wrong syntax\n");
        }
    }

    /**
     * set request parameters
     */
    public void setRequest(Request request, String[] args) throws Exception {
        if(args.length==2){
            System.out.println("Wrong Syntax\n");
            return;
        }
        int urlIndex = args.length-1;
        if(args[args.length-2].equals("-o")){
            if(!checkFileName(args[args.length-1])){
                System.out.println("Incorrect Output File Name\n");
                return;
            }
            request.setOutPutFile(args[args.length-1]);
            urlIndex = args.length - 3;
        }

        if(!checkURL(args[urlIndex])){
            System.out.println("Incorrect URL Format\n");
            return;
        }
        String url = args[urlIndex].substring(7);
        if(args[urlIndex].startsWith("https")){
            url = args[urlIndex].substring(8);
        }
        String host = url.split("/")[0];
        String path = url.substring(host.length());
        if(path.length()==0){
            path = "/";
        }
        request.setPath(path.trim());
        request.setHost(host.trim());
        int index= 2;
        if(args[2].equals("-v")){
            request.setVerbose(true);
            index++;
        }
        if((urlIndex-index)%2!=0){
            System.out.println("Wrong Syntax\n");
            return;
        }
        while(index<urlIndex-1&&args[index].equals("-h")){
            if(!checkHeader(args[index+1])){
                System.out.println("Wrong Syntax\n");
                return;
            }
            request.addHeader(args[index+1]);
            index +=2;
        }
        if(request.isPost()){
            if (index==urlIndex){
                sendRequest(request);
            } else if(args[index].equals("-f")){
                if(index+2==urlIndex){
                    String fileAddress = args[index+1];
                    File file = new File(fileAddress);
                    if(!file.exists()){
                        System.out.println("File does not exist");
                        return;
                    }
                    FileReader fileReader = new FileReader(fileAddress);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    String line = bufferedReader.readLine();
                    bufferedReader.close();
                    fileReader.close();
                    if(line==null){
                        System.out.println("File is empty");
                        return;
                    }
                    if(!checkInlineData(line)){
                        System.out.println("Incorrect File Content\n");
                        return;
                    }
                    String[] strings = line.substring(1,line.length()-1).split(",");
                    for(String str:strings){
                        request.addInlineData(str);
                    }
                    sendRequest(request);
                }else{
                    System.out.println("Wrong Syntax\n");
                }
            }else {
                while (index < urlIndex - 1) {
                    if (!args[index].equals("-d")) {
                        System.out.println("Wrong Syntax\n");
                        return;
                    }
                    if (!checkInlineData(args[index + 1])) {
                        System.out.println("Incorrect Inline Data Format\n");
                        return;
                    }
                    String line = args[index + 1];
                    String[] strings = line.substring(1, line.length() - 1).split(",");
                    for (String str : strings) {
                        request.addInlineData(str);
                    }
                    index += 2;
                }
                sendRequest(request);
            }
        }else{
            if (index==urlIndex){
                sendRequest(request);
            }else{
                System.out.println("Wrong Syntax\n");
            }
        }
    }

    /**
     * send request and receive response
     * @param request
     * @throws IOException
     */
    public void sendRequest(Request request) throws Exception {
        SocketAddress routerAddr = new InetSocketAddress("localhost", 3000);
        InetSocketAddress serverAddr = new InetSocketAddress(request.getHost(), request.getPort());
        handshake(routerAddr,serverAddr);
        List<String> res;

        try(DatagramChannel channel = DatagramChannel.open()){

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

            Packet p = new Packet.Builder()
                    .setType(4)
                    .setSequenceNumber(1L)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddr);

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
            res = Arrays.asList(payload.split("\r\n"));
            keys.clear();
        }
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
                    sendRequest(request);
                }
            }
        }else{
            outputResult(request,res);
        }
    }
    
    public void handshake(SocketAddress routerAddr,InetSocketAddress serverAddr) throws Exception {
        Long sequenceNum = generateRandomSequenceNum();
        sendSYN(sequenceNum,routerAddr,serverAddr);
        sendACK(routerAddr,serverAddr);
    }

    public Long generateRandomSequenceNum(){
        return (long) (Math.random() * 100000000);
    }

    public void sendSYN(Long sequenceNum,SocketAddress routerAddr,InetSocketAddress serverAddr) throws Exception {
        try(DatagramChannel channel = DatagramChannel.open()){
            Packet packet = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(sequenceNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload("".getBytes())
                    .create();

            do{
                channel.send(packet.toBuffer(), routerAddr);
                System.out.println("step1: send handshake syn to the server");
            } while(!receiveSYN_ACK(channel,sequenceNum));
        }

    }

    public boolean receiveSYN_ACK(DatagramChannel channel,Long sequenceNum) throws Exception{
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
            System.out.println("step3: receive a handshake syn and ack");
            ackNum = resp.getSequenceNumber();
            connected = true;
            keys.clear();
            return true;
        }
        return false;

    }

    public void sendACK(SocketAddress routerAddr,InetSocketAddress serverAddr) throws IOException{
        try (DatagramChannel channel = DatagramChannel.open()) {
            Packet packet = new Packet.Builder()
                    .setType(3)
                    .setSequenceNumber(ackNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload("".getBytes())
                    .create();
            channel.send(packet.toBuffer(), routerAddr);
            System.out.println("send handshake ack");

        }
    }
    

    /**
     * print the response or output it to the file
     * @param request
     * @param res
     * @throws IOException
     */
    public void outputResult(Request request, List<String> res) throws IOException {
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

    /**
     * check header format
     * @param header
     * @return
     */
    public boolean checkHeader(String header){
        if(!header.contains(":")) return false;
        String[] strings = header.split(":");
        if(strings.length==2&&(!strings[0].equals(""))&&(!strings[1].equals(""))) return true;
        return false;
    }

    /**
     * check inline data format
     * @param inlineData
     * @return
     */
    public boolean checkInlineData(String inlineData){
        if(inlineData.length()<2) return false;
        char start = inlineData.charAt(0);
        char end = inlineData.charAt(inlineData.length()-1);
        if(start!='{'||end!='}') return false;
        String string = inlineData.substring(1,inlineData.length()-1);
        String[] strings = string.split(",");
        for(String str:strings){
            if(!checkHeader(str)) return false;
        }
        return  true;
    }

    /**
     * check file name format
     * @param fileName
     * @return
     */
    public boolean checkFileName(String fileName){
        return fileName.endsWith(".txt");
    }

}


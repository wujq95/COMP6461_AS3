package server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class UDPServer {

    private static Integer port = 8007;
    private static boolean deBugging;
    private static String fileDirectory = "file";

    public static void main(String[] args) throws IOException{

        args = new String[]{"httpfs"};

        if(checkSyntax(args)){
            try (DatagramChannel channel = DatagramChannel.open()) {
                channel.bind(new InetSocketAddress(port));
                System.out.println("Server starts running");

                ByteBuffer buf = ByteBuffer
                        .allocate(Packet.MAX_LEN)
                        .order(ByteOrder.BIG_ENDIAN);

                for (; ; ) {
                    buf.clear();
                    SocketAddress router = channel.receive(buf);
                    buf.flip();
                    Packet packet = Packet.fromBuffer(buf);
                    buf.flip();
                    //new serverThread(packet,deBugging,fileDirectory,router,channel).start();
                    if(packet.getType()==1){
                        System.out.println("Server has received the handshake step1 packet.");
                        new Connection(packet,deBugging,"src/"+fileDirectory,router).start();
                    }
                }
            }
        }
    }

    public static boolean checkSyntax(String[] args){
        if(args.length==0||!args[0].equals("httpfs")){
            System.out.println("Wrong Syntax.\n");
            return false;
        }
        if(args.length==1){
            return true;
        }
        int index = 1;
        if(args[1].equals("-v")){
            deBugging = true;
            index++;
        }

        if((args.length-index)==0){
            return true;
        }else if((args.length-index)==2){
            if(args[index].equals("-p")){
                if(checkNum(args[index+1])){
                    port = Integer.parseInt(args[index+1]);
                    return true;
                }else{
                    System.out.println("Port must be a number.\n");
                    return false;
                }
            }else if(args[index].equals("-d")){
                fileDirectory = args[index+1];
                File file = new File(fileDirectory);
                if(file.exists()){
                    return true;
                }else{
                    System.out.println("File does not exist\n");
                    return false;
                }
            }else{
                System.out.println("Wrong Syntax.\n");
                return false;
            }
        }else if((args.length-index)==4){
            if(args[index].equals("-p")&&args[index+2].equals("-d")){
                if(!checkNum(args[index+1])){
                    System.out.println("Port must be a number.\n");
                    return false;
                }
                port = Integer.parseInt(args[index+1]);
                fileDirectory = args[index+3];
                File file = new File(fileDirectory);
                if(file.exists()){
                    return true;
                }else{
                    System.out.println("File does not exist\n");
                    return false;
                }
            }else{
                System.out.println("Wrong Syntax.\n");
                return false;
            }
        }else{
            System.out.println("Wrong Syntax.\n");
            return false;
        }
    }

    public static boolean checkNum(String str){
        if(str==null||str.length()==0) return false;
        if(str.equals("0")) return true;
        char c = str.charAt(0);
        if(Character.isDigit(c)&& c!='0'){
            for(int i=1;i<str.length();i++){
                if(!Character.isDigit(str.charAt(i))){
                    return false;
                }
            }
        }else{
            return false;
        }
        return  true;
    }
}
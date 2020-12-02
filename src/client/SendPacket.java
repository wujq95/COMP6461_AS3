package client;

import java.io.IOException;

public class SendPacket extends Thread{

    private SlidingWindow slidingWindow;
    private Packet packet;
    private Connection connection;

    public SendPacket(Connection connection, SlidingWindow slidingWindow, Packet packet){
        this.connection = connection;
        this.slidingWindow = slidingWindow;
        this.packet = packet;
    }

    @Override
    public void run() {
        while(!slidingWindow.getWindow().get(packet.getSequenceNumber())){
            try {
                connection.getChannel().send(packet.toBuffer(), connection.getRouterAddr());
                System.out.println("A data packet has been sent to the sever. The sequence number is "+packet.getSequenceNumber());
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e){
                e.getStackTrace();
            }
        }
    }
}

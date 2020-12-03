package server;

import java.io.IOException;

public class SendPacket extends Thread{

    private SlidingWindow slidingWindow;
    private Packet packet;
    private Connection connection;
    private boolean notFirstTime;

    /**
     * constructor method
     * @param connection
     * @param slidingWindow
     * @param packet
     */
    public SendPacket(Connection connection, SlidingWindow slidingWindow, Packet packet){
        this.connection = connection;
        this.slidingWindow = slidingWindow;
        this.packet = packet;
    }

    @Override
    public void run() {
        while(!slidingWindow.getWindow().get(packet.getSequenceNumber())){
            try {
                if(notFirstTime){
                    connection.printDebuggingMsg("The "+packet.getSequenceNumber()+" packet is time out");
                    }
                notFirstTime = true;
                connection.getChannel().send(packet.toBuffer(), connection.getRouter());
                connection.printDebuggingMsg("Server sent "+packet.getSequenceNumber()+" packet to the client");
                Thread.sleep(600);
            } catch (IOException | InterruptedException e){
                e.getStackTrace();
            }
        }
    }
}

package server;

import java.util.ArrayList;
import java.util.HashMap;

public class SlidingWindow {

    private Connection connection;
    private long start;
    private final Integer windowLength;
    private ArrayList<Packet> packets;
    private static HashMap<Long,Boolean> window;

    public SlidingWindow(Connection connection){
        this.connection = connection;
        start = 0;
        windowLength = 4;
        packets = connection.getPackets();
        window = new HashMap<>();

        for(int i=0;i<packets.size();i++){
            window.put(packets.get(i).getSequenceNumber(),false);
        }

        if(packets.size()<=windowLength){
            for(int i=0;i<packets.size();i++){
                new SendPacket(connection,this,packets.get(i)).start();
            }
        }else {
            for (int i = 0; i < windowLength; i ++){
                new SendPacket(connection,this,packets.get(i)).start();
            }
        }
    }

    public void sendNextPacket(long sequenceNum){
        long end = start+windowLength-1;
        if(packets.get((int)start).getSequenceNumber()==sequenceNum){
            while(end<packets.size()-1&&window.get(packets.get((int)start).getSequenceNumber())){
                start++;
                end++;
                new SendPacket(connection,this, packets.get((int)end)).start();
            }
        }
    }


    public static HashMap<Long, Boolean> getWindow() {
        return window;
    }

    public static void setWindow(HashMap<Long, Boolean> window) {
        SlidingWindow.window = window;
    }
}

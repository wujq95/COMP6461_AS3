package client;

import java.util.ArrayList;
import java.util.HashMap;

public class SlidingWindow {

    private long start;
    private final Integer windowLength;
    private Connection connection;
    private ArrayList<Packet> packets;
    private static HashMap<Long,Boolean> window;

    /**
     * constructor method
     * @param connection
     * @param packets
     */
    public SlidingWindow(Connection connection, ArrayList<Packet> packets){
        start = 0;
        windowLength = 4;
        this.packets = packets;
        this.connection = connection;
        window = new HashMap<>();

        for(long i=0;i<packets.size();i++){
            window.put(i,false);
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

    /**
     * send next packet of the window
     * @param sequenceNum
     */
    public void sendNextPacket(long sequenceNum){
        long end = start+windowLength-1;
        if(packets.get((int)start).getSequenceNumber()==sequenceNum){
            while(end<packets.size()-1&&window.get(start)){
                start++;
                end++;
                new SendPacket(connection,this, packets.get((int)end)).start();
            }
        }
    }

    public HashMap<Long, Boolean> getWindow() {
        return window;
    }

    public void setWindow(HashMap<Long, Boolean> window) {
        this.window = window;
    }
}

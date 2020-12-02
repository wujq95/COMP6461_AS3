package client;

import java.util.ArrayList;
import java.util.HashMap;

public class SlidingWindow {

    private long start;
    //private Integer end;
    private final Integer windowLength;
    private Connection connection;
    private ArrayList<Packet> packets;
    private boolean allFinished;
    private static HashMap<Long,Boolean> window;

    public SlidingWindow(Connection connection, ArrayList<Packet> packets){
        start = 0;
        windowLength = 4;
        this.packets = packets;
        this.connection = connection;
        window = new HashMap<>();

        for(int i=0;i<packets.size();i++){
            window.put((long)i,false);
        }

        if(packets.size()<=windowLength){
            for(int i=0;i<packets.size();i++){
                //window.put((long)i,false);
                new SendPacket(connection,this,packets.get(i)).start();
            }
            allFinished = true;
        }else {
            for (int i = 0; i < windowLength; i ++){
                //window.put((long)i,false);
                new SendPacket(connection,this,packets.get(i)).start();

            }
        }
    }

    public void sendNextPacket(long sequenceNum){
        long end = start+windowLength-1;
        if(packets.get((int)start).getSequenceNumber()==sequenceNum){
            while(end<packets.size()-1&&window.get(start)){
                start++;
                end++;
                //window.put((long)end,false);
                new SendPacket(connection,this, packets.get((int)end)).start();
                if(end == packets.size()-1) allFinished = true;
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

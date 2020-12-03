package client;

import java.io.*;

public class UDPClient {


    /**
     * main method
     * @param args
     * @throws IOException
     */
    public static void main(String [] args) throws Exception {

        ParseArgs parseArgs = new ParseArgs(new Connection());
        parseArgs.parseArgs(args);

    }
}


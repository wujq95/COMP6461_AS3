package client;

import java.io.*;

public class UDPClient {


    /**
     * main method
     * @param args
     * @throws IOException
     */
    public static void main(String [] args) throws Exception {

        //args = new String[]{"httpc","get","-v", "http://localhost/"};
        /*String[] args1 = new String[]{"httpc","get","-v", "http://localhost/data.json"};
        String[] args2 = new String[]{"httpc","post","-v", "-d", "{'Assignment': 4}", "http://localhost/data.json"};

        ParseArgs parseArgs1 = new ParseArgs(new Connection());
        parseArgs1.parseArgs(args1);

        ParseArgs parseArgs2 = new ParseArgs(new Connection());
        parseArgs2.parseArgs(args2);*/

        args = new String[]{"httpc","get","-v", "http://localhost/data2.json"};;
        ParseArgs parseArgs = new ParseArgs(new Connection());
        parseArgs.parseArgs(args);

    }



}


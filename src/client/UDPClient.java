package client;

import java.io.*;

public class UDPClient {


    /**
     * main method
     * @param args
     * @throws IOException
     */
    public static void main(String [] args) throws Exception {

        args = new String[]{"httpc","get","-v","http://localhost/"};

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
        Connection connection = new Connection();
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
                connection.requestHandle(request);
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
                    connection.requestHandle(request);
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
                connection.requestHandle(request);
            }
        }else{
            if (index==urlIndex){
                connection.requestHandle(request);
            }else{
                System.out.println("Wrong Syntax\n");
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


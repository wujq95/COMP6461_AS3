import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Request {

    private String host;
    private int port;
    private String path;
    private String version;
    private List<String> header;
    private List<String> inlineData;
    private boolean verbose;
    private boolean isPost;
    private String outPutFile;
    private int redirect;

    public int getRedirect() {
        return redirect;
    }

    public void setRedirect(int redirect) {
        this.redirect = redirect;
    }

    public Request(String str){
        redirect = 0;
        port = 8007;
        version = "HTTP/1.0";
        if(str.equals("post")){
            isPost = true;
        }
    }

    public boolean isPost() {
        return isPost;
    }

    public void setPost(boolean post) {
        isPost = post;
    }

    public String getOutPutFile() {
        return outPutFile;
    }

    public void setOutPutFile(String outPutFile) {
        this.outPutFile = outPutFile;
    }

    public List<String> getHeader() {
        return header;
    }

    public void setHeader(List<String> header) {
        this.header = header;
    }

    public void addHeader(String newHeader){
        if(header==null){
            header = new ArrayList<>();
        }
        header.add(newHeader);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getInlineData() {
        if(inlineData==null){
            inlineData = new ArrayList<>();
        }
        return inlineData;
    }

    public void setInlineData(List<String> inlineData) {
        this.inlineData = inlineData;
    }

    public void addInlineData(String newInlineData){
        if(inlineData==null){
            inlineData = new ArrayList<>();
        }
        inlineData.add(newInlineData);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}



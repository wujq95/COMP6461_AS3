package server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Request {
    private InputStream input;
    private String method;
    private String path;
    private String version;
    private List<String> headers;
    private String data;

    public Request(){
        headers = new ArrayList<String>();
    }
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

    public List<String> getHeaders() {
        return headers;
    }

    public void addHeaders(String headers) {
        this.headers.add(headers);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


}

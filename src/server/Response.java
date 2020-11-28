package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class Response {
    private int code;
    private String phrase;
    private List<String> headers;
    private List<String> data;

    public Response(){
        headers = new ArrayList<>();
        data = new ArrayList<>();
        Calendar cd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String s = sdf.format(cd.getTime());
        headers.add("Date: " + s);
    }
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers.add(headers);
    }

    public List<String> getData() {
        return data;
    }

    public void setData(String data) {

        this.data.add(data);
    }
    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }





}

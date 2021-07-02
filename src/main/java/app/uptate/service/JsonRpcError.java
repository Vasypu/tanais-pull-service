package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcError {
//    @JsonProperty("code")
//    private int code;
    @JsonProperty("message")
    private String message;
//    @JsonProperty("data")
//    private Object data;

//    public int getCode() {
//        return code;
//    }

//    public void setCode(int code) {
//        this.code = code;
//    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

//    public Object getData() {
//        return data;
//    }
//
//    public void setData(Object data) {
//        this.data = data;
//    }
}

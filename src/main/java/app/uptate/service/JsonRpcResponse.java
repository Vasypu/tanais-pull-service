package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JsonRpcResponse /*extends JsonRpcMessage*/ {
//    @JsonProperty("error")
//    private String error = null;
//    @JsonProperty("result")
//    private Object result = null;
    JsonRpcResponse() {}
    @JsonProperty("errors")
    private List<String> errors;

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

//    public String getError() { return error; }
//
//    public void setError(String error) {
//        this.error = error;
//    }

//    public Object getResult() { return result; }
//
//    public void setResult(Object result) {
//        this.result = result;
//    }

    public boolean success() { return errors == null; }
}

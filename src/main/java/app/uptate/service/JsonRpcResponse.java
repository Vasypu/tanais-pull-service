package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JsonRpcResponse {
    JsonRpcResponse() {}
    @JsonProperty("errors")
    private List<String> errors;

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean success() { return errors == null; }
}

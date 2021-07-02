package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcMessage {
    @JsonProperty("id")
    private String id = null;
    @JsonProperty("jsonrpc")
    private String jsonRpcVersion = "2.0";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJsonRpcVersion() {
        return jsonRpcVersion;
    }

    public void setJsonRpcVersion(String jsonRpcVersion) {
        this.jsonRpcVersion = jsonRpcVersion;
    }
}

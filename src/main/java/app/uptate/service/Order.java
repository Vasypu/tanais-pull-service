package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {

    public Order() {}
    @JsonProperty("num")
    String num;
    @JsonProperty("status")
    String status;
    @JsonProperty("statusRu")
    String statusRu;
    @JsonProperty("statusDate")
    String statusDate;
    @JsonProperty("register")
    String register;

    public String getNum() { return num; }
    public void setNum(String num) { this.num = num; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRegister() { return register; }
    public void setRegister(String register) { this.register = register; }

    public String getStatusDate() { return statusDate; }
    public void setStatusDate(String statusDate) { this.statusDate = statusDate; }

    public String getStatusRu() { return statusRu; }
    public void setStatusRu(String statusRu) { this.statusRu = statusRu; }
}

package app.uptate.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Shipment {

    public Shipment () {}
    @JsonProperty("awbStatus")
    private String awbStatus;
    @JsonProperty("awbStatusDescription")
    private String awbStatusDescription;

    @JsonProperty("orders")
    private List<Order> orders;

    public String getAwbStatus() { return awbStatus; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}

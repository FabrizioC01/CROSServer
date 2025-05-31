package Models;

import enums.MarketType;

import java.sql.Timestamp;

public class ClosedOrder{
    protected int orderId;
    private MarketType type;
    private String orderType; //market,stop or limit
    private Integer size;
    private Integer price;
    private long timestamp;
    public ClosedOrder(int orderId, MarketType type, String orderType, Integer size, Integer price, Timestamp timestamp, String user){
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp.getTime();
    }

    public long timestamp(){
        return timestamp;
    }

    public int getPrice(){
        return price;
    }
    public void setPrice(int price){
        this.price = price;
    }
    public int getFullSize(){
        return size;
    }

    public MarketType getType(){
        return type;
    }

    public int getOrderId() {
        return orderId;
    }



}

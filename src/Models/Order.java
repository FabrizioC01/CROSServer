package Models;

import enums.MarketType;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private MarketType type;
    private String orderType; //market,stop or limit
    private Integer size;
    private Integer price;
    private Timestamp timestamp;
    private Integer remaining;
    private String user;

    public Order(int orderId, MarketType type, String orderType, Integer size, Integer price, Timestamp timestamp,String user){
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
        this.remaining=size;
        this.user=user;
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
    public int getRemaining(){
        return remaining;
    }

    public MarketType getType(){
        return type;
    }

    public String getUser(){
        return user;
    }

    public void setRemaining(int v){
        this.remaining = v;
    }

    public void consume(int amount){
        this.remaining -= amount;
    }

    public int getOrderId() {
        return orderId;
    }

}

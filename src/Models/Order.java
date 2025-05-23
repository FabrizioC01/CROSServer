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
    private transient Integer remaining;

    public Order(int orderId, MarketType type, String orderType, Integer size, Integer price, Timestamp timestamp) {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
        this.remaining=size;
    }

    public int getPrice(){
        return price;
    }
    public int getFullSize(){
        return size;
    }
    public int getRemaining(){
        return remaining;
    }

    public void setRemaining(int v){
        this.remaining = v;
    }

    public void consume(int amount){
        this.size -= size;
    }

    public static Order nullOrder(){
        return new Order(-1,null,"",null,null,null);
    }
}

package Models;

import enums.MarketType;

import java.sql.Timestamp;


public class Order extends ClosedOrder{
    private Integer remaining;
    private String user;

    public Order(int orderId, MarketType type, String orderType, Integer size, Integer price, Timestamp timestamp,String user){
        super(orderId, type, orderType, size, price, timestamp, user);
        this.remaining=size;
        this.user=user;
    }

    public int getRemaining(){
        return remaining;
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


  public void setErrorID(){
        super.orderId = -1;
  }

}

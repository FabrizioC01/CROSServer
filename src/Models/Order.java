package Models;

import enums.MarketType;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private MarketType type;
    private String orderType; //market,stop or limit
    private int size;
    private int price;
    private Timestamp timestamp;

    public int getSize(){
        return size;
    }
}

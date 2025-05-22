package Models;

import enums.MarketType;

public class MarketValues {
    private MarketType type;
    private Integer size;
    private Integer price;
    private Integer orderId;
    private String month;

    public MarketType getType(){
        return type;
    }

    public int getSize(){
        return size;
    }
}

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
    public void decrease(int val){
        size -= val;
    }
    public int getPrice(){
        return price;
    }
    public void setsize(int val){
        size = val;
    }

    public int getSize(){
        return size;
    }
    public String getMonth(String month){
        return month;
    }
}

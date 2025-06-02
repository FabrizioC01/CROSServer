package Models;

import enums.MarketType;

/**
 * Classe (de)serializzazione campo values per operazioni nel book.
 */
public class MarketValues {
    private MarketType type;
    private Integer size;
    private Integer price;
    private Integer orderId;
    private String month;

    public MarketValues(MarketType type, Integer size, Integer price, Integer orderId, String month) {
        this.type = type;
        this.size = size;
        this.price = price;
        this.orderId = orderId;
        this.month = month;
    }

    public static MarketValues getFromOrder(Order o){
        return new MarketValues(o.getType(),o.getFullSize(),o.getPrice(),null,null);
    }

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
    public String getMonth(){
        return month;
    }
    public int getOrderId(){
        return orderId;
    }
}

package utils;

import Models.ClosedOrder;
import Models.Notification;
import Models.Order;
import com.google.gson.Gson;
import enums.ResponseCode;

import java.io.Serializable;
import java.util.ArrayList;

public class Serializer implements Serializable {
    private final Integer response;
    private final String errorMessage;
    private final Integer orderId;
    private final ArrayList<ClosedOrder> trades;

    /**
     * Used for operation responses
     * @param responseCode response code
     */
    public Serializer(ResponseCode responseCode) {
        this.response = responseCode.getCode();
        this.errorMessage = responseCode.getMessage();
        this.orderId = null;
        this.trades = null;
    }

    /**
     * Used for market ops responses
     * @param orderId order id, 0 otherwise
     */
    public Serializer(int orderId) {
        this.response = null;
        this.errorMessage = null;
        this.orderId = orderId;
        this.trades = null;
    }

    public Serializer(ArrayList<ClosedOrder> history) {
        this.response = null;
        this.errorMessage = null;
        this.orderId = null;
        this.trades = history;
    }

    public static String serializeClosedTrade(Order o){
        Gson gson = new Gson();
        Notification n = new Notification(o);
        return gson.toJson(n);
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}

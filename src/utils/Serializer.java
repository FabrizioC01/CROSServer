package utils;

import Models.Notification;
import Models.Order;
import com.google.gson.Gson;
import enums.ResponseCode;

import java.io.Serializable;

public class Serializer implements Serializable {
    private final Integer response;
    private final String errorMessage;
    private final Integer orderId;

    /**
     * Used for operation responses
     * @param responseCode response code
     */
    public Serializer(ResponseCode responseCode) {
        this.response = responseCode.getCode();
        this.errorMessage = responseCode.getMessage();
        this.orderId = null;
    }

    /**
     * Used for market ops responses
     * @param orderId order id, 0 otherwise
     */
    public Serializer(int orderId) {
        this.response = null;
        this.errorMessage = null;
        this.orderId = orderId;
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

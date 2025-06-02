package main.utils;

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
     * Costruttore per serializzare risposte con
     * i codici di errore {@code ResponseCode}.
     * @param responseCode codice di risposta
     */
    public Serializer(ResponseCode responseCode) {
        this.response = responseCode.getCode();
        this.errorMessage = responseCode.getMessage();
        this.orderId = null;
        this.trades = null;
    }

    /**
     * Costruttore per risposte con solo id degli ordini(market,limit,stop,)
     * @param orderId order id, 0 otherwise
     */
    public Serializer(int orderId) {
        this.response = null;
        this.errorMessage = null;
        this.orderId = orderId;
        this.trades = null;
    }

    /**
     * Serializza l'array con lo storico degli ordini.
     * @param history array con gli ordini da mandare al client
     */
    public Serializer(ArrayList<ClosedOrder> history) {
        this.response = null;
        this.errorMessage = null;
        this.orderId = null;
        this.trades = history;
    }

    /**
     * Metodo utilizzato da {@code NotificationService} per serializzare le notifiche da inviare
     * @param o ordine da serializzare per notifica
     * @return Notifica serializzata
     */
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

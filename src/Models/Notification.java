package Models;

import java.util.ArrayList;

/**
 * Formato delle notifiche
 */
public class Notification {
    private String notification;
    private ArrayList<Order> trades;


    public Notification(Order o) {
        this.notification = "closedTrades";
        this.trades = new ArrayList<>();
        this.trades.add(o);
    }
}

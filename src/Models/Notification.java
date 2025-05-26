package Models;

import java.util.ArrayList;

public class Notification {
    private String notification;
    private ArrayList<Order> trades;

    // for closed trades
    public Notification(Order o) {
        this.notification = "closedTrades";
        this.trades = new ArrayList<>();
        this.trades.add(o);
    }
}

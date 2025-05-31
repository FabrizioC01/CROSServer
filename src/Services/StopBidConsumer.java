package Services;

import Models.MarketValues;
import Models.Notification;
import Models.Order;
import utils.MarketManager;

import java.util.LinkedList;
import java.util.TreeMap;

public class StopBidConsumer  implements Runnable {
    private final TreeMap<Integer, LinkedList<Order>> stopBid;

    public StopBidConsumer(TreeMap<Integer, LinkedList<Order>> stopBid) {
        this.stopBid=stopBid;
    }

    @Override
    public void run() {
        while(true) {
            synchronized (stopBid) {
                while(stopBid.isEmpty()) {
                    try{
                        stopBid.wait();
                    }catch (InterruptedException e) {
                        return;
                    }
                }
                Integer val = MarketManager.getBestPrice(false);
                if(val == null) return;
                int firstValue = stopBid.firstKey();
                if( val<=firstValue){
                    Order o = stopBid.get(firstValue).poll();
                    if(o==null) continue;
                    MarketValues conv = MarketValues.getFromOrder(o);
                    int orderId = MarketManager.insertMarketOrder(conv,o.getUser(),"stop");
                    if(orderId==-1) NotificationService.notify(Order.failedStopOrder(o.getOrderId(),o.timestamp()));
                    else NotificationService.notify(o);
                }
            }
        }
    }

}

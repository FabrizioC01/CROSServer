package Services;

import Models.MarketValues;
import Models.Order;
import utils.MarketManager;

import java.util.LinkedList;
import java.util.TreeMap;

public class StopAskConsumer implements Runnable {
    private final TreeMap<Integer, LinkedList<Order>> stopAsk;

    public StopAskConsumer( TreeMap<Integer, LinkedList<Order>> stopAsk) {
        this.stopAsk=stopAsk;
    }

    @Override
    public void run() {
        while(true) {
            synchronized (stopAsk) {
                while(stopAsk.isEmpty()) {
                    try{
                        stopAsk.wait();
                    }catch (InterruptedException e) {
                        return;
                    }
                }
                Integer val = MarketManager.getBestPrice(false);
                if(val == null) return;
                int firstValue = stopAsk.firstKey();
                if( val>=firstValue){
                    Order o = stopAsk.get(firstValue).poll();
                    if(o==null) continue;
                    MarketValues conv = MarketValues.getFromOrder(o);
                    int orderId = MarketManager.insertMarketOrder(conv,o.getUser(),"stop");

                }
            }
        }
    }


}

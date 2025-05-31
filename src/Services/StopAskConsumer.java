package Services;

import Models.Order;

import java.util.Collections;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class StopConsumer implements Runnable {
    private final TreeMap<Integer, Queue<Order>> stopBid;
    private final TreeMap<Integer, Queue<Order>> stopAsk;
    private final ReentrantLock lock;

    public StopConsumer(TreeMap<Integer, Queue<Order>> stopBid, TreeMap<Integer, Queue<Order>> stopAsk,ReentrantLock lock) {
        this.stopAsk=stopAsk;
        this.stopBid=stopBid;
        this.lock=lock;
    }

    @Override
    public void run() {

    }

    public static void submitStop(){

    }

}

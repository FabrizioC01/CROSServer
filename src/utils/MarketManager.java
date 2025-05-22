package utils;

import Models.MarketValues;
import Models.Order;
import enums.MarketType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class MarketManager {
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> bidBook = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> askBook = new ConcurrentHashMap<>();

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ReentrantLock orderLock = new ReentrantLock();

    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> stopAskBook = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> stopBidBook = new ConcurrentHashMap<>();

    public int insertMarketOrder(MarketValues mv){
        ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> map = (mv.getType().equals(MarketType.ask))? bidBook:askBook;
        orderLock.lock();
        if(getBookSize(map)<mv.getSize()){
            orderLock.unlock();
            return -1;
        }
        return 0;
    }

    private int getBookSize(ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Order>> book){
        int sum=0;
        for(ConcurrentLinkedQueue<Order> q : book.values()){
            for(Order o : q){
                sum+=o.getSize();
            }
        }
        return sum;
    }

}


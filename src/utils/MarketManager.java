package utils;

import Models.MarketValues;
import Models.Order;
import enums.MarketType;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class MarketManager {
    private static final ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> bidBook = new ConcurrentSkipListMap<>();
    private static final ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> askBook = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ReentrantLock orderLock = new ReentrantLock();

    private static final ConcurrentLinkedQueue<Order> storico = new ConcurrentLinkedQueue<>();

    private static final ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> stopAskBook = new ConcurrentSkipListMap<>();
    private static final ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> stopBidBook = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    public static int insertMarketOrder(MarketValues mv){
        int mid=0;
        int n=0;
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = (mv.getType().equals(MarketType.ask))? bidBook:askBook;
        orderLock.lock();
        if(getBookSize(map)<mv.getSize()){
            orderLock.unlock();
            return -1;
        }
        int tot = mv.getSize();
        Iterator<Map.Entry<Integer, ConcurrentLinkedQueue<Order>>> it = map.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, ConcurrentLinkedQueue<Order>> entry = it.next();
            ConcurrentLinkedQueue<Order> orders = entry.getValue();
            Iterator<Order> orderIterator = orders.iterator();
            while (orderIterator.hasNext()){
                Order order = orderIterator.next();
                n++;
                mid+=order.getPrice();
                if(order.getRemaining()<=mv.getSize()){
                    mv.decrease(order.getRemaining());
                    orderIterator.remove();
                    if(orders.isEmpty()) it.remove();
                }else{
                    order.consume(mv.getSize());
                    mv.setsize(0);
                    if(orders.isEmpty()) it.remove();
                    break;
                }
            }
        }
        orderLock.unlock();
        int id = idCounter.incrementAndGet();
        storico.add(new Order(id,mv.getType(),"market",tot,mid/n,new Timestamp(System.currentTimeMillis())));
        return id;
    }

    public static int insertLimitOrder(MarketValues mv){
        ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> map = (mv.getType().equals(MarketType.ask))? bidBook:askBook;
        if(map.firstKey()<mv.getSize()){}
    }

    private static int getBookSize(ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<Order>> book){
        int sum=0;
        for(ConcurrentLinkedQueue<Order> q : book.values()){
            for(Order o : q){
                sum+=o.getRemaining();
            }
        }
        return sum;
    }

}


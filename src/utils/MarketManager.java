package utils;


import Models.MarketValues;
import Models.Order;
import enums.MarketType;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MarketManager {
    private static final TreeMap<Integer, Queue<Order>> bidBook = new TreeMap<>(Comparator.reverseOrder());
    private static final ReentrantLock bidLock = new ReentrantLock();

    private static final TreeMap<Integer, Queue<Order>> askBook = new TreeMap<>();
    private static final ReentrantLock askLock = new ReentrantLock();

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ReentrantLock historyLock=new ReentrantLock();
    private static final ArrayList<Order> history = new ArrayList<>();

    public static int insertLimitOrder(MarketValues request,String user){
        Order order = new Order(idCounter.incrementAndGet(),request.getType(),"limit",request.getSize(),request.getPrice(),new Timestamp(System.currentTimeMillis()),user);
        if(request.getType().equals(MarketType.ask)){
            bidLock.lock();
            Iterator<Map.Entry<Integer,Queue<Order>>> it = bidBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,Queue<Order>> entry = it.next();
                if(request.getPrice()<entry.getKey()) break;
                if (bookConsumer(order, it, entry, bidLock)) return order.getOrderId();
            }
            bidLock.unlock();
            askLock.lock();
            Queue<Order> list =askBook.get(order.getPrice());
            if(list==null) list = new LinkedList<>();
            list.add(order);
            askLock.unlock();
        }else {
            askLock.lock();
            Iterator<Map.Entry<Integer,Queue<Order>>> it = askBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,Queue<Order>> entry = it.next();
                if(request.getPrice()>entry.getKey()) break;
                if (bookConsumer(order, it, entry, askLock)) return order.getOrderId();
            }
            askLock.unlock();
            bidLock.lock();
            Queue<Order> list =bidBook.get(order.getPrice());
            if(list == null) list = new LinkedList<>();
            list.add(order);
            bidLock.unlock();

        }
        printBooks();
        return order.getOrderId();
    }

    private static boolean bookConsumer(Order order, Iterator<Map.Entry<Integer, Queue<Order>>> it, Map.Entry<Integer, Queue<Order>> entry, ReentrantLock askLock) {
        Iterator<Order> queue = entry.getValue().iterator();
        while(queue.hasNext()){
            Order val = queue.next();
            if(val.getRemaining()<=order.getRemaining()){ //ORDINI GRANDI DA SVUOTARE
                order.consume(val.getRemaining());
                historyLock.lock();
                history.add(order);
                historyLock.unlock();
                queue.remove();
            }else{ //ORDINI QUASI O COMPLETAMENTE VUOTI
                val.consume(order.getRemaining());
                order.setRemaining(0);
                historyLock.lock();
                history.add(order);
                historyLock.unlock();
                askLock.unlock();
                return true;
            }
        }
        if(entry.getValue().isEmpty()) it.remove();
        return false;
    }

    private static void printBooks(){
        bidLock.lock();
        System.out.println("BIDBOOK :");
        for(Queue<Order> q : bidBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+o.getPrice()+" x "+o.getRemaining());
            }
        }
        bidLock.unlock();
        askLock.lock();
        System.out.println("Askbook :");
        for(Queue<Order> q : askBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+o.getPrice()+" x "+o.getRemaining());
            }
        }
        askLock.unlock();
    }

}


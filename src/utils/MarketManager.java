package utils;


import Models.MarketValues;
import Models.Order;
import enums.MarketType;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MarketManager {
    private static final TreeMap<Integer, Queue<Order>> bidBook = new TreeMap<>();

    private static final TreeMap<Integer, Queue<Order>> askBook = new TreeMap<>(Collections.reverseOrder());

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ArrayList<Order> history = new ArrayList<>();

    public static synchronized int insertLimitOrder(MarketValues request,String user){
        Order order = new Order(idCounter.incrementAndGet(),request.getType(),"limit",request.getSize(),request.getPrice(),new Timestamp(System.currentTimeMillis()),user);
        if(request.getType().equals(MarketType.ask)){

            Iterator<Map.Entry<Integer,Queue<Order>>> it = bidBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,Queue<Order>> entry = it.next();
                if(request.getPrice()>entry.getKey()) break;
                if (bookConsumer(order, it, entry)) return order.getOrderId();
            }
            if (order.getRemaining()!=0){
                Queue<Order> list =askBook.get(order.getPrice());
                if(list==null) list = new LinkedList<>();
                askBook.put(order.getPrice(),list);
                list.add(order);
            }
        }else {
            Iterator<Map.Entry<Integer,Queue<Order>>> it = askBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,Queue<Order>> entry = it.next();
                if(request.getPrice()<entry.getKey()) break;
                if (bookConsumer(order, it, entry)) return order.getOrderId();
            }
            if(order.getRemaining()!=0){
                Queue<Order> list =bidBook.get(order.getPrice());
                if(list == null) list = new LinkedList<>();
                bidBook.put(order.getPrice(),list);
                list.add(order);
            }
        }
        return order.getOrderId();
    }

    private static synchronized boolean bookConsumer(Order order, Iterator<Map.Entry<Integer, Queue<Order>>> it, Map.Entry<Integer, Queue<Order>> entry) {
        Iterator<Order> queue = entry.getValue().iterator();
        while(queue.hasNext()){
            Order val = queue.next();
            if(val.getRemaining()<=order.getRemaining()){ //ORDINI GRANDI DA SVUOTARE
                order.consume(val.getRemaining());
                val.setRemaining(0);
                history.add(val);
                queue.remove();
            }else{ //ORDINI QUASI O COMPLETAMENTE VUOTI
                val.consume(order.getRemaining());
                order.setRemaining(0);
                history.add(order);
                return true;
            }
        }
        if(entry.getValue().isEmpty()) it.remove();
        return false;
    }

    public static synchronized void printBooks(){
        System.out.println("BIDBOOK :");
        for(Queue<Order> q : bidBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+o.getPrice()+"€"+" x "+o.getRemaining());
            }
        }
        System.out.println("Askbook :");
        for(Queue<Order> q : askBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+o.getPrice()+"€"+" x "+o.getRemaining());
            }
        }
        System.out.println("History :");
        for(Order o : history){
            System.out.println("["+o.getOrderId()+"] "+o.getPrice()+"€"+" x "+o.getFullSize());
        }
    }

}


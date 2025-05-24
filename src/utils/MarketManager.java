package utils;


import Models.MarketValues;
import Models.Order;
import Models.User;
import enums.MarketType;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MarketManager {
    private static final TreeMap<Integer, Queue<Order>> bidBook = new TreeMap<>(Collections.reverseOrder());

    private static final TreeMap<Integer, Queue<Order>> askBook = new TreeMap<>();

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ArrayList<Order> history = new ArrayList<>();

    public static synchronized int insertMarketOrder(MarketValues marketValues, String user) {
        int n=0;
        int sum=0;
        int size = marketValues.getSize();
        TreeMap<Integer, Queue<Order>> book = (marketValues.getType().equals(MarketType.ask))?bidBook:askBook;
        if(quantityCheck(book,marketValues.getSize())){
            Iterator<Map.Entry<Integer, Queue<Order>>> iterator = book.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<Integer, Queue<Order>> entry = iterator.next(); //key value pair
                Iterator<Order> orderIterator = entry.getValue().iterator();
                while(orderIterator.hasNext()){
                    Order order = orderIterator.next();
                    if(marketValues.getSize()==0) break;
                    if(marketValues.getSize()>=order.getRemaining()){
                        marketValues.decrease(order.getRemaining());
                        order.setRemaining(0);
                        history.add(order);
                        sum+=order.getPrice();
                        n++;
                        orderIterator.remove();
                    }else{
                        order.consume(marketValues.getSize());
                        marketValues.setsize(0);
                        history.add(new);
                    }
                }
                if(entry.getValue().isEmpty()) iterator.remove();
                if(marketValues.getSize()==0){
                    history.add(new Order(idCounter.incrementAndGet(),marketValues.getType(),"market",size,sum/n,new Timestamp(System.currentTimeMillis()),user));
                }
            }
        }return -1;
    }

    private static synchronized boolean quantityCheck(TreeMap<Integer, Queue<Order>> book,int qty) {
        for(Queue<Order> q : book.values()) {
            for(Order o : q) {
                qty-=o.getRemaining();
                if(qty<=0) return true;
            }
        }
        return false;
    }

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
                val.setPrice(order.getPrice());
                history.add(val);
                queue.remove();
            }else{ //ORDINI QUASI O COMPLETAMENTE VUOTI
                val.consume(order.getRemaining());
                order.setRemaining(0);
                order.setPrice(val.getPrice());
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


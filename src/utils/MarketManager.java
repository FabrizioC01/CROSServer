package utils;


import Models.MarketValues;
import Models.Notification;
import Models.Order;
import Services.NotificationService;
import enums.MarketType;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
                Map.Entry<Integer, Queue<Order>> entry = iterator.next(); // key-value
                Iterator<Order> orderIterator = entry.getValue().iterator();
                while(orderIterator.hasNext()){
                    Order order = orderIterator.next();
                    if(order.getRemaining()<=marketValues.getSize()){
                        marketValues.decrease(order.getRemaining());
                        history.add(order);
                        NotificationService.notify(order);
                        order.setRemaining(0);
                        n++;
                        sum+=order.getPrice();
                        orderIterator.remove();
                    }else{
                        if(marketValues.getSize()!=0){
                            n++;
                            sum += order.getPrice();
                            order.consume(marketValues.getSize());
                            marketValues.setsize(0);
                        }
                        int id = idCounter.incrementAndGet();
                        Order ord = new Order(id,marketValues.getType(),"market",size,sum/n,new Timestamp(System.currentTimeMillis()),user);
                        history.add(ord);
                        return id;
                    }
                }
                if(entry.getValue().isEmpty()) iterator.remove();
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
                NotificationService.notify(val);
                queue.remove();
            }else{ //ORDINI QUASI O COMPLETAMENTE VUOTI
                val.consume(order.getRemaining());
                order.setRemaining(0);
                order.setPrice(val.getPrice());
                history.add(order);
                NotificationService.notify(order);
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


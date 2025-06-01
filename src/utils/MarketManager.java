package utils;


import Models.ClosedOrder;
import Models.MarketValues;
import Models.Notification;
import Models.Order;
import Services.NotificationService;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import enums.MarketType;

import java.io.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketManager {
    private final String bFilename;
    private final String hFilename;

    private final TreeMap<Integer, LinkedList<Order>> bidBook = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Integer, LinkedList<Order>> askBook = new TreeMap<>();

    private final TreeMap<Integer, LinkedList<Order>> stopBid = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Integer, LinkedList<Order>> stopAsk = new TreeMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(0);

    private final ArrayList<ClosedOrder> history = new ArrayList<>();

    public MarketManager(String historyFile, String bookFile){
        bFilename = bookFile;
        hFilename = historyFile;
        loadBook(bookFile);
        loadHistory(historyFile);
    }

    private void loadHistory(String filename) {
        File file = new File(filename);
        try{
            if(file.createNewFile()) {
                System.out.println("[Market] History file created");
                return;
            }
            else System.out.println("[Market] Using an existing history file");
        } catch (IOException e) {
            System.out.println("[Market] Error creating file " + filename);
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        try(FileReader f = new FileReader(filename)){
            JsonElement je = JsonParser.parseReader(f);
            if(je.isJsonNull()){
                System.out.println("[Market] history file format error");
                return;
            }
            JsonObject json = je.getAsJsonObject();
            if(json.get("trades")==null){
                System.out.println("[Market] history file format error");
            }else{
                ArrayList<Order> orders = gson.fromJson(json.getAsJsonArray("trades"),new TypeToken<ArrayList<Order>>(){}.getType());
                int max_id=-1;
                for(Order order : orders){
                    if(order.getOrderId() == -1 ){
                        System.out.println("[Market] history invalid order id");
                        return;
                    }
                    if (max_id<order.getOrderId()) max_id=order.getOrderId();
                }
                if(max_id<0) max_id=idCounter.get();
                System.out.println("[Market] history loaded last id: "+max_id);
                history.addAll(orders);
                idCounter.set(max_id);
            }

        }catch(JsonIOException ex){
            System.out.println("[Market] history file format error");
        }catch (IOException e){
            System.out.println("[Market] Error loading history file");
            history.clear();
        }catch (JsonSyntaxException e){
            System.out.println("[Market] Error parsing history file");
            history.clear();
        }
    }

    private void loadBook(String filename) {
        File file = new File(filename);
        try{
            if(file.createNewFile()) {
                System.out.println("[Market] Book file created");
                return;
            }
            else System.out.println("[Market] Using an existing book file");
        } catch (IOException e) {
            System.out.println("[Market] Error creating file " + filename);
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        try(FileReader fr = new FileReader(filename)){
            JsonElement jo = JsonParser.parseReader(fr);
            if(jo.isJsonNull()) return;
            JsonObject json = jo.getAsJsonObject();
            JsonObject ask = json.getAsJsonObject("ask");
            JsonObject bid = json.getAsJsonObject("bid");
            JsonObject sAsk = json.getAsJsonObject("stopAsk");
            JsonObject sBid = json.getAsJsonObject("stopBid");
            JsonElement id = json.get("lastID");
            if(id==null) {
                System.out.println("[Market] id not found, ignoring persistent book file...");
                return;
            }
            if(ask!=null) askBook.putAll(gson.fromJson(ask, new TypeToken<TreeMap<Integer, LinkedList<Order>>>(){}.getType()));
            if(bid!=null) bidBook.putAll(gson.fromJson(bid, new TypeToken<TreeMap<Integer, LinkedList<Order>>>(){}.getType()));
            if(sAsk!=null) stopAsk.putAll(gson.fromJson(sAsk, new TypeToken<TreeMap<Integer, LinkedList<Order>>>(){}.getType()));
            if(sBid!=null) stopBid.putAll(gson.fromJson(sBid, new TypeToken<TreeMap<Integer, LinkedList<Order>>>(){}.getType()));
            int lastVal= gson.fromJson(id,Integer.class);
            idCounter.set(lastVal);
            System.out.println("[Market] Loaded persistent book start from id "+lastVal);
        }catch (FileNotFoundException f){
          System.out.println("[Market] Persistent book file not found");
          throw new RuntimeException();
        } catch (IOException e){
            System.out.println("[Market] Error loading persistent book file");
        }
    }


    public synchronized int insertMarketOrder(MarketValues marketValues, String user,String Type) {
        int n=0;
        int sum=0;
        int size = marketValues.getSize();
        TreeMap<Integer, LinkedList<Order>> book = (marketValues.getType().equals(MarketType.ask))?bidBook:askBook;
        if(quantityCheck(book,marketValues.getSize())){
            Iterator<Map.Entry<Integer, LinkedList<Order>>> iterator = book.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<Integer, LinkedList<Order>> entry = iterator.next(); // key-value
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
                        Order ord = new Order(id,marketValues.getType(),Type,size,sum/n,new Timestamp(System.currentTimeMillis()),user);
                        history.add(ord);
                        return id;
                    }
                }
                if(entry.getValue().isEmpty()) iterator.remove();
            }
        }return -1;
    }

    public synchronized int insertStopOrder(MarketValues mv,String user) {
        int id =idCounter.incrementAndGet();
        Order o = new Order(id,mv.getType(),"stop",mv.getSize(),mv.getPrice(),new Timestamp(System.currentTimeMillis()),user);
        TreeMap<Integer, LinkedList<Order>> book =(o.getType().equals(MarketType.ask))?bidBook:askBook;
        if(o.getType().equals(MarketType.ask)){
            if(!book.isEmpty() && book.firstKey()>=o.getPrice()) {
                int v =insertMarketOrder(mv,user,"stop");
                if(v==-1)o.setErrorID();
                NotificationService.notify(o);
            } else{
                stopAsk.computeIfPresent(o.getPrice(),(K,V)->{
                    V.add(o);
                    return V;
                });
                stopAsk.computeIfAbsent(o.getPrice(),(K)->{
                    LinkedList<Order> orders = new LinkedList<>();
                    orders.add(o);
                    return orders;
                });
            }
        }else{
            if(!book.isEmpty() && book.firstKey()<=o.getPrice()) {
                int v =insertMarketOrder(mv,user,"stop");
                if(v==-1)o.setErrorID();
                NotificationService.notify(o);
            }
            else{
                stopBid.computeIfPresent(o.getPrice(),(K,V)->{
                    V.add(o);
                    return V;
                });
                stopBid.computeIfAbsent(o.getPrice(),(K)->{
                    LinkedList<Order> orders = new LinkedList<>();
                    orders.add(o);
                    return orders;
                });
            }
        }
        return id;
    }

    private synchronized void checkStops(int price,MarketType type){
        if(type.equals(MarketType.ask)){
            if(!stopBid.isEmpty() && price<=stopBid.firstKey()){
                Iterator<Map.Entry<Integer, LinkedList<Order>>> iterator = stopBid.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<Integer, LinkedList<Order>> entry = iterator.next();
                    Iterator<Order> orderIterator = entry.getValue().iterator();
                    while(orderIterator.hasNext()){
                        Order order = orderIterator.next();
                        if(price>order.getPrice()) return;
                        int id =insertMarketOrder(MarketValues.getFromOrder(order),order.getUser(),"stop");

                        if(id==-1) order.setErrorID();

                        NotificationService.notify(order);

                        orderIterator.remove();
                    }
                    if(entry.getValue().isEmpty()) iterator.remove();
                }
            }
        }else{
            if(!stopAsk.isEmpty() && price>=stopAsk.firstKey()){
                Iterator<Map.Entry<Integer, LinkedList<Order>>> iterator = stopAsk.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<Integer, LinkedList<Order>> entry = iterator.next();
                    Iterator<Order> orderIterator = entry.getValue().iterator();
                    while(orderIterator.hasNext()){
                        Order order = orderIterator.next();
                        if(price<order.getPrice()) return;
                        int id =insertMarketOrder(MarketValues.getFromOrder(order),order.getUser(),"stop");

                        if(id==-1) order.setErrorID();

                        NotificationService.notify(order);

                        orderIterator.remove();
                    }
                    if(entry.getValue().isEmpty()) iterator.remove();
                }
            }
        }
    }

    private synchronized boolean quantityCheck(TreeMap<Integer, LinkedList<Order>> book,int qty) {
        for(LinkedList<Order> q : book.values()) {
            for(Order o : q) {
                qty-=o.getRemaining();
                if(qty<=0) return true;
            }
        }
        return false;
    }

    public synchronized int insertLimitOrder(MarketValues request,String user){
        Order order = new Order(idCounter.incrementAndGet(),request.getType(),"limit",request.getSize(),request.getPrice(),new Timestamp(System.currentTimeMillis()),user);
        if(request.getType().equals(MarketType.ask)){

            Iterator<Map.Entry<Integer,LinkedList<Order>>> it = bidBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,LinkedList<Order>> entry = it.next();
                if(request.getPrice()>entry.getKey()) break;
                if (bookConsumer(order, it, entry)) return order.getOrderId();
            }
            if (order.getRemaining()!=0){
                LinkedList<Order> list =askBook.get(order.getPrice());
                if(list==null) list = new LinkedList<>();
                askBook.put(order.getPrice(),list);
                list.add(order);
                checkStops(request.getPrice(),request.getType());
            }
        }else {
            Iterator<Map.Entry<Integer,LinkedList<Order>>> it = askBook.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer,LinkedList<Order>> entry = it.next();
                if(request.getPrice()<entry.getKey()) break;
                if (bookConsumer(order, it, entry)) return order.getOrderId();
            }
            if(order.getRemaining()!=0){
                LinkedList<Order> list =bidBook.get(order.getPrice());
                if(list == null) list = new LinkedList<>();
                bidBook.put(order.getPrice(),list);
                list.add(order);
                checkStops(request.getPrice(),request.getType());
            }
        }
        return order.getOrderId();
    }


    private synchronized boolean bookConsumer(Order order, Iterator<Map.Entry<Integer, LinkedList<Order>>> it, Map.Entry<Integer, LinkedList<Order>> entry) {
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

    public synchronized ArrayList<ClosedOrder> getMonthHistory(String dat){
        ArrayList<ClosedOrder> orders = new ArrayList<>();
        DateTimeFormatter form = DateTimeFormatter.ofPattern("MMyyyy");
        YearMonth yearMonth = YearMonth.parse(dat,form);

        ZonedDateTime start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());
        System.out.println("Request DB for "+dat);

        long startTimestamp = start.toInstant().toEpochMilli();
        long endTimestamp = end.toInstant().toEpochMilli();

        for(ClosedOrder o: history){
            if(o.timestamp()>= startTimestamp && o.timestamp()<=endTimestamp){
                orders.add(new ClosedOrder(o.getOrderId(),o.getType(),o.getOrderType(),o.getFullSize(),o.getPrice(),new Timestamp(o.timestamp())));
            }

        }
        return orders;
    }

    public synchronized boolean cancelOrder(int id,String user){
        return checkAndDelete(askBook,id,user) || checkAndDelete(bidBook,id,user) || checkAndDelete(stopAsk,id,user) || checkAndDelete(stopBid,id,user);
    }

    private boolean checkAndDelete(TreeMap<Integer, LinkedList<Order>> book, int id,String user){
        boolean ret_val=false;
        Iterator<Map.Entry<Integer,LinkedList<Order>>> bid = book.entrySet().iterator();
        while(bid.hasNext()){
            Map.Entry<Integer,LinkedList<Order>> entry = bid.next();
            Iterator<Order> queue = entry.getValue().iterator();
            while(queue.hasNext()){
                Order val = queue.next();
                if(val.getOrderId()==id && val.getUser().equals(user)){
                    ret_val=true;
                    queue.remove();
                    break;
                }
            }
            if(entry.getValue().isEmpty()) bid.remove();
            if(ret_val) return ret_val;
        }
        return ret_val;
    }

    public synchronized void printBooks(){
        System.out.println("BIDBOOK (price x size): [");
        for(LinkedList<Order> q : bidBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
            }
        }
        System.out.println("]");
        System.out.println("\nASKBOOK (price x size): [");
        for(LinkedList<Order> q : askBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
            }
        }
        System.out.println("]");
        if(!askBook.isEmpty() && !bidBook.isEmpty()){
            Order ask = askBook.firstEntry().getValue().peek();
            Order bid = bidBook.firstEntry().getValue().peek();
            if(ask!=null && bid!=null) System.out.println("\nSpread: "+(float)Math.abs(ask.getPrice()-bid.getPrice())/1000);
        }
    }

    public synchronized void saveAll(){
        if(hFilename!=null && bFilename!=null){
            saveHistory(hFilename);
            saveBook(bFilename);
        }
    }

    private synchronized void saveBook(String file){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if(askBook.isEmpty() && bidBook.isEmpty() && stopAsk.isEmpty() && stopBid.isEmpty()) return;
        try {
            File f = new File(file);
            if(f.createNewFile()){
                System.out.println("[Market] Creating new book file...");
            }else System.out.println("[Market] Overwriting book file...");
            FileWriter fw = new FileWriter(f);
            JsonObject jo = new JsonObject();
            JsonElement ask = gson.toJsonTree(askBook);
            jo.add("ask",ask);
            JsonElement bid = gson.toJsonTree(bidBook);
            jo.add("bid",bid);
            JsonElement sAsk = gson.toJsonTree(stopAsk);
            jo.add("stopAsk",sAsk);
            JsonElement sBid = gson.toJsonTree(stopBid);
            jo.add("stopBid",sBid);
            JsonElement last = gson.toJsonTree(idCounter.get());
            jo.add("lastID",last);
            gson.toJson(jo,fw);
            fw.close();
        }catch (IOException e){
            System.out.println("[Market] Error saving book file");
        }
    }


    private synchronized void saveHistory(String file){
        File f = new File(file);
        Gson gson = new Gson();
        if(history.isEmpty()) return;
        try{
            if(f.createNewFile()) System.out.println("[Market] Creating new history file...");
            else System.out.println("[Market] Overwriting history file...");
            FileWriter fw = new FileWriter(f);
            JsonObject jo = new JsonObject();
            ArrayList<ClosedOrder> copy = new ArrayList<>();
            for(ClosedOrder o : history){
                copy.add(new ClosedOrder(o.getOrderId(),o.getType(),o.getOrderType(),o.getFullSize(),o.getPrice(),new Timestamp(System.currentTimeMillis())));
            }
            JsonElement h = gson.toJsonTree(copy);
            jo.add("trades",h);
            gson.toJson(jo,fw);
            fw.close();
        }catch (IOException ex){
            System.out.println("[Market] Error saving history file");
        }
    }

    public void printStop(){
        System.out.println("Sell Stops:");
        synchronized (stopAsk){
            stopAsk.forEach((K,V)->{
                for (Order o: V){
                    System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
                }
            });
        }
        System.out.println("Buy Stops:");
        synchronized (stopBid){
            stopBid.forEach((K,V)->{
                for (Order o: V){
                    System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
                }
            });
        }
    }


}


package utils;


import Models.MarketValues;
import Models.Order;
import Services.NotificationService;
import Services.StopAskConsumer;
import Services.StopBidConsumer;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import enums.MarketType;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MarketManager {
    private static String bFilename,hFilename;

    private static final TreeMap<Integer, LinkedList<Order>> bidBook = new TreeMap<>(Collections.reverseOrder());
    private static final TreeMap<Integer, LinkedList<Order>> askBook = new TreeMap<>();

    private static final TreeMap<Integer, LinkedList<Order>> stopBid = new TreeMap<>(Collections.reverseOrder());
    private static final TreeMap<Integer, LinkedList<Order>> stopAsk = new TreeMap<>();

    private static Thread stopAskThread = null;
    private static Thread stopBidThread = null;

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private static final ArrayList<Order> history = new ArrayList<>();

    public static void init(String historyFile, String bookFile){
        loadHistory(historyFile);
        loadBook(bookFile);
        hFilename=historyFile;
        bFilename=bookFile;
        stopAskThread= new Thread(new StopAskConsumer(stopAsk));
        stopBidThread= new Thread(new StopBidConsumer(stopBid));
    }

    private static void loadHistory(String filename) {
        if (filename==null) return;
        Gson gson = new Gson();
        try(FileReader f = new FileReader(filename)){
            JsonObject json = JsonParser.parseReader(f).getAsJsonObject();
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

        }catch (IOException e){
            System.out.println("[Market] Error loading history file");
            history.clear();
        }catch (JsonSyntaxException e){
            System.out.println("[Market] Error parsing history file");
            history.clear();
        }
    }

    private static void loadBook(String filename) {
        if (filename==null) return;
        Gson gson = new Gson();
        try(FileReader fr = new FileReader(filename)){
            JsonObject json = JsonParser.parseReader(fr).getAsJsonObject();
            JsonObject ask = json.getAsJsonObject("ask");
            JsonObject bid = json.getAsJsonObject("bid");
            JsonObject sAsk = json.getAsJsonObject("stopAsk");
            JsonObject sBid = json.getAsJsonObject("stopBid");
            JsonElement id = json.get("lastID");
            if(id==null) {
                System.out.println("[Market] id not found, ignoring persistent book file...");
                return;
            }
            if(ask!=null) askBook.putAll(gson.fromJson(ask, new TypeToken<TreeMap<Integer, Queue<Order>>>(){}.getType()));
            if(bid!=null) bidBook.putAll(gson.fromJson(bid, new TypeToken<TreeMap<Integer, Queue<Order>>>(){}.getType()));
            if(sAsk!=null) stopAsk.putAll(gson.fromJson(sAsk, new TypeToken<TreeMap<Integer, Queue<Order>>>(){}.getType()));
            if(sBid!=null) stopBid.putAll(gson.fromJson(sBid, new TypeToken<TreeMap<Integer, Queue<Order>>>(){}.getType()));
            int lastVal= gson.fromJson(id,Integer.class);
            idCounter.set(lastVal);
            System.out.println("[Market] Loaded persistent book start from id "+lastVal);
        }catch (FileNotFoundException f){
          System.out.println("[Market] Persistent book file not found");
        } catch (IOException e){
            System.out.println("[Market] Error loading persistent book file");
        }
    }


    public static synchronized int insertMarketOrder(MarketValues marketValues, String user,String Type) {
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

    public static synchronized Integer getBestPrice(boolean fromAsk){
        Integer val;
        try{
            if(fromAsk) val = askBook.firstKey();
            else val = askBook.lastKey();
        }catch (NoSuchElementException el ){
            return null;
        }
        return val;
    }

    public static int insertStopOrder(MarketValues mv,String user) {
        int id =idCounter.incrementAndGet();
        Order o = new Order(id,mv.getType(),"stop",mv.getSize(),mv.getPrice(),new Timestamp(System.currentTimeMillis()),user);

        if(mv.getType().equals(MarketType.ask)){
            synchronized (stopAsk){
                LinkedList<Order> list =stopAsk.get(mv.getPrice());
                if(list ==null) list = new LinkedList<>();
                list.add(o);
                stopAsk.notify();
            }
        }else{
            synchronized (stopBid){
                LinkedList<Order> list =stopBid.get(mv.getPrice());
                if(list ==null) list = new LinkedList<>();
                list.add(o);
                stopBid.notify();
            }
        }
        return id;
    }

    private static synchronized boolean quantityCheck(TreeMap<Integer, LinkedList<Order>> book,int qty) {
        for(LinkedList<Order> q : book.values()) {
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
            }
            synchronized (stopBid){
                stopBid.notify();
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
            }
            synchronized (stopBid){
                stopBid.notify();
            }
        }
        return order.getOrderId();
    }


    private static synchronized boolean bookConsumer(Order order, Iterator<Map.Entry<Integer, LinkedList<Order>>> it, Map.Entry<Integer, LinkedList<Order>> entry) {
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

    public static synchronized void getMonthHistory(String dat){
        ArrayList<Order> orders = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        String month = dat.substring(0,1);
        String year = dat.substring(2,5);
        for(Order o: history){
            Date d = new Date(o.timestamp());


        }
    }

    public static synchronized void printBooks(){
        System.out.println("BIDBOOK (price x size): [");
        for(Queue<Order> q : bidBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
            }
        }
        System.out.println("]");
        System.out.println("\nASKBOOK (price x size): [");
        for(Queue<Order> q : askBook.values()){
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

    public static synchronized void saveAll(){
        if(hFilename!=null && bFilename!=null){
            saveHistory(hFilename);
            saveBook(bFilename);
        }
    }

    private static synchronized void saveBook(String file){
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


    private static synchronized void saveHistory(String file){
        File f = new File(file);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if(history.isEmpty()) return;
        try{
            if(f.createNewFile()) System.out.println("[Market] Creating new history file...");
            else System.out.println("[Market] Overwriting history file...");
            FileWriter fw = new FileWriter(f);
            JsonObject jo = new JsonObject();
            JsonElement h = gson.toJsonTree(history);
            jo.add("trades",h);
            gson.toJson(jo,fw);
            fw.close();
        }catch (IOException ex){
            System.out.println("[Market] Error saving history file");
        }
    }


}


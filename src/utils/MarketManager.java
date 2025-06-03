package utils;


import Models.ClosedOrder;
import Models.MarketValues;
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
    //Nomi dei file dello storico e del book
    private final String bFilename;
    private final String hFilename;

    //book
    private final TreeMap<Integer, LinkedList<Order>> bidBook = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Integer, LinkedList<Order>> askBook = new TreeMap<>();

    //book che gestiscono gli stop
    private final TreeMap<Integer, LinkedList<Order>> stopBid = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Integer, LinkedList<Order>> stopAsk = new TreeMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(0);

    private final ArrayList<ClosedOrder> history = new ArrayList<>();

    public MarketManager(String historyFile, String bookFile){
        bFilename = bookFile;
        hFilename = historyFile;
        loadHistory();
        loadBook();
    }

    /**
     * Carica lo storico in memoria e imposta il counter degli id
     * al massimo trovato nella lista (in caso non ci siano ordini nel book)
     */
    private void loadHistory() {
        File file = new File(hFilename);
        try{
            if(file.createNewFile()) {
                System.out.println("[Market] History file created");
                return;
            }
            else System.out.println("[Market] Using an existing history file");
        } catch (IOException e) {
            System.out.println("[Market] Error creating file " + hFilename);
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        try(FileReader f = new FileReader(hFilename)){
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

    /**
     * Carica il book e imposta il counter degli ordini
     */
    private void loadBook() {
        File file = new File(bFilename);
        try{
            if(file.createNewFile()) {
                System.out.println("[Market] Book file created");
                return;
            }
            else System.out.println("[Market] Using an existing book file");
        } catch (IOException e) {
            System.out.println("[Market] Error creating file " + bFilename);
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        try(FileReader fr = new FileReader(bFilename)){
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

    /**
     * Effettua un ordine chiamando prima {@code quantityCheck} verificando prima
     * la disponibilità per poter evadere correttamente l'ordine e in tal caso lo esegue.
     * @param marketValues Campo values ricevuto dal client
     * @param user nome utente che richiede l'operazione
     * @param Type tipo di operazione (può essere uno stop trasformato)
     * @return id dell'ordine (-1 in caso di errore)
     */
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

    /**
     * Metodo chiamato solamente da {@code insertMarketOrder} per verificare la disponibilità.
     * @param book book dalla quale si vuole effettuare l'operazione
     * @param qty quantità necessaria
     * @return true se l'operazione è eseguibile
     */
    private boolean quantityCheck(TreeMap<Integer, LinkedList<Order>> book,int qty) {
        for(LinkedList<Order> q : book.values()) {
            for(Order o : q) {
                qty-=o.getRemaining();
                if(qty<=0) return true;
            }
        }
        return false;
    }

    /**
     * Crea lo stop order e verifica il prezzo di mercato, se favorevole viene attivato subito
     * altrimenti viene inserito nelle stutture degli ordini stop.
     * @param mv richiesta ricevuta dall'utente
     * @param user utente richiedente
     * @return id dell'ordine
     */
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

    /**
     * Verifica se ci sono ordini compatibili nell'altro book, in caso
     * l'ordine non venga evaso completamente viene messo nel relativo book.
     * @param request richiesta dell'utente
     * @param user username
     * @return id dell'ordine
     */
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

    /**
     * Metodo chiamato quando viene inserito un limit a un certo prezzo,
     * si verifica se ci sono ordini stop da attivare.
     * @param price prezzo appena inserito
     * @param type ask / bid
     */
    private void checkStops(int price,MarketType type){
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

    /**
     * Metodo chiamato da {@code insertLimitOrder} per "cosumare" gli ordini dai book
     * restituisce true o false in caso sia stato completamente evaso o meno.
     * @param order ordine limit
     * @param it iteratore della mappa(utile per rimuovere il record in caso sia vuoto)
     * @param entry iteratore della lista FIFO degli ordini dello stesso prezzo
     * @return true se l'ordine è completamente evaso (rimanente 0)
     */
    private boolean bookConsumer(Order order, Iterator<Map.Entry<Integer, LinkedList<Order>>> it, Map.Entry<Integer, LinkedList<Order>> entry) {
        Iterator<Order> queue = entry.getValue().iterator();
        while(queue.hasNext()){
            Order val = queue.next();
            if(val.getRemaining()<=order.getRemaining()){
                order.consume(val.getRemaining());
                val.setRemaining(0);
                val.setPrice(order.getPrice());
                history.add(val);
                NotificationService.notify(val);
                if(order.getRemaining()==0){
                    NotificationService.notify(order);
                    return true;
                }
                queue.remove();
            }else{
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

    /**
     * Metodo che filtra l'array dello storico e restituisce gli ordini del mese
     * richiesto
     * @param dat mese in formato MMyyyy
     * @return lista degli ordini in quel mese
     */
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

    /**
     * Elimina l'ordine {@code id}(se possibile) appartenente all'utente {@code user}
     * se è nel book o nelle strutture degli stop.
     * @param id id ordine da eliminare
     * @param user utente che richiede l'eliminazione
     * @return true se viene eliminato correttamente, false altrimenti
     */
    public synchronized boolean cancelOrder(int id,String user){
        return checkAndDelete(askBook,id,user) || checkAndDelete(bidBook,id,user) || checkAndDelete(stopAsk,id,user) || checkAndDelete(stopBid,id,user);
    }

    /**
     * Metodo privato chiamato da {@code cancelOrder} che elimina dai book
     * o dalle strutture degli stop gli ordini e restituisce true se lo elimina.
     * @param book struttura / book
     * @param id id dell'ordine
     * @param user utente che richiede l'eliminazione
     * @return true se viene eliminato
     */
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

    /**
     * Stampa i book e lo spread
     */
    public synchronized void printBooks(){
        System.out.println("BIDBOOK (price x size): [");
        for(LinkedList<Order> q : bidBook.values()){
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
        System.out.println("\nASKBOOK (price x size): [");
        for(LinkedList<Order> q : askBook.values()){
            for(Order o : q){
                System.out.println("["+o.getOrderId()+"] "+(float)o.getPrice()/1000+"$ x "+(float)o.getRemaining()/1000+"BTC");
            }
        }
        System.out.println("]");

    }

    /**
     * Chiama i metodi privati per il salvataggio delle strutture e il
     * salvataggio dello storico
     */
    public synchronized void saveAll(){
        if(hFilename!=null && bFilename!=null){
            saveHistory();
            saveBook();
        }
    }

    /**
     * Salva i book e le strutture per gli stop, e l'id
     * dell'ultimo ordine creato
     */
    private synchronized void saveBook(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if(askBook.isEmpty() && bidBook.isEmpty() && stopAsk.isEmpty() && stopBid.isEmpty()) return;
        try {
            File f = new File(bFilename);
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

    /**
     * Salva lo storico precedentemente letto
     * con quello disponibile alla chiusura del server
     */
    private synchronized void saveHistory(){
        File f = new File(hFilename);
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

    /**
     * Stampa gli stop in attesa di attivazione
     */
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

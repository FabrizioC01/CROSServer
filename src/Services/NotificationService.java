package main.Services;

import Models.Order;
import utils.Serializer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationService implements Runnable{
    private static final ConcurrentHashMap<String,Address> usersIPs = new ConcurrentHashMap<>();
    private static final BlockingQueue<Order> notifications = new LinkedBlockingQueue<>();


    /**
     * Thread che attende notifiche sulla blocking queue,
     * ne legge l'utente, lo cerca nella mappa di utenti-indirizzi
     * e se possibile manda la notifica altrimenti la scarta
     */
    @Override
    public void run() {
        while(true){
            try {
                DatagramSocket ds = new DatagramSocket();
                Order o = notifications.take();
                Address addr = usersIPs.get(o.getUser());
                if(addr == null)continue;
                byte[] buffer = Serializer.serializeClosedTrade(o).getBytes();
                DatagramPacket packet = new DatagramPacket(buffer,buffer.length, InetAddress.getByName(addr.getIp()),addr.getPort());
                ds.send(packet);
            } catch (InterruptedException e) {
                System.out.println("[NotificationService] Notification service stopped");
                return;
            }catch (UnknownHostException ignored){
            } catch (SocketException conn){
                System.out.println("[NotificationService] Notification service socket exception");
                return;
            }catch (IOException io){
                System.out.println("[NotificationService] Notification service i/o error");
            }

        }
    }

    /**
     * Funzione che aggiunge l'ordine alla coda bloccante le notifiche.
     * @param order ordine concluso da notificare
     */
    public static void notify(Order order){
        try{
            notifications.put(order);
        } catch (InterruptedException e) {
            System.out.println("[Notifications] Notification interrupted");
        }
    }

    /**
     *  Funzione chiamata al login dell'utente che permette di registrarsi
     *  al servizio di notifiche.
     * @param user username dell'utente
     * @param ip indirizzo ip(stringa)
     * @param port porta udp passata dall'utente (login)
     */
    public static void register(String user, String ip, int port){
        usersIPs.put(user,new Address(ip,port));
        System.out.println("[Notifications] added "+user+" with address "+ip+":"+port);
    }

    /**
     * Funzione che rimuove l'utente e l'indirizzo dalla mappa,
     * ignorando le notifiche da mandare.
     * @param user username dell'utente da rimuovere
     */
    public static void unRegister(String user){
        usersIPs.remove(user);
    }

}

/**
 * Classe per la memorizzazione delle coppie ip-porta degli utenti
 */
class Address{
    private final String ip;
    private final int port;

    public Address(String ip, int port){
        this.ip = ip;
        this.port = port;
    }
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }
}

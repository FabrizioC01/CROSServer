package Services;

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
    private static boolean running = true;

    @Override
    public void run() {
        while(running){
            try {
                DatagramSocket ds = new DatagramSocket();
                Order o = notifications.take();
                Address addr = usersIPs.get(o.getUser());
                if(addr == null)continue;
                byte[] buffer = Serializer.serializeClosedTrade(o).getBytes();
                DatagramPacket packet = new DatagramPacket(buffer,buffer.length, InetAddress.getByName(addr.getIp()),addr.getPort());
                ds.send(packet);
            } catch (InterruptedException e) {
                System.out.println("[NotificationService] Notification service interrupted");
                return;
            }catch (UnknownHostException ignored){

            } catch (SocketException conn){
                System.out.println("[NotificationService] Notification service socket exception");
                return;
            }catch (IOException io){
                System.out.println("[NotificationService] Notification service i/o error");
            }

        }
        System.out.println("[NotificationService] Notification service stopped");
    }

    public static void notify(Order order){
        try{
            notifications.put(order);
        } catch (InterruptedException e) {
            System.out.println("Notification function interrupted");
        }
    }

    public static void register(String user, String ip, int port){
        usersIPs.put(user,new Address(ip,port));
        System.out.println("[Notifications] added "+user+" with address "+ip+":"+port);
    }
    public static void unRegister(String user){
        usersIPs.remove(user);
    }

    public static void stop(){
        running = false;
    }

}

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

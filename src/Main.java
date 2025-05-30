import Services.ConnectionService;
import Services.Console;
import Services.NotificationService;
import utils.AuthManager;
import utils.MarketManager;
import utils.PropertiesManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        PropertiesManager pm = new PropertiesManager();
        AuthManager.init(pm.getUsersFile());
        MarketManager.loadHistory(pm.getHistoryFile());
        MarketManager.loadBook(pm.getBookFile());
        ArrayList<Socket> sockets = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        try(ServerSocket socket = new ServerSocket(pm.getPort());
            ExecutorService exec = Executors.newCachedThreadPool()) {
            System.out.println("[Server] Listening on port " + socket.getLocalPort());
            socket.setSoTimeout(pm.getTimeout());
            Console c = new Console(exec,socket,pm,sockets);
            Thread t = new Thread(c);
            t.start();
            rt.addShutdownHook(new stopThread(c));
            exec.execute(new NotificationService());
            while (true) {
                Socket s = socket.accept();
                sockets.add(s);
                exec.execute(new ConnectionService(s));
            }
        }catch (SocketException soc){
            System.out.println("[Server] Socket Closed");
        }catch (IOException e){
            System.out.println("unable to create server socket...");
        }
    }
}

class stopThread extends Thread {
    private final Console c;
    public stopThread(Console cons) {
        this.c = cons;
    }

    @Override
    public void run() {
        System.out.println("[Server] Starting shutdown thread...");
        c.serverStop();
    }
}
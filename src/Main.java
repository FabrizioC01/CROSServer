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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {

        PropertiesManager pm = new PropertiesManager();
        AuthManager.init(pm.getUsersFile());
        MarketManager.loadHistory(pm.getHistoryFile());
        MarketManager.loadBook(pm.getBookFile());
        try(ServerSocket socket = new ServerSocket(pm.getPort());
            ExecutorService exec = Executors.newCachedThreadPool()) {
            System.out.println("[Server] Listening on port " + socket.getLocalPort());
            socket.setSoTimeout(pm.getTimeout());
            Thread t = new Thread(new Console(exec,socket,pm));
            t.start();
            exec.execute(new NotificationService());
            while (true) {
                Socket s = socket.accept();
                exec.execute(new ConnectionService(s));
            }
        }catch (SocketException soc){
            System.out.println("[Server] Socket Closed");
        }catch (IOException e){
            System.out.println("unable to create server socket...");
        }
    }
}
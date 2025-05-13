import Services.ConnectionService;
import utils.AuthManager;
import utils.Deserializer;
import utils.PropertiesManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        PropertiesManager pm = new PropertiesManager();
        AuthManager.init(pm.getUsersFile());

        try(ServerSocket socket = new ServerSocket(pm.getPort());
            ExecutorService exec = Executors.newCachedThreadPool()) {

            socket.setSoTimeout(pm.getTimeout());
            while (true) {
                Socket s = socket.accept();
                exec.execute(new ConnectionService(s));
            }

        }catch (IOException e){
            System.out.println("unable to create server socket...");
        }
    }
}
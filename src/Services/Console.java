package Services;

import utils.AuthManager;
import utils.MarketManager;
import utils.PropertiesManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public class Console implements Runnable {
    private final Scanner scanner=new Scanner(System.in);
    private final ExecutorService exec;
    private final ServerSocket s;
    private final PropertiesManager manager;

    public Console(ExecutorService e,ServerSocket s,PropertiesManager pm) {
        this.exec=e;
        this.s=s;
        this.manager=pm;
    }
    @Override
    public void run() {
        System.out.println("[Console] Console enabled, type 'help' for command list");
        while(true){
            String command=scanner.nextLine();
            switch(command.toLowerCase().trim()){
                case "help"->{
                    System.out.println("[Console] Command list:\n help - list of commands\n online - list of online users\n book - display order book\n exit - stop server");
                }
                case "online"->{
                    AuthManager.printOnlineUsers();
                }
                case "book"->{
                    MarketManager.printBooks();
                }
                case "exit"->{
                    try {
                        s.close();
                    } catch (IOException ignored) {}
                    MarketManager.saveBook(manager.getBookFile());
                    MarketManager.saveHistory(manager.getHistoryFile());
                    NotificationService.stop();
                    exec.shutdownNow();
                    System.out.println("[Console] Incoming connections blocked, the server will shut down when all clients are offline.");
                    return;
                }
                default -> {
                    System.out.println("[Console] Unknown command: " + command);
                    System.out.println("[Console] Command list:\n help - list of commands\n online - list of online users\n book - display order book\n exit - stop server");
                }
            }
        }
    }
}

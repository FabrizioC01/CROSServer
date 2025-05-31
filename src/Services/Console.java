package Services;

import utils.AuthManager;
import utils.MarketManager;
import utils.PropertiesManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public class Console implements Runnable {
    private static final Scanner scanner=new Scanner(System.in);
    private final ExecutorService exec;
    private final ServerSocket s;
    private final PropertiesManager manager;
    private final ArrayList<Socket> userSockets;

    public Console(ExecutorService e, ServerSocket s, PropertiesManager pm, ArrayList<Socket> sock) {
        this.exec=e;
        this.s=s;
        this.manager=pm;
        this.userSockets=sock;
    }
    @Override
    public void run() {
        System.out.println("[Console] Console enabled, type 'help' for command list");
        while(true){
            String command=scanner.nextLine();
            switch(command.toLowerCase().trim()){
                case "help"->{
                    System.out.println("[Console] Command list:\n help - list of commands\n online - list of online users\n book - display order book\n stops - display stop orders\n exit - stop server");
                }
                case "online"->{
                    AuthManager.printOnlineUsers();
                }
                case "book"->{
                    MarketManager.printBooks();
                }
                case "stops"->{
                    MarketManager.printStop();
                }
                case "exit"->{
                    System.exit(0);
                    return;
                }
                default -> {
                    System.out.println("[Console] Unknown command: " + command);
                    System.out.println("[Console] Command list:\n help - list of commands\n online - list of online users\n book - display order book\n stops - display stop orders\n exit - stop server");
                }
            }
        }
    }

    public void serverStop(){
        try {
            s.close();
        } catch (IOException ignored) {}
        ConnectionService.disconnectAll();
        MarketManager.saveAll();
        exec.shutdownNow();
        for(Socket sock:userSockets){
            try{sock.close();}
            catch(IOException ignored){}
        }
        System.out.println("[Console] Incoming connections blocked, the server will shut down when all clients are offline.");
    }

}

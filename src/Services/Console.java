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
    private final ArrayList<Socket> userSockets;
    private final MarketManager marketManager;

    public Console(ExecutorService e, ServerSocket s, PropertiesManager pm, ArrayList<Socket> sock,MarketManager manager) {
        this.exec=e;
        this.s=s;
        this.userSockets=sock;
        this.marketManager=manager;
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
                    marketManager.printBooks();
                }
                case "stops"->{
                    marketManager.printStop();
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
        marketManager.saveAll();
        exec.shutdownNow();
        for(Socket sock:userSockets){
            try{sock.close();}
            catch(IOException ignored){}
        }
        System.out.println("[Console] Closing server");
    }

}

package main.Services;

import utils.AuthManager;
import utils.MarketManager;
import utils.PropertiesManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

/**
 * Classe Console che attende un comando per effettuare varie operazioni
 * dal server.
 */
public class Console implements Runnable {
    private static final Scanner scanner=new Scanner(System.in);
    private final MarketManager marketManager;

    /**
     * Inizializza la classe passando il gestore del book per poter vedere:
     * il book, gli stop, gli utenti online e fermare il server.
     * @param manager gestore del book utilizzato concorrentemente con i thread per eseguire il salvataggio delle varie strutture
     */
    public Console(MarketManager manager) {
        this.marketManager=manager;
    }

    /**
     * Thread che attende i comandi.
     */
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



}

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
        MarketManager manager = new MarketManager(pm.getHistoryFile(),pm.getBookFile());
        ArrayList<Socket> sockets = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();

        try(ServerSocket socket = new ServerSocket(pm.getPort());
            ExecutorService exec = Executors.newCachedThreadPool()) {
            Console c = new Console(manager);
            Thread t = new Thread(c);

            System.out.println("[Server] Listening on port " + socket.getLocalPort());
            socket.setSoTimeout(pm.getTimeout());
            t.start();
            rt.addShutdownHook(new stopThread(exec,socket,sockets,manager)); //thread per la chiusura safe del server anche con SIGINT
            exec.execute(new NotificationService()); //parte thread delle notifiche

            while (true) {

                Socket s = socket.accept();
                sockets.add(s);
                exec.execute(new ConnectionService(s,manager));
                sockets.removeIf(Socket::isClosed); //rimuove dall'array i socket chiusi

            }

        }catch (SocketException soc){
            System.out.println("[Server] Socket Closed");
        }catch (IOException e){
            System.out.println("unable to create server socket...");
        }
    }
}

/**
 * Classe per lo shutdown Hook
 */
class stopThread extends Thread {
    private final ExecutorService exec;
    private final ServerSocket s;
    private final ArrayList<Socket> userSockets;
    private final MarketManager marketManager;

    /**
     *
     * @param exec executor dei thread client connessi al server
     * @param s socket del server in ascolto
     * @param userSockets socket degli utenti connessi
     * @param marketManager manager dei book per la persistenza dei dati
     */
    public stopThread(ExecutorService exec, ServerSocket s, ArrayList<Socket> userSockets, MarketManager marketManager) {
        this.exec = exec;
        this.s = s;
        this.userSockets = userSockets;
        this.marketManager = marketManager;
    }

    @Override
    public void run() {
        System.out.println("[Server] Starting shutdown thread...");
        serverStop();
    }

    /**
     * Funzione di chiusura che chiude tutte le socket
     * con i client(ignorando le eccezioni),
     * (disconnectAll) imposta una variabile booleana utile solo all'output nel server,
     * termina i thread e chiude la socket in ascolto.
     * Questa funzione è chiamata esclsivamente dallo shutdownHook(per ogni stop del server)
     */
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
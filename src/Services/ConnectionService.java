package main.Services;

import Errors.ClientSocketClose;
import Errors.InvalidJsonObject;
import Models.ClosedOrder;
import Models.Credentials;
import Models.MarketValues;
import Models.User;
import enums.ResponseCode;
import utils.AuthManager;
import utils.Deserializer;
import utils.MarketManager;
import utils.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionService implements Runnable{
    private final Socket socket;
    private PrintWriter out=null;
    private BufferedReader in=null;
    private String msgPrefix;
    private User user=null;
    private final MarketManager manager;

    public ConnectionService(Socket socket,MarketManager manager) {
        this.socket = socket;
        this.msgPrefix = "["+socket.getInetAddress().getHostAddress()+":"+socket.getPort()+"] ";
        this.manager = manager;
    }

    /**
     * Thread che attende le connessioni in fase iniziale quindi
     * login, password update e registrazione, in caso di login con esito
     * positivo verrà chiamata {@code reservedArea}
     */
    @Override
    public void run() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            this.out=writer;
            this.in=reader;
            System.out.println(msgPrefix+"client connected");
            String msg = reader.readLine();
            Deserializer obj = new Deserializer(msg);
            System.out.println(msgPrefix+"ask for : "+obj.getOperation());

            switch (obj.getOperation()){
                case login -> {
                    User u = obj.getUser();
                    ResponseCode resp = AuthManager.login(u);
                    writer.println(new Serializer(resp));
                    if(resp.equals(ResponseCode.LOG_OK)){
                        NotificationService.register(u.getUsername(),socket.getInetAddress().getHostAddress(),u.getPort());
                        reservedArea(u);
                    }
                }
                case register -> {
                    User u = obj.getUser();
                    ResponseCode resp = AuthManager.register(u);
                    writer.println(new Serializer(resp));
                }
                case updateCredentials ->{
                    Credentials c = obj.getCredentials();
                    ResponseCode resp = AuthManager.changePassword(c);
                    writer.println(new Serializer(resp));
                }
                case null, default -> throw new InvalidJsonObject();
            }

        }catch (SocketException ex){
            System.out.println(msgPrefix+" Socket error");
        } catch (ClientSocketClose cl){
            System.out.println(msgPrefix+"client disconnected");
        }catch(InvalidJsonObject inv){
            System.out.println(msgPrefix+"invalid message received");
        }catch (SocketTimeoutException exc){
            System.out.println(msgPrefix+"timeout");
        } catch(IOException e) {
            System.out.println("error opening communication stream");
        }finally {
            AuthManager.logout(user);
        }
    }

    /**
     * Funzione che gestisce gli utenti appena loggati,
     * attende che richiedano operazioni, se l'operazione richiesta non
     * è contemplata l'utente viene disconnesso e viene sloggato.
     * @param user struttura dell' utente appena loggato
     */
    private void reservedArea(User user){
        this.msgPrefix = this.msgPrefix+ user.getUsername()+": ";
        this.user = user;
        System.out.println(msgPrefix+"logged in");
        try{
            while(true){
                Deserializer req = new Deserializer(in.readLine());
                System.out.println(msgPrefix+"ask for : "+req.getOperation());
                switch(req.getOperation()){
                    case login,register,updateCredentials -> {
                        System.out.println(msgPrefix+"operation not accepted in this area");
                        AuthManager.logout(user);
                        return;
                    }case insertStopOrder -> {
                        int id = manager.insertStopOrder(req.getMarketValues(),user.getUsername());
                        Serializer ser = new Serializer(id);
                        out.println(ser);
                    }
                    case insertMarketOrder -> {
                        int id = manager.insertMarketOrder(req.getMarketValues(),user.getUsername(),"market");
                        Serializer ser = new Serializer(id);
                        out.println(ser);
                    }
                    case insertLimitOrder -> {
                        int id = manager.insertLimitOrder(req.getMarketValues(), user.getUsername());
                        Serializer ser = new Serializer(id);
                        out.println(ser);
                    }
                    case getPriceHistory -> {
                        MarketValues mv =req.getMarketValues();
                        ArrayList<ClosedOrder> resp = manager.getMonthHistory(mv.getMonth());
                        Serializer ser  = new Serializer(resp);
                        out.println(ser);
                    }
                    case cancelOrder -> {
                        MarketValues mv =req.getMarketValues();
                        boolean res = manager.cancelOrder(mv.getOrderId(),user.getUsername());
                        Serializer ser;
                        if(res) ser = new Serializer(ResponseCode.DEL_OK);
                        else ser = new Serializer(ResponseCode.DEL_ERR);
                        out.println(ser);
                    }
                    case logout -> {
                        ResponseCode rc = AuthManager.logout(user);
                        Serializer ser = new Serializer(rc);
                        out.println(ser);

                    }
                    case null, default -> {
                        AuthManager.logout(user);
                        return;
                    }
                }
            }
        }catch (IOException ex){
            System.out.println(msgPrefix+"error in I/O operation...");
        }catch (ClientSocketClose cl){
            System.out.println(msgPrefix+"client disconnected");
        }catch(InvalidJsonObject inv){
            System.out.println(msgPrefix+"invalid message received");
        }finally {
            //effettua sempre il logout
            AuthManager.logout(user);
        }
    }
}

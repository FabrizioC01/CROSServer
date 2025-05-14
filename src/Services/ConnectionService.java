package Services;

import Errors.ClientSocketClose;
import Errors.InvalidJsonObject;
import Models.Credentials;
import Models.User;
import enums.ResponseCode;
import utils.AuthManager;
import utils.Deserializer;
import utils.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ConnectionService implements Runnable{
    private final Socket socket;
    private String msgPrefix;
    private User user=null;

    public ConnectionService(Socket socket) {
        this.socket = socket;
        this.msgPrefix = "["+socket.getInetAddress().getHostAddress()+":"+socket.getPort()+"] ";
    }
    @Override
    public void run() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            System.out.println(msgPrefix+"client connected");
            String msg = reader.readLine();
            Deserializer obj = new Deserializer(msg);
            System.out.println(msgPrefix+"ask for : "+obj.getOperation());

            switch (obj.getOperation()){
                case login -> {
                    User u = obj.getUser();
                    ResponseCode resp = AuthManager.login(u);
                    if(resp.equals(ResponseCode.LOG_OK)) reservedArea(u);
                    writer.println(new Serializer(resp));
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
                default -> throw new InvalidJsonObject();
            }
            System.out.println(msgPrefix+"client disconnected");
            AuthManager.logout(user);
        }catch (ClientSocketClose cl){
            System.out.println(msgPrefix+"client disconnected");
            AuthManager.logout(user);
        }catch(InvalidJsonObject inv){
            System.out.println(msgPrefix+"invalid message received");
            AuthManager.logout(user);
        }catch (SocketTimeoutException exc){
            System.out.println(msgPrefix+"timeout");
            AuthManager.logout(user);
        } catch(IOException e) {
            System.out.println("error opening communication stream");
        }
    }


    private void reservedArea(User user){
        this.msgPrefix = this.msgPrefix+ user.getUsername()+": ";
        this.user = user;
        System.out.println(msgPrefix+"logged in");

    }
}

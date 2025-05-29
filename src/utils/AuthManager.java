package utils;


import Models.Credentials;
import Models.User;
import Models.UserList;
import Services.NotificationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import enums.ResponseCode;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class AuthManager {
    private static String fileName=null;

    private static final ArrayList<User> online=new ArrayList<>();

    public static void init(String fName){
        fileName=fName;
        File f = new File(fName);
        try{
            if(f.createNewFile()){
                System.out.println("Creating and using "+fileName+" as users file...");
            }else{
                System.out.println("Using "+fileName+" as users file...");

            }
        }catch (IOException e){
            System.out.println("Users file error...");
            throw new RuntimeException();
        }
    }

    public static synchronized ResponseCode register(User auth){
        Gson gson=new GsonBuilder().setPrettyPrinting().create();
        if(auth.getPassword().isEmpty() || auth.getPassword().isBlank()) return ResponseCode.REG_INV_PWD;
        try{
            BufferedReader fr=new BufferedReader(new FileReader(fileName));
            UserList val = gson.fromJson(fr, UserList.class);
            ArrayList<User> list;

            if(val == null) list = new ArrayList<>();
            else list = val.getUsers();

            if(list.contains(auth)) return ResponseCode.REG_INV_USR;

            fr.close();
            list.add(new User(auth.getUsername(),auth.getPassword()));

            FileWriter fw = new FileWriter(fileName);
            gson.toJson(new UserList(list), UserList.class, fw);


            fw.close();
            return ResponseCode.REG_OK;
        }catch (IOException e){
            System.out.println("Users file error...");
            return ResponseCode.REG_GENERIC;
        }
    }

    public static synchronized ResponseCode login(User auth){
        if(online.contains(auth)) return ResponseCode.LOG_ONLINE;

        try(BufferedReader read= new BufferedReader(new FileReader(fileName))){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            UserList obj= gson.fromJson(read, UserList.class);

            if(obj==null) return ResponseCode.LOG_WRONG;

            ArrayList<User> users;
            users = obj.getUsers();

            for (User us : users) {
                if (us.match(auth)) {
                    online.add(auth);
                    printOnlineUsers();
                    return ResponseCode.LOG_OK;
                }
            }
            return ResponseCode.LOG_WRONG;

        }catch (IOException e){
            System.out.println("Users file error...");
            return ResponseCode.LOG_GENERIC;
        }
    }

    public static synchronized void printOnlineUsers(){
        System.out.println("[Online] "+online.size());
        online.forEach((User u)->{
            System.out.println("- "+u.getUsername());
        });
    }

    public static synchronized ResponseCode logout(User auth){
        boolean r = online.remove(auth);
        if (r) {
            printOnlineUsers();
            NotificationService.unRegister(auth.getUsername());
            return ResponseCode.LOGOUT_OK;
        }
        return ResponseCode.LOGOUT_OFFLINE;
    }

    public static synchronized ResponseCode changePassword(Credentials auth){
        Gson gson=new GsonBuilder().setPrettyPrinting().create();
        if(auth.getNewPassword().equals(auth.getOldPassword())) return ResponseCode.UPD_EQ;
        if(auth.getNewPassword().isBlank()|| auth.getNewPassword().isEmpty()) return ResponseCode.UPD_INV_PWD;
        for(User u : online){
            if(u.getUsername().equals(auth.getUsername())) return ResponseCode.UPD_ONLINE;
        }
        try{
            FileReader fr = new FileReader(fileName);
            UserList obj = gson.fromJson(fr, UserList.class);
            fr.close();
            if(obj==null) obj = new UserList(new ArrayList<>());
            ArrayList<User> list = obj.getUsers();

            for (User u : list) {
                if (u.getUsername().equals(auth.getUsername()) && u.getPassword().equals(auth.getOldPassword())) {
                    u.setPassword(auth.getNewPassword());
                    try {
                        FileWriter fw = new FileWriter(fileName);
                        gson.toJson(new UserList(list), fw);
                        fw.close();
                    } catch (IOException e) {
                        System.out.println("Users file error...");
                        return ResponseCode.UPD_GENERIC;
                    }
                    return ResponseCode.UPD_OK;
                }
            }
            return ResponseCode.UPD_WRONG;
        }catch (IOException e){
            System.out.println("Users file error...");
            return ResponseCode.UPD_GENERIC;
        }
    }
}
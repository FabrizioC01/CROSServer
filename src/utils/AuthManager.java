package utils;


import Models.Credentials;
import Models.User;
import Models.UserList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enums.ResponseCode;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class AuthManager {
    private static String fileName=null;
    private static final ReentrantLock fileLock = new ReentrantLock();

    private static final ArrayList<User> online=new ArrayList<>();
    private static final ReentrantLock onlineLock = new ReentrantLock();

    public static void init(String fName){
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
        fileName=fName;
    }

    public static ResponseCode register(User auth){
        if(auth.getPassword()==null || auth.getPassword().isEmpty()) return ResponseCode.REG_INV_PWD;
        fileLock.lock();
        try {
            BufferedReader read= new BufferedReader(new FileReader(fileName));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            UserList obj;
            obj = gson.fromJson(read, UserList.class);
            if(obj==null) obj = new UserList(new ArrayList<>());
            ArrayList<User> users;
            users = obj.getUsers();

            read.close();
            FileWriter writer = new FileWriter(fileName);

            if (users.contains(auth)) {
                fileLock.unlock();
                writer.close();
                return  ResponseCode.REG_INV_USR;
            }else{
                users.add(auth);
                gson.toJson(new UserList(users),writer);
                fileLock.unlock();
                writer.close();
                return ResponseCode.REG_OK;
            }
        }catch (IOException e){
            System.out.println("Users file error...");
            return ResponseCode.REG_GENERIC;
        }
    }

    public static ResponseCode login(User auth){
        onlineLock.lock();
        if(online.contains(auth)){
            onlineLock.unlock();
            return ResponseCode.LOG_ONLINE;
        }
        try(BufferedReader read= new BufferedReader(new FileReader(fileName))){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            UserList obj= gson.fromJson(read, UserList.class);
            if(obj==null){
                onlineLock.unlock();
                return ResponseCode.LOG_WRONG;
            }
            ArrayList<User> users;
            users = obj.getUsers();

            if(users.contains(auth)){
                onlineLock.unlock();
                return ResponseCode.LOG_OK;
            }else{
                onlineLock.unlock();
                return ResponseCode.LOG_WRONG;
            }
        }catch (IOException e){
            System.out.println("Users file error...");
            return ResponseCode.LOG_GENERIC;
        }
    }

    public static ResponseCode logout(User auth){
        onlineLock.lock();
        boolean r = online.remove(auth);
        onlineLock.unlock();
        if (r) return  ResponseCode.LOGOUT_OK;
        return ResponseCode.LOGOUT_OFFLINE;
    }

    public static ResponseCode changePassword(Credentials auth){
        if(auth.getNewPassword().equals(auth.getOldPassword())) return ResponseCode.UPD_EQ;
        if(auth.getNewPassword().isEmpty()) return ResponseCode.UPD_INV_PWD;
        onlineLock.lock();
        for(User u : online){
            if(u.getUsername().equals(auth.getUsername())){
                onlineLock.unlock();
                return ResponseCode.UPD_ONLINE;
            }
        }
        fileLock.lock();
        try(BufferedReader read= new BufferedReader(new FileReader(fileName))){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            UserList obj= gson.fromJson(read, UserList.class);
            if(obj==null){
                onlineLock.unlock();
                fileLock.unlock();
                return ResponseCode.UPD_WRONG;
            }

            for(int i =0; i<obj.getUsers().size(); i++){
                String username = obj.getUsers().get(i).getUsername();
                String password = obj.getUsers().get(i).getPassword();
                if(username.equals(auth.getUsername()) && password.equals(auth.getOldPassword())){
                    User u = new User(username,auth.getNewPassword());
                    obj.getUsers().set(i,u);
                    onlineLock.unlock();
                    fileLock.unlock();
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
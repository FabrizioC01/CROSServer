package utils;


import Models.Credentials;
import Models.User;
import Models.UserList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enums.ResponseCode;

import java.io.*;
import java.util.ArrayList;
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
            System.exit(1);
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
            throw new RuntimeException();
        }
    }

    public static ResponseCode login(User auth){
        onlineLock.lock();
        if(online.contains(auth)){
            onlineLock.unlock();
            return ResponseCode.LOG_ONLINE;
        }
        try{
            BufferedReader read= new BufferedReader(new FileReader(fileName));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            UserList obj= gson.fromJson(read, UserList.class);
            if(obj==null){
                onlineLock.unlock();
                return ResponseCode.LOG_WRONG;
            }
            ArrayList<User> users;
            users = obj.getUsers();

            if(users.contains(auth)){
                read.close();
                onlineLock.unlock();
                return ResponseCode.LOG_OK;
            }else{
                read.close();
                onlineLock.unlock();
                return ResponseCode.LOG_WRONG;
            }
        }catch (IOException e){
            System.out.println("Users file error...");
            throw new RuntimeException();
        }
    }


}
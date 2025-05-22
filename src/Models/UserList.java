package Models;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;

public class UserList implements Serializable {
    private ArrayList<User> users;

    public UserList(ArrayList<User> users) {
        this.users = users;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(users);
    }
}
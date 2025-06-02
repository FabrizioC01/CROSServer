package Models;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Classe wrapper per (de)serializzazione del file utenti
 */
public class UserList implements Serializable {
    private final ArrayList<User> users;

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
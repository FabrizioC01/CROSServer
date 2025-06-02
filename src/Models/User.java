package main.Models;

/**
 * Campo values che viene mandato dal client al momento del login,
 * nella quale viene inclusa la porta udp per l'eventuale servizio di notifica
 */
public class User {
    private String username;
    private String password;
    private Integer port;

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public int getPort(){
        return port;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.port = null;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return this.username.equals(((User) obj).getUsername());
        }
        return false;
    }

    public boolean match(User user) {
        return this.username.equals(user.getUsername()) && this.password.equals(user.getPassword());
    }
}

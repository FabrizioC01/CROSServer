package Models;

public class User {
    private String username;
    private String password;
    private transient int port;

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public int getPort(){
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User user) {
            return this.username.equals(user.getUsername()) && this.password.equals(user.password);
        }
        return false;
    }
}

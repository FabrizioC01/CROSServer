package Models;

/**
 * Classe (de)serializzazione campo values per cambio password.
 */
public class Credentials {
    private String username;
    private String oldPassword;
    private String newPassword;

    public Credentials(String username, String oldPassword, String newPassword) {
        this.username = username;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getOldPassword() {
        return oldPassword;
    }
    public String getNewPassword() {
        return newPassword;
    }

}

package main.enums;

public enum ResponseCode {

    /**
     * Codici di risposta in fase di registrazione
     */
    REG_OK(100,"Registration Successful"),
    REG_INV_PWD(101,"Invalid Password"),
    REG_INV_USR(102,"Username already exists"),
    REG_GENERIC(103,"Registration Failed"),

    /**
     * Codici di risposta in fase di login
     */
    LOG_OK(100,"Login Successful"),
    LOG_WRONG(102,"Wrong username or password"),
    LOG_ONLINE(103,"User already logged in"),
    LOG_GENERIC(104,"Authentication failed"),

    /**
     * Codici di risposta per di aggiornamento delle credenziali
     */
    UPD_OK(100,"Credentials update successful"),
    UPD_INV_PWD(101,"Invalid new password"),
    UPD_WRONG(102,"Invalid username or old password"),
    UPD_EQ(103,"New password equals to old one"),
    UPD_ONLINE(104,"User online"),
    UPD_GENERIC(105,"Credentials update failed"),

    /**
     * Codici di riposta al logout
     */
    LOGOUT_OK(100,"Logout Successful"),
    LOGOUT_OFFLINE(101,"User already offline, or other logout error"),

    /**
     * Codice di risposta per cancellazione degli ordini
     */
    DEL_OK(100,"Order deleted successfully"),
    DEL_ERR(101,"Order does not exist, or other order deletion error");

    private final int code;
    private final String message;
    ResponseCode(int code,String message) {
        this.code = code;
        this.message = message;

    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}

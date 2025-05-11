import utils.AuthManager;
import utils.Deserializer;
import utils.PropertiesManager;

public class Main {
    public static void main(String[] args) {
        PropertiesManager pm = new PropertiesManager();
        AuthManager.init(pm.getUsersFile());
        Deserializer<Object> d = new Deserializer<>();
    }
}
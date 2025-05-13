package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {
    private final static String path = "server.properties";
    private final Properties properties;

    public PropertiesManager() {
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        File file = new File(path);
        if (file.exists()) {
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                System.out.println("Error loading properties file");
                System.exit(1);
            }
        } else {
            setDefaultProperties();
            saveProperties();
        }
    }

    private void setDefaultProperties() {
        properties.setProperty("port", "1234");
        properties.setProperty("timeout", "0");
        properties.setProperty("usersFile","users.json");
    }

    private void saveProperties() {
        try (FileOutputStream output = new FileOutputStream(path)) {
            properties.store(output, null);
        } catch (IOException e) {
            System.out.println("Error saving properties file");
            System.exit(1);
        }
    }

    public int getPort(){
        String port = properties.getProperty("port");
        if(port==null){
            System.out.println("Port not specified using default: 1234");
            return 1234;
        }
        return Integer.parseInt(properties.getProperty("port"));
    }
    public int getTimeout(){
        String timeout = properties.getProperty("timeout");
        if(timeout==null){
            System.out.println("Timeout not specified, default timeout: 0");
            return 0;
        }
        return Integer.parseInt(properties.getProperty("timeout"));
    }
    public String getUsersFile(){
        String usersFile = properties.getProperty("usersFile");
        if(usersFile==null){
            System.out.println("Users file not specified using default: 'users.json'");
            return "users.json";
        }
        return properties.getProperty("usersFile");
    }
}
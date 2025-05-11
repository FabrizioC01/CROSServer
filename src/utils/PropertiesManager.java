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
        return Integer.parseInt(properties.getProperty("port"));
    }
    public int getTimeout(){
        return Integer.parseInt(properties.getProperty("timeout"));
    }
    public String getUsersFile(){
        return properties.getProperty("usersFile");
    }
}
package main.utils;

import Errors.ClientSocketClose;
import Errors.InvalidJsonObject;
import Models.Credentials;
import Models.MarketValues;
import Models.User;
import com.google.gson.*;
import enums.OperationToken;

/**
 * Client Json String deserializer
 */
public class Deserializer {
    private final OperationToken operation;
    private Credentials credentials=null; //used only for password update
    private User user=null; //used for login and registration
    private MarketValues marketValues=null; //used only for market operations

    /**
     *  Popola i campi a seconda della richiesta ricevuta e mette null gli altri.
     * @param serialized_object Oggetto che si vuole de-serializzare
     * @throws InvalidJsonObject Sollevata in caso il messaggio non rispetti il formato richiesto
     * @throws ClientSocketClose Sollevata in caso la readLine riceva null (connessione chiusa dal client)
     */
    public Deserializer(String serialized_object) throws InvalidJsonObject,ClientSocketClose {
        Gson gson = new Gson();
        if(serialized_object==null) throw new ClientSocketClose();
        try {
            JsonObject jsonObject = JsonParser.parseString(serialized_object).getAsJsonObject();
            if (!jsonObject.has("operation")) throw new InvalidJsonObject();
            this.operation = gson.fromJson(jsonObject.get("operation"), OperationToken.class);
            switch (operation){
                case OperationToken.login,OperationToken.register,OperationToken.logout ->{
                    this.user = gson.fromJson(jsonObject.get("values"), User.class);
                }
                case OperationToken.insertStopOrder,
                     OperationToken.insertLimitOrder,
                     OperationToken.cancelOrder,
                     OperationToken.insertMarketOrder,
                     OperationToken.getPriceHistory-> {
                    this.marketValues = gson.fromJson(jsonObject.get("values"), MarketValues.class);
                }
                case OperationToken.updateCredentials -> this.credentials = gson.fromJson(jsonObject.get("values"), Credentials.class);
                default -> throw new InvalidJsonObject();
            }
        } catch (JsonSyntaxException e) {
            throw new InvalidJsonObject();
        }

    }

    public OperationToken getOperation() {
        return operation;
    }

    public User getUser() {
        return user;
    }

    public MarketValues getMarketValues() {
        return marketValues;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}

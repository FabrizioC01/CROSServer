package utils;

import Errors.InvalidJsonObject;
import Models.Credentials;
import Models.MarketValues;
import Models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import enums.OperationToken;

/**
 * Client Json String deserializer
 */
public class Deserializer {
    private final OperationToken operation;
    private Credentials credentials=null;
    private User user=null;
    private MarketValues marketValues=null;

    public Deserializer(String serialized_object) throws InvalidJsonObject {
        Gson gson = new Gson();
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
}

package utils;

import Errors.InvalidJsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import enums.OperationToken;

public class Deserializer<T> {
    private OperationToken operation;
    private T values;

    public Deserializer(String serialized_object) throws InvalidJsonObject {
        Gson gson = new Gson();
        try {
            JsonObject jsonObject = JsonParser.parseString(serialized_object).getAsJsonObject();
            if (!jsonObject.has("operation")) throw new InvalidJsonObject();
            this.operation = gson.fromJson(jsonObject.get("operation"), OperationToken.class);

        } catch (JsonSyntaxException e) {
            throw new InvalidJsonObject();
        }

    }
}

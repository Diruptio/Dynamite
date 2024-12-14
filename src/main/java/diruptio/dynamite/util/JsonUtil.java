package diruptio.dynamite.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class JsonUtil {
    public static final Gson GSON = new Gson();

    /**
     * Builds an error message in JSON format
     *
     * @param message The error message
     * @return The JSON error message
     */
    public static @NotNull String jsonError(final @NotNull String message) {
        JsonObject json = new JsonObject();
        json.addProperty("error", message);
        return json.toString();
    }
}

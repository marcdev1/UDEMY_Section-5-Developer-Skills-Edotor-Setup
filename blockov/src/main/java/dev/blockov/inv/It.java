package dev.blockov.inv;

import com.google.gson.JsonObject;

/** Objet leger echange entre serveur et client (contenu de conteneur, objet en cours d'utilisation...). */
public record It(String uid, String id, int count, String name) {
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("uid", uid);
        o.addProperty("id", id);
        o.addProperty("count", count);
        o.addProperty("name", name);
        return o;
    }

    public static It fromJson(JsonObject o) {
        return new It(
                o.get("uid").getAsString(),
                o.get("id").getAsString(),
                o.has("count") ? o.get("count").getAsInt() : 1,
                o.has("name") ? o.get("name").getAsString() : o.get("id").getAsString());
    }
}

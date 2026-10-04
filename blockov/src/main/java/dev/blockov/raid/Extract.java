package dev.blockov.raid;

import com.google.gson.JsonObject;

/** Point d'extraction : rayon fixe autour de (x, z), cout en koens. */
public record Extract(String name, int x, int z, int cost) {
    public static final double RADIUS = 4.0;

    public boolean contains(double px, double pz) {
        double dx = px - (x + 0.5), dz = pz - (z + 0.5);
        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("x", x);
        o.addProperty("z", z);
        o.addProperty("cost", cost);
        return o;
    }

    public static Extract fromJson(JsonObject o) {
        return new Extract(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("z").getAsInt(),
                o.get("cost").getAsInt());
    }
}

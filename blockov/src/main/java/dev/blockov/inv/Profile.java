package dev.blockov.inv;

import com.google.gson.JsonObject;

/** Progression persistante d'un joueur (hors raid). */
public class Profile {
    public static final int XP_PER_LEVEL = 1000;
    public static final int MAX_LEVEL = 60;

    public int koens = 5000;
    public int xp;
    public int raids;
    public int extracts;
    public int deaths;
    public int kills;

    public int level() {
        return Math.min(MAX_LEVEL, 1 + xp / XP_PER_LEVEL);
    }

    public float survivalRate() {
        return raids == 0 ? 0f : (float) extracts / raids;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("koens", koens);
        o.addProperty("xp", xp);
        o.addProperty("raids", raids);
        o.addProperty("extracts", extracts);
        o.addProperty("deaths", deaths);
        o.addProperty("kills", kills);
        return o;
    }

    public static Profile fromJson(JsonObject o) {
        Profile p = new Profile();
        if (o == null) return p;
        p.koens = getInt(o, "koens", p.koens);
        p.xp = getInt(o, "xp", 0);
        p.raids = getInt(o, "raids", 0);
        p.extracts = getInt(o, "extracts", 0);
        p.deaths = getInt(o, "deaths", 0);
        p.kills = getInt(o, "kills", 0);
        return p;
    }

    private static int getInt(JsonObject o, String key, int def) {
        return o.has(key) ? o.get(key).getAsInt() : def;
    }
}

package dev.blockov.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Sante par partie du corps, facon Arena Breakout. */
public class Health {
    public enum Part {
        HEAD("Tete", 35, true),
        THORAX("Thorax", 85, true),
        STOMACH("Ventre", 70, false),
        LEFT_ARM("Bras G", 60, false),
        RIGHT_ARM("Bras D", 60, false),
        LEFT_LEG("Jambe G", 65, false),
        RIGHT_LEG("Jambe D", 65, false);

        public final String label;
        public final float max;
        /** Partie vitale : tombe a 0 = mort. */
        public final boolean vital;

        Part(String label, float max, boolean vital) {
            this.label = label;
            this.max = max;
            this.vital = vital;
        }
    }

    public static final Part[] PARTS = Part.values();
    /** Les degats sur un membre deja detruit sont repartis sur le reste du corps avec ce facteur. */
    private static final float OVERFLOW = 0.7f;

    public final float[] hp = new float[PARTS.length];

    public Health() {
        for (Part p : PARTS) hp[p.ordinal()] = p.max;
    }

    public float get(Part p) {
        return hp[p.ordinal()];
    }

    public float frac(Part p) {
        return hp[p.ordinal()] / p.max;
    }

    public boolean destroyed(Part p) {
        return hp[p.ordinal()] <= 0;
    }

    public float total() {
        float t = 0;
        for (float v : hp) t += Math.max(0, v);
        return t;
    }

    public static float totalMax() {
        float t = 0;
        for (Part p : PARTS) t += p.max;
        return t;
    }

    /** Applique des degats ; renvoie true si le joueur meurt. */
    public boolean damage(Part part, float amount) {
        int i = part.ordinal();
        if (hp[i] > 0) {
            float rest = amount - hp[i];
            hp[i] = Math.max(0, hp[i] - amount);
            if (hp[i] <= 0 && part.vital) return true;
            if (rest <= 0) return false;
            amount = rest;
        }
        // Membre detruit : repartition sur les parties encore en vie
        int alive = 0;
        for (Part p : PARTS) if (hp[p.ordinal()] > 0) alive++;
        if (alive == 0) return true;
        float share = amount * OVERFLOW / alive;
        for (Part p : PARTS) {
            int j = p.ordinal();
            if (hp[j] <= 0) continue;
            hp[j] = Math.max(0, hp[j] - share);
            if (hp[j] <= 0 && p.vital) return true;
        }
        return false;
    }

    /** Degats repartis uniformement (feu, poison, explosion...). */
    public boolean damageAll(float amount) {
        boolean dead = false;
        float share = amount / PARTS.length;
        for (Part p : PARTS) dead |= damage(p, share);
        return dead;
    }

    /** Soin lent des parties non detruites. */
    public void heal(float amount) {
        for (Part p : PARTS) {
            int i = p.ordinal();
            if (hp[i] > 0) hp[i] = Math.min(p.max, hp[i] + amount);
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (float v : hp) a.add(v);
        o.add("hp", a);
        return o;
    }

    public static Health fromJson(JsonObject o) {
        Health h = new Health();
        if (o == null || !o.has("hp")) return h;
        JsonArray a = o.getAsJsonArray("hp");
        for (int i = 0; i < Math.min(a.size(), h.hp.length); i++) h.hp[i] = a.get(i).getAsFloat();
        return h;
    }
}

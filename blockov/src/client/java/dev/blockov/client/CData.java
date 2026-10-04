package dev.blockov.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blockov.combat.Health;
import dev.blockov.inv.It;
import dev.blockov.inv.Profile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Etat client recu du serveur (reconstruit a partir de CData.class, CData$Extract.class, CData$Notice.class).
 */
final class CData {
    static Profile profile = new Profile();
    static int level = 1;
    static boolean inRaid;
    static Health health = new Health();
    static int raidLeft;
    static long raidLeftAt;
    static float weight;
    static final List<Extract> EXTRACTS = new ArrayList<>();
    static String extracting;
    static float exLeft;
    static String useUid;
    static float useFrac;
    static It container;
    static String containerName;
    static JsonObject raidEnd;
    static final List<Notice> NOTICES = new ArrayList<>();
    static long flashUntil;
    static int flashTicks;
    static long hurtAt;
    static int hurtPart = -1;
    static Vec3 hurtFrom;
    static long hitAt;
    static boolean hitKill;
    static long reloadUntil;
    static int reloadTicks;
    static long magCheckAt;

    private CData() {}

    static void onProfile(JsonObject o) {
        profile = Profile.fromJson(o.getAsJsonObject("p"));
        level = o.get("level").getAsInt();
    }

    static void onRaid(JsonObject o) {
        inRaid = true;
        raidLeft = o.get("left").getAsInt();
        raidLeftAt = System.currentTimeMillis();
        health = Health.fromJson(o.getAsJsonObject("health"));
        weight = o.get("weight").getAsFloat();
        EXTRACTS.clear();
        for (JsonElement e : o.getAsJsonArray("extracts")) {
            JsonObject eo = e.getAsJsonObject();
            EXTRACTS.add(new Extract(eo.get("name").getAsString(), eo.get("x").getAsInt(), eo.get("z").getAsInt(),
                    eo.get("cost").getAsInt()));
        }
        extracting = o.has("extracting") ? o.get("extracting").getAsString() : null;
        exLeft = o.has("exLeft") ? o.get("exLeft").getAsFloat() : 0;
        useUid = o.has("use") ? o.get("use").getAsString() : null;
        useFrac = o.has("useFrac") ? o.get("useFrac").getAsFloat() : 0;
    }

    static void onRaidEnd(JsonObject o) {
        inRaid = false;
        raidEnd = o;
        container = null;
        extracting = null;
        useUid = null;
        health = new Health();
    }

    static int timeLeft() {
        return Math.max(0, raidLeft - (int) ((System.currentTimeMillis() - raidLeftAt) / 1000L));
    }

    static void notice(String text, int color) {
        NOTICES.add(new Notice(text, color, System.currentTimeMillis()));
        while (NOTICES.size() > 4) NOTICES.remove(0);
    }

    static void onHurt(JsonObject o) {
        hurtAt = System.currentTimeMillis();
        hurtPart = o.get("part").getAsInt();
        if (o.has("from")) {
            JsonArray a = o.getAsJsonArray("from");
            hurtFrom = new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
        }
    }

    static void clear() {
        profile = new Profile();
        inRaid = false;
        container = null;
        raidEnd = null;
        NOTICES.clear();
    }

    record Extract(String name, int x, int z, int cost) {}

    record Notice(String text, int color, long at) {}
}

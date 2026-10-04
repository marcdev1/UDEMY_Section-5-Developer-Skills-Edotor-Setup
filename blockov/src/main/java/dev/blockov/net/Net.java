package dev.blockov.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.blockov.inv.Profile;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class Net {
    public static final int WHITE = 0xFFFFFF;
    public static final int GREEN = 0x55FF55;
    public static final int YELLOW = 0xFFDD55;
    public static final int RED = 0xFF5555;

    private Net() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(BlockovPayload.TYPE, BlockovPayload.CODEC);
    }

    public static void send(ServerPlayer player, String type, JsonObject o) {
        o.addProperty("t", type);
        if (ServerPlayNetworking.canSend(player, BlockovPayload.TYPE)) {
            ServerPlayNetworking.send(player, new BlockovPayload(o.toString()));
        }
    }

    public static void profile(ServerPlayer player, Profile p) {
        JsonObject o = new JsonObject();
        o.add("p", p.toJson());
        o.addProperty("level", p.level());
        send(player, "profile", o);
    }

    public static void notice(ServerPlayer player, String text, int color) {
        JsonObject o = new JsonObject();
        o.addProperty("text", text);
        o.addProperty("color", color);
        send(player, "notice", o);
    }

    public static void hurt(ServerPlayer player, int part, Vec3 from) {
        JsonObject o = new JsonObject();
        o.addProperty("part", part);
        if (from != null) {
            JsonArray a = new JsonArray();
            a.add(from.x);
            a.add(from.y);
            a.add(from.z);
            o.add("from", a);
        }
        send(player, "hurt", o);
    }

    public static void hit(ServerPlayer player, boolean kill) {
        JsonObject o = new JsonObject();
        o.addProperty("kill", kill);
        send(player, "hit", o);
    }
}

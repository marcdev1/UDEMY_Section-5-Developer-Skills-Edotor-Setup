package dev.blockov.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.blockov.Blockov;
import dev.blockov.net.BlockovPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class BlockovClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Les receveurs de payload s'executent sur le thread client
        ClientPlayNetworking.registerGlobalReceiver(BlockovPayload.TYPE, (payload, context) -> handle(payload.json()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CData.clear());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Blockov.MOD_ID, "hud"), Hud::render);
    }

    private static void handle(String json) {
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            Blockov.LOG.warn("Paquet blockov invalide", e);
            return;
        }
        switch (o.get("t").getAsString()) {
            case "profile" -> CData.onProfile(o);
            case "raid" -> CData.onRaid(o);
            case "raidEnd" -> CData.onRaidEnd(o);
            case "notice" -> CData.notice(o.get("text").getAsString(), o.get("color").getAsInt());
            case "hurt" -> CData.onHurt(o);
            case "hit" -> {
                CData.hitAt = System.currentTimeMillis();
                CData.hitKill = o.get("kill").getAsBoolean();
            }
            default -> Blockov.LOG.debug("Type de paquet inconnu : {}", o.get("t"));
        }
    }
}

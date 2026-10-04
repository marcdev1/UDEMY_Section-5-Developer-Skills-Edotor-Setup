package dev.blockov;

import dev.blockov.command.RaidCommand;
import dev.blockov.net.Net;
import dev.blockov.raid.RaidManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blockov implements ModInitializer {
    public static final String MOD_ID = "blockov";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Net.register();
        RaidManager raids = RaidManager.INSTANCE;

        ServerLifecycleEvents.SERVER_STARTED.register(raids::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(raids::shutdown);
        ServerTickEvents.END_SERVER_TICK.register(raids::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> raids.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> raids.onDisconnect(handler.getPlayer()));

        ServerLivingEntityEvents.AFTER_DAMAGE.register(raids::onDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(raids::onDeath);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> RaidCommand.register(dispatcher));
        LOG.info("Blockov charge");
    }
}

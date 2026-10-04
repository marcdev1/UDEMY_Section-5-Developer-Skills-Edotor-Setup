package dev.blockov.raid;

import dev.blockov.combat.Health;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

/** Etat d'un joueur pendant un raid. */
public class Raid {
    public final UUID player;
    public final long startTick;
    public final long endTick;
    public final Health health = new Health();

    public String extracting;
    public int exTicks;
    public int kills;
    public float weight;
    /** Mort declenchee par la sante localisee, appliquee au prochain tick. */
    public DamageSource pendingDeath;

    public Raid(UUID player, long startTick, long endTick) {
        this.player = player;
        this.startTick = startTick;
        this.endTick = endTick;
    }

    public int secondsLeft(long now) {
        return (int) Math.max(0, (endTick - now) / 20);
    }
}

package dev.blockov.raid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.blockov.combat.Health;
import dev.blockov.inv.Profile;
import dev.blockov.net.Net;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidManager {
    public static final RaidManager INSTANCE = new RaidManager();

    /** Duree d'extraction une fois dans la zone. */
    public static final int EXTRACT_TICKS = 8 * 20;
    /** Conversion degats vanilla -> points de vie localises. */
    public static final float DAMAGE_SCALE = 6f;
    public static final float OVERWEIGHT = 30f;
    public static final float HEAVY = 45f;

    public final Store store = new Store();
    private final Map<UUID, Raid> raids = new HashMap<>();
    private MinecraftServer server;

    public void load(MinecraftServer server) {
        this.server = server;
        raids.clear();
        store.load(server.getWorldPath(LevelResource.ROOT).resolve("blockov.json"));
    }

    public void shutdown(MinecraftServer server) {
        for (UUID id : new ArrayList<>(raids.keySet())) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) end(p, "MIA");
        }
        store.save();
        this.server = null;
    }

    public Raid get(ServerPlayer p) {
        return raids.get(p.getUUID());
    }

    public Profile profile(ServerPlayer p) {
        return store.profile(p.getUUID());
    }

    // ---------------------------------------------------------------- cycle de raid

    public boolean start(ServerPlayer p, int minutes) {
        if (raids.containsKey(p.getUUID())) return false;
        long now = server.getTickCount();
        Raid r = new Raid(p.getUUID(), now, now + minutes * 60L * 20L);
        raids.put(p.getUUID(), r);
        if (!store.spawns.isEmpty()) {
            Vec3 s = store.spawns.get(p.getRandom().nextInt(store.spawns.size()));
            p.teleportTo(s.x, s.y, s.z);
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        Net.notice(p, "Raid lance - " + minutes + " min. Trouvez une extraction !", Net.YELLOW);
        sync(p, r);
        return true;
    }

    /** Termine le raid : status = EXTRACTED, KIA (tue) ou MIA (temps ecoule / abandon). */
    public void end(ServerPlayer p, String status) {
        Raid r = raids.remove(p.getUUID());
        if (r == null) return;
        Profile prof = profile(p);
        long now = server.getTickCount();
        int seconds = (int) ((now - r.startTick) / 20);
        int xp = r.kills * 100;
        int earned = 0;
        prof.raids++;
        prof.kills += r.kills;
        switch (status) {
            case "EXTRACTED" -> {
                prof.extracts++;
                earned = Loot.sellValuables(p.getInventory());
                prof.koens += earned;
                xp += 300 + Math.min(600, seconds);
            }
            case "KIA" -> {
                prof.deaths++;
                xp += 50;
            }
            default -> prof.deaths++;
        }
        int oldLevel = prof.level();
        prof.xp += xp;
        p.removeEffect(MobEffects.SLOWNESS);
        p.removeEffect(MobEffects.WEAKNESS);

        JsonObject o = new JsonObject();
        o.addProperty("status", status);
        o.addProperty("kills", r.kills);
        o.addProperty("xp", xp);
        o.addProperty("earned", earned);
        o.addProperty("time", seconds);
        Net.send(p, "raidEnd", o);
        Net.profile(p, prof);
        if (prof.level() > oldLevel) Net.notice(p, "Niveau " + prof.level() + " !", Net.GREEN);
        store.save();
    }

    // ---------------------------------------------------------------- tick

    public void tick(MinecraftServer server) {
        if (raids.isEmpty()) return;
        long now = server.getTickCount();
        for (Raid r : new ArrayList<>(raids.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(r.player);
            if (p == null) {
                raids.remove(r.player);
                continue;
            }
            if (r.pendingDeath != null) {
                DamageSource src = r.pendingDeath;
                r.pendingDeath = null;
                p.setHealth(0);
                p.die(src);
                continue;
            }
            if (!p.isAlive()) continue;
            if (now >= r.endTick) {
                end(p, "MIA");
                Net.notice(p, "Temps ecoule : porte disparu", Net.RED);
                p.setHealth(0);
                p.die(p.damageSources().generic());
                continue;
            }
            // La sante reelle est geree par les parties du corps
            if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());

            tickExtract(p, r);

            if (now % 20 == 0) {
                if (p.getFoodData().getFoodLevel() >= 18) {
                    r.health.heal(1f);
                    p.causeFoodExhaustion(0.5f);
                }
                r.weight = Loot.weight(p.getInventory());
                applyEffects(p, r);
                sync(p, r);
            }
        }
    }

    private void tickExtract(ServerPlayer p, Raid r) {
        Extract here = null;
        for (Extract e : store.extracts) {
            if (e.contains(p.getX(), p.getZ())) {
                here = e;
                break;
            }
        }
        if (here == null) {
            if (r.extracting != null) {
                Net.notice(p, "Extraction annulee", Net.RED);
                r.extracting = null;
                sync(p, r);
            }
            return;
        }
        if (!here.name().equals(r.extracting)) {
            if (profile(p).koens < here.cost()) {
                if (server.getTickCount() % 40 == 0) {
                    Net.notice(p, here.name() + " : " + here.cost() + " koens requis", Net.RED);
                }
                return;
            }
            r.extracting = here.name();
            r.exTicks = EXTRACT_TICKS;
            Net.notice(p, "Extraction en cours : " + here.name(), Net.YELLOW);
            sync(p, r);
            return;
        }
        if (--r.exTicks <= 0) {
            profile(p).koens -= here.cost();
            end(p, "EXTRACTED");
            p.sendSystemMessage(Component.literal("Extrait via " + here.name() + " !"));
        }
    }

    private void applyEffects(ServerPlayer p, Raid r) {
        int slow = -1;
        if (r.weight > HEAVY) slow = 1;
        else if (r.weight > OVERWEIGHT) slow = 0;
        if (r.health.destroyed(Health.Part.LEFT_LEG) || r.health.destroyed(Health.Part.RIGHT_LEG)) slow = Math.max(slow, 1);
        if (slow >= 0) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, slow, false, false));
        if (r.health.destroyed(Health.Part.LEFT_ARM) || r.health.destroyed(Health.Part.RIGHT_ARM)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false));
        }
    }

    public void sync(ServerPlayer p, Raid r) {
        long now = server.getTickCount();
        JsonObject o = new JsonObject();
        o.addProperty("left", r.secondsLeft(now));
        o.add("health", r.health.toJson());
        o.addProperty("weight", r.weight);
        JsonArray ex = new JsonArray();
        store.extracts.forEach(e -> ex.add(e.toJson()));
        o.add("extracts", ex);
        if (r.extracting != null) {
            o.addProperty("extracting", r.extracting);
            o.addProperty("exLeft", r.exTicks / 20f);
        }
        Net.send(p, "raid", o);
    }

    // ---------------------------------------------------------------- evenements

    public void onJoin(ServerPlayer p) {
        Net.profile(p, profile(p));
    }

    public void onDisconnect(ServerPlayer p) {
        if (raids.containsKey(p.getUUID())) {
            // Deconnexion en raid = porte disparu, le butin est perdu
            p.getInventory().clearContent();
            end(p, "MIA");
        }
    }

    public void onDamage(LivingEntity entity, DamageSource src, float baseDamage, float damage, boolean blocked) {
        if (damage <= 0) return;
        if (src.getEntity() instanceof ServerPlayer attacker && attacker != entity && raids.containsKey(attacker.getUUID())) {
            Net.hit(attacker, entity.isDeadOrDying());
        }
        if (!(entity instanceof ServerPlayer p)) return;
        Raid r = raids.get(p.getUUID());
        if (r == null || r.pendingDeath != null) return;

        Health.Part part = pickPart(p, src, p.getRandom());
        float amount = damage * DAMAGE_SCALE;
        boolean dead = part == null ? r.health.damageAll(amount) : r.health.damage(part, amount);
        Vec3 from = src.getSourcePosition();
        Net.hurt(p, part == null ? -1 : part.ordinal(), from);
        if (dead) r.pendingDeath = src;
        sync(p, r);
    }

    public void onDeath(LivingEntity entity, DamageSource src) {
        if (src.getEntity() instanceof ServerPlayer killer && killer != entity) {
            Raid kr = raids.get(killer.getUUID());
            if (kr != null) {
                kr.kills++;
                Net.notice(killer, "Elimination : " + entity.getName().getString(), Net.GREEN);
            }
        }
        if (entity instanceof ServerPlayer p && raids.containsKey(p.getUUID())) {
            end(p, "KIA");
        }
    }

    /** Partie touchee ; null = degats repartis sur tout le corps. */
    private static Health.Part pickPart(ServerPlayer p, DamageSource src, RandomSource rnd) {
        if (src.is(DamageTypeTags.IS_FALL)) {
            return rnd.nextBoolean() ? Health.Part.LEFT_LEG : Health.Part.RIGHT_LEG;
        }
        if (src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE)) return null;
        Entity direct = src.getDirectEntity();
        if (direct instanceof Projectile) {
            double rel = (direct.getY() - p.getY()) / p.getBbHeight();
            if (rel > 0.8) return Health.Part.HEAD;
            if (rel > 0.55) return rnd.nextInt(3) == 0 ? side(rnd, Health.Part.LEFT_ARM, Health.Part.RIGHT_ARM) : Health.Part.THORAX;
            if (rel > 0.4) return Health.Part.STOMACH;
            return side(rnd, Health.Part.LEFT_LEG, Health.Part.RIGHT_LEG);
        }
        if (src.getEntity() != null) {
            int roll = rnd.nextInt(100);
            if (roll < 10) return Health.Part.HEAD;
            if (roll < 40) return Health.Part.THORAX;
            if (roll < 60) return Health.Part.STOMACH;
            if (roll < 80) return side(rnd, Health.Part.LEFT_ARM, Health.Part.RIGHT_ARM);
            return side(rnd, Health.Part.LEFT_LEG, Health.Part.RIGHT_LEG);
        }
        return null;
    }

    private static Health.Part side(RandomSource rnd, Health.Part a, Health.Part b) {
        return rnd.nextBoolean() ? a : b;
    }
}

package dev.blockov.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.blockov.Blockov;
import dev.blockov.inv.Profile;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sauvegarde JSON dans le dossier du monde : profils, extractions et points d'apparition. */
public class Store {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final Map<UUID, Profile> profiles = new HashMap<>();
    public final List<Extract> extracts = new ArrayList<>();
    public final List<Vec3> spawns = new ArrayList<>();
    private Path file;

    public void load(Path file) {
        this.file = file;
        profiles.clear();
        extracts.clear();
        spawns.clear();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.has("profiles")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("profiles").entrySet()) {
                    profiles.put(UUID.fromString(e.getKey()), Profile.fromJson(e.getValue().getAsJsonObject()));
                }
            }
            if (root.has("extracts")) {
                for (JsonElement e : root.getAsJsonArray("extracts")) extracts.add(Extract.fromJson(e.getAsJsonObject()));
            }
            if (root.has("spawns")) {
                for (JsonElement e : root.getAsJsonArray("spawns")) {
                    JsonArray a = e.getAsJsonArray();
                    spawns.add(new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()));
                }
            }
        } catch (IOException | RuntimeException ex) {
            Blockov.LOG.error("Lecture impossible de {}", file, ex);
        }
    }

    public void save() {
        if (file == null) return;
        JsonObject root = new JsonObject();
        JsonObject ps = new JsonObject();
        profiles.forEach((id, p) -> ps.add(id.toString(), p.toJson()));
        root.add("profiles", ps);
        JsonArray ex = new JsonArray();
        extracts.forEach(e -> ex.add(e.toJson()));
        root.add("extracts", ex);
        JsonArray sp = new JsonArray();
        for (Vec3 v : spawns) {
            JsonArray a = new JsonArray();
            a.add(v.x);
            a.add(v.y);
            a.add(v.z);
            sp.add(a);
        }
        root.add("spawns", sp);
        try {
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException ex2) {
            Blockov.LOG.error("Ecriture impossible de {}", file, ex2);
        }
    }

    public Profile profile(UUID id) {
        return profiles.computeIfAbsent(id, k -> new Profile());
    }
}

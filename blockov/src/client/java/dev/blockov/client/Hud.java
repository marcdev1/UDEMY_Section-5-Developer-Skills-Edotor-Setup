package dev.blockov.client;

import com.google.gson.JsonObject;
import dev.blockov.combat.Health;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** HUD du mode raid : chrono, sante localisee, extractions, notifications, indicateurs de tir. */
final class Hud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFDD55;
    private static final int RED = 0xFFFF5555;
    private static final int BG = 0x90000000;

    private static final long NOTICE_MS = 4000;
    private static final long HURT_MS = 1200;
    private static final long HIT_MS = 250;
    private static final long END_MS = 10000;

    private static JsonObject shownEnd;
    private static long shownEndAt;

    private Hud() {}

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        long now = System.currentTimeMillis();

        if (CData.inRaid) {
            timer(g, font, w);
            body(g, font, h);
            extracts(g, font, w, player);
            extracting(g, font, w, h);
            hurt(g, w, h, player, now);
            hit(g, w, h, now);
            reload(g, w, h, now);
        } else {
            g.drawString(font, "Niv. " + CData.level + "  |  " + CData.profile.koens + " koens", 4, 4, GREY);
        }
        notices(g, font, w, now);
        raidEnd(g, font, w, h, now);
        flash(g, w, h, now);
    }

    private static void timer(GuiGraphics g, Font font, int w) {
        int left = CData.timeLeft();
        String s = String.format("%02d:%02d", left / 60, left % 60);
        int color = left <= 60 ? RED : left <= 300 ? YELLOW : WHITE;
        int tw = font.width(s);
        g.fill(w / 2 - tw / 2 - 4, 2, w / 2 + tw / 2 + 4, 14, BG);
        g.drawCenteredString(font, s, w / 2, 4, color);
    }

    private static void body(GuiGraphics g, Font font, int h) {
        Health hp = CData.health;
        int x = 4, y = h - 12 - Health.PARTS.length * 11 - 12;
        g.fill(x - 2, y - 2, x + 112, y + Health.PARTS.length * 11 + 12, BG);
        for (Health.Part p : Health.PARTS) {
            float f = Mth.clamp(hp.frac(p), 0, 1);
            int color = f <= 0 ? 0xFF444444 : f < 0.35f ? RED : f < 0.7f ? YELLOW : GREEN;
            boolean flashing = p.ordinal() == CData.hurtPart && System.currentTimeMillis() - CData.hurtAt < 400;
            g.drawString(font, p.label, x, y, flashing ? RED : WHITE);
            g.fill(x + 48, y + 2, x + 108, y + 7, 0xFF222222);
            g.fill(x + 48, y + 2, x + 48 + (int) (60 * f), y + 7, color);
            y += 11;
        }
        int wc = CData.weight > 45 ? RED : CData.weight > 30 ? YELLOW : GREY;
        g.drawString(font, String.format("Poids %.1f kg", CData.weight), x, y + 1, wc);
    }

    private static void extracts(GuiGraphics g, Font font, int w, LocalPlayer player) {
        int y = 20;
        for (CData.Extract e : CData.EXTRACTS) {
            double dist = Math.sqrt(Mth.square(player.getX() - e.x()) + Mth.square(player.getZ() - e.z()));
            boolean afford = CData.profile.koens >= e.cost();
            String s = e.name() + "  " + (int) dist + "m" + (e.cost() > 0 ? "  " + e.cost() + "k" : "");
            int tw = font.width(s);
            g.fill(w - tw - 8, y - 2, w - 2, y + 10, BG);
            g.drawString(font, s, w - tw - 4, y, afford ? GREEN : RED);
            y += 13;
        }
    }

    private static void extracting(GuiGraphics g, Font font, int w, int h) {
        if (CData.extracting == null) return;
        // exLeft est envoye chaque seconde : interpolation locale entre deux paquets
        float left = Math.max(0, CData.exLeft - (System.currentTimeMillis() - CData.raidLeftAt) / 1000f);
        float total = 8f;
        int bw = 120, x = w / 2 - bw / 2, y = h / 2 + 30;
        g.drawCenteredString(font, "Extraction : " + CData.extracting + String.format(" (%.1fs)", left), w / 2, y - 11, YELLOW);
        g.fill(x, y, x + bw, y + 4, 0xFF222222);
        g.fill(x, y, x + (int) (bw * (1 - left / total)), y + 4, YELLOW);
    }

    private static void hurt(GuiGraphics g, int w, int h, LocalPlayer player, long now) {
        if (CData.hurtFrom == null || now - CData.hurtAt > HURT_MS) return;
        double dx = CData.hurtFrom.x - player.getX(), dz = CData.hurtFrom.z - player.getZ();
        if (dx * dx + dz * dz < 0.01) return;
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        double rel = Math.toRadians(Mth.wrapDegrees(targetYaw - player.getYRot()));
        int alpha = (int) (255 * (1 - (now - CData.hurtAt) / (float) HURT_MS));
        int color = (alpha << 24) | 0xFF2020;
        int cx = w / 2 + (int) (Math.sin(rel) * 40), cy = h / 2 - (int) (Math.cos(rel) * 40);
        g.fill(cx - 3, cy - 3, cx + 3, cy + 3, color);
    }

    private static void hit(GuiGraphics g, int w, int h, long now) {
        if (now - CData.hitAt > HIT_MS) return;
        int c = CData.hitKill ? RED : WHITE, cx = w / 2, cy = h / 2;
        for (int i = 3; i <= 6; i++) {
            g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, c);
            g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, c);
            g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, c);
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
        }
    }

    private static void reload(GuiGraphics g, int w, int h, long now) {
        if (now >= CData.reloadUntil || CData.reloadTicks <= 0) return;
        float f = 1 - (CData.reloadUntil - now) / (CData.reloadTicks * 50f);
        g.fill(w / 2 - 20, h / 2 + 12, w / 2 + 20, h / 2 + 14, 0xFF222222);
        g.fill(w / 2 - 20, h / 2 + 12, w / 2 - 20 + (int) (40 * f), h / 2 + 14, WHITE);
    }

    private static void notices(GuiGraphics g, Font font, int w, long now) {
        CData.NOTICES.removeIf(n -> now - n.at() > NOTICE_MS);
        int y = 20 + (CData.inRaid ? CData.EXTRACTS.size() * 13 + 6 : 0);
        for (CData.Notice n : CData.NOTICES) {
            float life = 1 - (now - n.at()) / (float) NOTICE_MS;
            int alpha = Math.max(16, (int) (255 * Math.min(1, life * 3)));
            g.drawString(font, n.text(), w - font.width(n.text()) - 4, y, (alpha << 24) | (n.color() & 0xFFFFFF));
            y += 11;
        }
    }

    private static void raidEnd(GuiGraphics g, Font font, int w, int h, long now) {
        if (CData.raidEnd == null) return;
        if (CData.raidEnd != shownEnd) {
            shownEnd = CData.raidEnd;
            shownEndAt = now;
        }
        if (now - shownEndAt > END_MS) {
            CData.raidEnd = null;
            return;
        }
        JsonObject o = CData.raidEnd;
        String status = o.get("status").getAsString();
        String title = switch (status) {
            case "EXTRACTED" -> "EXTRAIT";
            case "KIA" -> "TUE AU COMBAT";
            default -> "PORTE DISPARU";
        };
        int color = "EXTRACTED".equals(status) ? GREEN : RED;
        int time = o.get("time").getAsInt();
        int bx = w / 2 - 90, by = h / 4;
        g.fill(bx, by, bx + 180, by + 70, 0xC0000000);
        g.drawCenteredString(font, title, w / 2, by + 6, color);
        g.drawCenteredString(font, String.format("Duree %d:%02d   Kills %d", time / 60, time % 60,
                o.get("kills").getAsInt()), w / 2, by + 24, WHITE);
        g.drawCenteredString(font, "+" + o.get("xp").getAsInt() + " XP   +" + o.get("earned").getAsInt() + " koens",
                w / 2, by + 38, YELLOW);
        g.drawCenteredString(font, "Niveau " + CData.level + "  -  " + CData.profile.koens + " koens", w / 2, by + 52, GREY);
    }

    private static void flash(GuiGraphics g, int w, int h, long now) {
        if (now >= CData.flashUntil || CData.flashTicks <= 0) return;
        float f = (CData.flashUntil - now) / (CData.flashTicks * 50f);
        g.fill(0, 0, w, h, ((int) (255 * Mth.clamp(f, 0, 1)) << 24) | 0xFFFFFF);
    }
}

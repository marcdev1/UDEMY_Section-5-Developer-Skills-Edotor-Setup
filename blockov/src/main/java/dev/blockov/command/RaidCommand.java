package dev.blockov.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.blockov.inv.Profile;
import dev.blockov.net.Net;
import dev.blockov.raid.Extract;
import dev.blockov.raid.RaidManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * /raid start [minutes] | leave | profile
 * /raid extract add <nom> <cout> | remove <nom> | list   (admin)
 * /raid spawn add | clear                                (admin)
 */
public final class RaidCommand {
    private RaidCommand() {}

    /** Niveau operateur 2. Seul endroit a adapter si l'API de permissions change. */
    private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("raid")
                .then(Commands.literal("start")
                        .executes(c -> start(c, 20))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 120))
                                .executes(c -> start(c, IntegerArgumentType.getInteger(c, "minutes")))))
                .then(Commands.literal("leave").executes(RaidCommand::leave))
                .then(Commands.literal("profile").executes(RaidCommand::profile))
                .then(Commands.literal("extract").requires(ADMIN)
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                                .executes(RaidCommand::extractAdd))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(RaidCommand::extractRemove)))
                        .then(Commands.literal("list").executes(RaidCommand::extractList)))
                .then(Commands.literal("spawn").requires(ADMIN)
                        .then(Commands.literal("add").executes(RaidCommand::spawnAdd))
                        .then(Commands.literal("clear").executes(RaidCommand::spawnClear))));
    }

    private static int start(CommandContext<CommandSourceStack> c, int minutes) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RaidManager rm = RaidManager.INSTANCE;
        if (rm.store.extracts.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Aucune extraction definie (/raid extract add)"));
            return 0;
        }
        if (!rm.start(p, minutes)) {
            c.getSource().sendFailure(Component.literal("Deja en raid"));
            return 0;
        }
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RaidManager rm = RaidManager.INSTANCE;
        if (rm.get(p) == null) {
            c.getSource().sendFailure(Component.literal("Pas en raid"));
            return 0;
        }
        // Abandon : compte comme porte disparu, le butin est perdu
        p.getInventory().clearContent();
        rm.end(p, "MIA");
        return 1;
    }

    private static int profile(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Profile prof = RaidManager.INSTANCE.profile(p);
        c.getSource().sendSuccess(() -> Component.literal(String.format(
                "Niveau %d (%d XP) | %d koens | Raids %d | Extractions %d (%.0f%%) | Morts %d | Kills %d",
                prof.level(), prof.xp, prof.koens, prof.raids, prof.extracts, prof.survivalRate() * 100,
                prof.deaths, prof.kills)), false);
        Net.profile(p, prof);
        return 1;
    }

    private static int extractAdd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        int cost = IntegerArgumentType.getInteger(c, "cost");
        RaidManager rm = RaidManager.INSTANCE;
        rm.store.extracts.removeIf(e -> e.name().equalsIgnoreCase(name));
        rm.store.extracts.add(new Extract(name, p.getBlockX(), p.getBlockZ(), cost));
        rm.store.save();
        c.getSource().sendSuccess(() -> Component.literal("Extraction " + name + " ajoutee (" + cost + " koens)"), true);
        return 1;
    }

    private static int extractRemove(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name");
        RaidManager rm = RaidManager.INSTANCE;
        if (!rm.store.extracts.removeIf(e -> e.name().equalsIgnoreCase(name))) {
            c.getSource().sendFailure(Component.literal("Extraction inconnue : " + name));
            return 0;
        }
        rm.store.save();
        c.getSource().sendSuccess(() -> Component.literal("Extraction " + name + " supprimee"), true);
        return 1;
    }

    private static int extractList(CommandContext<CommandSourceStack> c) {
        var list = RaidManager.INSTANCE.store.extracts;
        if (list.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("Aucune extraction"), false);
        }
        for (Extract e : list) {
            c.getSource().sendSuccess(() -> Component.literal(
                    "- " + e.name() + " @ " + e.x() + ", " + e.z() + " : " + e.cost() + " koens"), false);
        }
        return list.size();
    }

    private static int spawnAdd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RaidManager rm = RaidManager.INSTANCE;
        rm.store.spawns.add(p.position());
        rm.store.save();
        c.getSource().sendSuccess(() -> Component.literal("Point d'apparition #" + rm.store.spawns.size() + " ajoute"), true);
        return 1;
    }

    private static int spawnClear(CommandContext<CommandSourceStack> c) {
        RaidManager rm = RaidManager.INSTANCE;
        rm.store.spawns.clear();
        rm.store.save();
        c.getSource().sendSuccess(() -> Component.literal("Points d'apparition effaces"), true);
        return 1;
    }
}

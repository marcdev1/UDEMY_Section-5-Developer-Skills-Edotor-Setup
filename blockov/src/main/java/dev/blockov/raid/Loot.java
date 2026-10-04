package dev.blockov.raid;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Valeur (en koens) et poids (en kg) des objets. */
public final class Loot {
    /** Objets de valeur revendus automatiquement a l'extraction. */
    private static final Map<Item, Integer> VALUABLES = new HashMap<>();

    static {
        VALUABLES.put(Items.NETHERITE_INGOT, 4000);
        VALUABLES.put(Items.NETHERITE_SCRAP, 900);
        VALUABLES.put(Items.DIAMOND, 500);
        VALUABLES.put(Items.EMERALD, 300);
        VALUABLES.put(Items.GOLD_INGOT, 120);
        VALUABLES.put(Items.GOLD_NUGGET, 12);
        VALUABLES.put(Items.IRON_INGOT, 50);
        VALUABLES.put(Items.COPPER_INGOT, 15);
        VALUABLES.put(Items.AMETHYST_SHARD, 40);
        VALUABLES.put(Items.ENDER_PEARL, 150);
        VALUABLES.put(Items.TOTEM_OF_UNDYING, 2500);
    }

    private Loot() {}

    public static int value(ItemStack stack) {
        Integer v = VALUABLES.get(stack.getItem());
        return v == null ? 0 : v * stack.getCount();
    }

    public static float weight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // Objets non empilables (armes, armures, outils) : lourds ; le reste : leger
        float unit = stack.getMaxStackSize() == 1 ? 1.5f : stack.getMaxStackSize() <= 16 ? 0.25f : 0.05f;
        return unit * stack.getCount();
    }

    public static float weight(Inventory inv) {
        float w = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) w += weight(inv.getItem(i));
        return w;
    }

    /** Retire les objets de valeur de l'inventaire et renvoie leur prix total. */
    public static int sellValuables(Inventory inv) {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int v = value(s);
            if (v > 0) {
                total += v;
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return total;
    }
}

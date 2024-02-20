package me.lamalditag.hungergamesulp.utils;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import net.md_5.bungee.api.ChatMessageType;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ChestFill {
    public static ItemStack potion(ItemStack item,Map<?,?> itemMap){
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        assert meta != null;
        Object effectsObj = itemMap.get("effects");
        if (effectsObj instanceof List<?> effectsList) {
            for (Object effectObj : effectsList) {
                if (effectObj instanceof Map<?, ?> effectMap) {
                    String potionTypeString = (String) effectMap.get("type");
                    int level = (int) effectMap.get("level");
                    Object dur = effectMap.get("duration");
                    int duration = dur != null ? (int)dur : 0;

                    if(effectsList.size()==1){
                        PotionType potionType = PotionType.valueOf(potionTypeString);
                        PotionData potionData = new PotionData(potionType, duration > 0 && potionType.isExtendable(), level > 1 && duration <= 0 && potionType.isUpgradeable());
                        
                        meta.setBasePotionData(potionData);
                    }else{
                        PotionEffectType potionEffectType = PotionType.valueOf(potionTypeString).getEffectType();
                        meta.addCustomEffect(potionEffectType.createEffect(duration, level), false);
                    }
                }
            }
        }
        if(itemMap.containsKey("color")) {
            int rgb = (int)itemMap.get("color");
            meta.setColor(Color.fromRGB(rgb));
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack enchantedItem(ItemStack item, Map<?,?> itemMap){
        Object enchantmentsObj = itemMap.get("enchantments");
        if (enchantmentsObj instanceof List<?> enchantmentsList) {
            for (Object enchantmentObj : enchantmentsList) {
                if (enchantmentObj instanceof Map<?, ?> enchantmentMap) {
                    String enchantmentType = (String) enchantmentMap.get("type");
                    int level = (int) enchantmentMap.get("level");
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType.toLowerCase()));
                    if (enchantment != null) {
                        if (item.getType() == Material.ENCHANTED_BOOK) {
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                            assert meta != null;
                            meta.addStoredEnchant(enchantment, level, true);
                            item.setItemMeta(meta);
                        } else {
                            item.addEnchantment(enchantment, level);
                        }
                    }
                }
            }
        }
        return item;
    }

    public static ItemStack firework(ItemStack item,Map<?,?> itemMap,Map<String, Color> colorMap){
        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        int power = (int) itemMap.get("power");
        assert meta != null;
        meta.setPower(power);
        Object effectsObj = itemMap.get("effects");
        if (effectsObj instanceof List<?> effectsList) {
            for (Object effectObj : effectsList) {
                if (effectObj instanceof Map<?, ?> effectMap) {
                    String effectType = (String) effectMap.get("type");
                    Object colorsObj = effectMap.get("colors");
                    if (colorsObj instanceof List<?> colorsList) {
                        List<Color> colors = colorsList.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                .collect(Collectors.toList());
                        Object fadeColorsObj = effectMap.get("fade-colors");
                        if (fadeColorsObj instanceof List<?> fadeColorsList) {
                            List<Color> fadeColors = fadeColorsList.stream()
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .map(colorName -> colorMap.getOrDefault(colorName.toUpperCase(), Color.RED))
                                    .collect(Collectors.toList());
                            boolean flicker = (boolean) effectMap.get("flicker");
                            boolean trail = (boolean) effectMap.get("trail");
                            FireworkEffect effect = FireworkEffect.builder()
                                    .with(FireworkEffect.Type.valueOf(effectType))
                                    .withColor(colors)
                                    .withFade(fadeColors)
                                    .flicker(flicker)
                                    .trail(trail)
                                    .build();
                            meta.addEffect(effect);
                        }
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack item(ItemStack item,Map<?,?> itemMap){
        ItemMeta meta = (ItemMeta)item.getItemMeta();
        if(itemMap.containsKey("name")) {
            meta.setDisplayName(ChatColor.RESET + "" + (String)itemMap.get("name")+ "" + ChatColor.RESET );
        }
        item.setItemMeta(meta);
        return item;
    }
}

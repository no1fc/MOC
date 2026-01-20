package me.user.moc.ability.impl;

import me.user.moc.ability.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Ranga extends Ability {

    private final Map<UUID, UUID> ownerToWolf = new HashMap<>(); // Owner UUID -> Wolf UUID
    private final Map<UUID, UUID> wolfToOwner = new HashMap<>(); // Wolf UUID -> Owner UUID

    public Ranga(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "란가";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§b전투 ● 란가(전생슬)",
                "§f템페스트 스타 울프 란가가 도와줍니다.",
                "§f[능력]",
                "§f- 거대 늑대 소환 (체력60/공격10/이속II)",
                "§f- 공격 시 40% 확률로 번개 (데스 스톰!)",
                "§f- 란가 사망 시 주인에게 이속II 영구 부여");
    }

    @Override
    public void giveItem(Player p) {
        Bukkit.broadcast(Component.text("§7[Debug] giveItem called for " + p.getName()));
        summonRanga(p);
        p.sendMessage("§b[MOC] §f그림자 속에서 란가가 나타납니다!");
    }

    private void summonRanga(Player p) {
        World world = p.getWorld();
        Bukkit.broadcast(Component.text("§7[Debug] Summoning wolf at " + p.getLocation()));
        Wolf wolf = (Wolf) world.spawnEntity(p.getLocation(), EntityType.WOLF);

        // 1. 기본 설정
        wolf.setTamed(true);
        wolf.setOwner(p);

        try {
            wolf.setVariant(Wolf.Variant.BLACK);
        } catch (Throwable t) {
            Bukkit.broadcast(Component.text("§7[Debug] Variant failed: " + t.getMessage()));
        }

        wolf.customName(Component.text("§b란가"));
        wolf.setCustomNameVisible(true);

        // 2. 능력치 설정
        // IMPORTANT: Do NOT use GENERIC_MAX_HEALTH as fallback, it causes compile error
        // if symbol is missing.
        try {
            AttributeInstance maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null)
                maxHealth.setBaseValue(60.0);
            wolf.setHealth(60.0);
        } catch (Throwable t) {
            Bukkit.broadcast(Component.text("§7[Debug] Health failed: " + t.getMessage()));
        }

        try {
            // IMPORTANT: Do NOT use GENERIC_ATTACK_DAMAGE as fallback.
            AttributeInstance attackDamage = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackDamage != null)
                attackDamage.setBaseValue(10.0);
        } catch (Throwable t) {
            Bukkit.broadcast(Component.text("§7[Debug] Attack failed: " + t.getMessage()));
        }

        // 3. 장비
        if (wolf.getEquipment() != null) {
            try {
                wolf.getEquipment().setChestplate(new ItemStack(Material.WOLF_ARMOR));
            } catch (Throwable t) {
                Bukkit.broadcast(Component.text("§7[Debug] Armor failed: " + t.getMessage()));
            }
        }

        wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));

        // 4. 이펙트
        world.spawnParticle(Particle.LARGE_SMOKE, wolf.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        try {
            // Ensure proper Sound enum is used
            world.playSound(wolf.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, 0.5f);
        } catch (Throwable ignored) {
        }

        // 5. 추적 등록
        ownerToWolf.put(p.getUniqueId(), wolf.getUniqueId());
        wolfToOwner.put(wolf.getUniqueId(), p.getUniqueId());
        Bukkit.broadcast(
                Component.text("§7[Debug] Wolf registered. Owner: " + p.getName() + ", WolfID: " + wolf.getUniqueId()));
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Wolf wolf))
            return;

        if (!wolfToOwner.containsKey(wolf.getUniqueId()))
            return;

        // Debug
        Bukkit.broadcast(Component.text("§7[Debug] Ranga attacked!"));

        if (Math.random() < 0.4) {
            World world = wolf.getWorld();
            world.strikeLightning(e.getEntity().getLocation());

            UUID ownerId = wolfToOwner.get(wolf.getUniqueId());
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner != null) {
                owner.sendMessage("§b§l데스 스톰!");
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Wolf wolf) {
            if (wolfToOwner.containsKey(wolf.getUniqueId())) {
                Bukkit.broadcast(Component.text("§7[Debug] Ranga died!"));
            }

            UUID ownerId = wolfToOwner.remove(wolf.getUniqueId());
            if (ownerId == null)
                return;
            ownerToWolf.remove(ownerId);

            Player owner = Bukkit.getPlayer(ownerId);
            if (owner != null) {
                owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
                owner.sendMessage("§b[MOC] 란가가 당신의 그림자에 깃들었습니다. (이속 증가)");

                try {
                    // Ensure ALL CAPS for enum
                    owner.playSound(owner.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                } catch (Throwable ignored) {
                }

                owner.getWorld().spawnParticle(Particle.LARGE_SMOKE, wolf.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}

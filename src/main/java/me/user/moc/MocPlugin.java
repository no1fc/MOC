package me.user.moc;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.EntityDamageByEntityEvent; // 올라프 대미지용
import org.bukkit.entity.Snowball;                      // 올라프 눈구멍(도끼)용
import org.bukkit.event.block.Action;                   // 클릭 감지용

import java.util.*;

public final class MocPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, String> playerAbilities = new HashMap<>();
    private final Map<UUID, Horse> activeBikes = new HashMap<>();
    private BukkitTask borderTask;
    private BukkitTask damageTask;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Optional.ofNullable(getCommand("moc")).ifPresent(cmd -> cmd.setExecutor(this));
        getLogger().info("MOC 플러그인 정리 완료!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) return false;
        if (args.length == 0) return false;

        // --- 게임 시작 (/moc start) ---
        if (args[0].equalsIgnoreCase("start")) {
            Location center = admin.getLocation();
            World world = center.getWorld();
            int floorY = center.getBlockY() - 1;

            // 환경 설정
            world.setTime(6000);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setStorm(false);
            world.setDifficulty(Difficulty.NORMAL);

            // 경기장 설정
            Location emeraldLoc = center.getBlock().getRelative(0, -1, 0).getLocation();
            emeraldLoc.getBlock().setType(Material.EMERALD_BLOCK);
            world.getWorldBorder().setCenter(center);
            world.getWorldBorder().setSize(120);

            admin.sendMessage("§e[MOC] §f경기장 건설 및 게임을 시작합니다!");
            generateCircleFloor(center, 60, floorY, emeraldLoc);
            setupPlayers();

            // 기존 스케줄러 초기화 후 재등록
            if (borderTask != null) borderTask.cancel();
            if (damageTask != null) damageTask.cancel();

            borderTask = new BukkitRunnable() {
                @Override
                public void run() { startCustomBorderShrink(world); }
            }.runTaskLater(this, 20L * 60 * 5); // 5분 뒤 수축 시작

            startBorderDamageTask(world);
            return true;
        }

        // --- 게임 종료 (/moc stop) ---
        if (args[0].equalsIgnoreCase("stop")) {
            World world = admin.getWorld();
            if (borderTask != null) borderTask.cancel();
            if (damageTask != null) damageTask.cancel();

            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            world.getWorldBorder().setSize(30000000);

            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§c§l[MOC] §f게임이 종료되었습니다!");
            Bukkit.broadcastMessage(" ");

            playerAbilities.clear();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().clear();
                AttributeInstance health = p.getAttribute(Attribute.MAX_HEALTH);
                if (health != null) health.setBaseValue(20.0);
                p.setHealth(20.0);
                p.sendTitle("§c§lGAME OVER", "§f게임이 종료되었습니다.", 10, 40, 10);
            }
            activeBikes.values().forEach(Entity::remove);
            activeBikes.clear();
            return true;
        }
        return false;
    }

    private void setupPlayers() {
        List<String> abilityList = new ArrayList<>(Arrays.asList("우에키", "올라프", "미다스", "매그너스"));
        Collections.shuffle(abilityList);
        int index = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            String assigned = abilityList.get(index % abilityList.size());
            playerAbilities.put(p.getUniqueId(), assigned);

            AttributeInstance maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(60.0);
                p.setHealth(60.0);
            }
            giveItems(p, assigned);
            p.sendTitle("§6§l게임 시작!", "§f능력: " + assigned, 10, 70, 20);
            index++;
        }
    }

    private void giveItems(Player p, String ability) {
        p.getInventory().clear();
        p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 64));
        p.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
        p.getInventory().addItem(new ItemStack(Material.GLASS, 10));
        p.getInventory().addItem(new ItemStack(Material.IRON_CHESTPLATE));

        if (ability.equals("우에키")) p.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 16));
        else if (ability.equals("올라프")) p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 9));
        else if (ability.equals("미다스")) p.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 64));
        else if (ability.equals("매그너스")) {
            ItemStack bike = new ItemStack(Material.GRAY_DYE);
            p.getInventory().addItem(bike);
        }
    }

    private void generateCircleFloor(Location center, int radius, int targetY, Location teleportDest) {
        World world = center.getWorld();
        int cx = center.getBlockX(), cz = center.getBlockZ();
        long radiusSq = (long) radius * radius;
        int emX = teleportDest.getBlockX(), emZ = teleportDest.getBlockZ();

        new BukkitRunnable() {
            int x = cx - radius;
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    if (x > cx + radius) {
                        world.getEntitiesByClass(Item.class).forEach(Entity::remove);
                        Bukkit.getOnlinePlayers().forEach(p -> p.teleport(teleportDest.clone().add(0.5, 1.0, 0.5)));
                        this.cancel();
                        return;
                    }
                    long dx = (long) (x - cx) * (x - cx);
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        if (dx + (long) (z - cz) * (z - cz) <= radiusSq) {
                            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                Block b = world.getBlockAt(x, y, z);
                                if (y == targetY) { if (x != emX || z != emZ) b.setType(Material.BEDROCK, false); }
                                else if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                            }
                        }
                    }
                    x++;
                }
            }
        }.runTaskTimer(this, 0, 1);
    }












    @EventHandler
    public void onMagnusAbility(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!"매그너스".equals(playerAbilities.get(p.getUniqueId()))) return;
        if (event.getItem() != null && event.getItem().getType() == Material.GRAY_DYE && event.getAction().name().contains("RIGHT")) {
            if (activeBikes.containsKey(p.getUniqueId())) return;

            Horse bike = (Horse) p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
            bike.setTamed(true);
            bike.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            Optional.ofNullable(bike.getAttribute(Attribute.MOVEMENT_SPEED)).ifPresent(a -> a.setBaseValue(0.5));
            bike.addPassenger(p);
            activeBikes.put(p.getUniqueId(), bike);

            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    t++;
                    if (t > 200 || bike.getPassengers().isEmpty() || bike.isDead()) {
                        explodeBike(bike, p);
                        activeBikes.remove(p.getUniqueId());
                        this.cancel();
                        return;
                    }
                    if (bike.getLocation().add(bike.getLocation().getDirection().multiply(1.2)).getBlock().getType().isSolid()) {
                        explodeBike(bike, p);
                        activeBikes.remove(p.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0, 1);
        }
    }

    private void explodeBike(Horse bike, Player owner) {
        Location loc = bike.getLocation();
        new BukkitRunnable() {
            int m = 0;
            @Override
            public void run() {
                m++;
                loc.add(loc.getDirection().multiply(1.0));
                if (m >= 10 || loc.getBlock().getType().isSolid()) {
                    loc.getWorld().createExplosion(loc, 4.0f, false, false);
                    owner.setNoDamageTicks(20);
                    bike.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void startCustomBorderShrink(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerAbilities.isEmpty()) { this.cancel(); return; }
                double size = world.getWorldBorder().getSize();
                if (size <= 1) { this.cancel(); return; }
                world.getWorldBorder().setSize(size - 2, 1);
            }
        }.runTaskTimer(this, 0, 60L);
    }

    private void startBorderDamageTask(World world) {
        damageTask = new BukkitRunnable() {
            @Override
            public void run() {
                WorldBorder b = world.getWorldBorder();
                double s = b.getSize() / 2.0; // 반지름으로 계산해야 정확함
                Location c = b.getCenter();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location l = p.getLocation();
                    if (Math.abs(l.getX() - c.getX()) > s || Math.abs(l.getZ() - c.getZ()) > s) {
                        p.damage(6.0);
                        p.sendMessage("§c§l[자기장 밖 대미지]");
                    }
                }
            }
        }.runTaskTimer(this, 0, 20L);
    }

    @EventHandler
    public void onUeki(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!"우에키".equals(playerAbilities.get(p.getUniqueId()))) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType() == Material.OAK_SAPLING) {
            e.getClickedBlock().getRelative(0, 1, 0).setType(Material.OAK_LOG);
            e.getClickedBlock().getRelative(0, 2, 0).setType(Material.OAK_LOG);
            e.getClickedBlock().getRelative(0, 3, 0).setType(Material.AZALEA_LEAVES);
        }
    }



    @EventHandler
    public void onOlaf(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!"올라프".equals(playerAbilities.get(p.getUniqueId()))) return;
        if (e.getItem() != null && e.getItem().getType() == Material.IRON_AXE && e.getAction().name().contains("RIGHT")) {
            Snowball axe = p.launchProjectile(Snowball.class);
            axe.setItem(new ItemStack(Material.IRON_AXE));
            axe.setCustomName("olaf_axe");
        }
    }

    @EventHandler
    public void onAxeHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Snowball s && "olaf_axe".equals(s.getCustomName())) {
            e.setDamage(10.0); // 하트 5칸 대미지
        }
    }


    @EventHandler
    public void onMidas(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!"미다스".equals(playerAbilities.get(p.getUniqueId()))) return;
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType() == Material.GOLD_INGOT) {
            e.getClickedBlock().setType(Material.GOLD_BLOCK);
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (e.getBlock().getType() == Material.BEDROCK) e.setCancelled(true); }
    @EventHandler public void onDura(PlayerItemDamageEvent e) { if (playerAbilities.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }
}
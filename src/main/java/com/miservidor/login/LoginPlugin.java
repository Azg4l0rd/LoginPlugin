package com.miservidor.login;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class LoginPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private File usersFile;
    private FileConfiguration usersConfig;
    private final HashMap<UUID, Boolean> loggedInUsers = new HashMap<>();
    private final HashMap<UUID, Location> frozenLocations = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("register").setExecutor(this);
        getCommand("login").setExecutor(this);
        
        usersFile = new File(getDataFolder(), "users.yml");
        if (!usersFile.exists()) {
            try {
                usersFile.getParentFile().mkdirs();
                usersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        usersConfig = YamlConfiguration.loadConfiguration(usersFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (label.equalsIgnoreCase("register")) {
            if (usersConfig.contains(uuid.toString())) {
                player.sendMessage("§cYa estás registrado. Usa /login <contraseña> para iniciar sesión.");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cUso: /register <contraseña>");
                return true;
            }
            usersConfig.set(uuid.toString(), args[0]);
            saveUsersFile();
            loggedInUsers.put(uuid, true);
            player.sendMessage("§aRegistrado exitosamente. Ahora estás logueado.");
            return true;
        }
        
        if (label.equalsIgnoreCase("login")) {
            if (!usersConfig.contains(uuid.toString())) {
                player.sendMessage("§cNo estás registrado. Usa /register <contraseña> para registrarte.");
                return true;
            }
            if (args.length != 1 || !usersConfig.getString(uuid.toString()).equals(args[0])) {
                player.sendMessage("§cContraseña incorrecta.");
                return true;
            }
            loggedInUsers.put(uuid, true);
            player.sendMessage("§aHas iniciado sesión correctamente.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        loggedInUsers.put(uuid, false);
        frozenLocations.put(uuid, player.getLocation());
        player.sendMessage("§ePor favor, usa /login <contraseña> o /register <contraseña>.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!loggedInUsers.getOrDefault(player.getUniqueId(), false)) {
            event.setTo(frozenLocations.get(player.getUniqueId()));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!loggedInUsers.getOrDefault(event.getPlayer().getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!loggedInUsers.getOrDefault(event.getPlayer().getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (!loggedInUsers.getOrDefault(player.getUniqueId(), false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!loggedInUsers.getOrDefault(player.getUniqueId(), false)) {
                event.setCancelled(true);
            }
        }
    }
    
    private void saveUsersFile() {
        try {
            usersConfig.save(usersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

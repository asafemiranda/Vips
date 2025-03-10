package me.Vips.vip;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VipPlugin extends JavaPlugin {
    protected Map<UUID, String> vipPlayers;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        vipPlayers = new HashMap<>();
        loadVipData();
        getCommand("darvip").setExecutor(new VipCommand(this));
        getLogger().info("VIP Plugin ativado!");
    }

    @Override
    public void onDisable() {
        saveVipData();
    }

    private void loadVipData() {
        if (config.contains("vipPlayers")) {
            for (String key : config.getConfigurationSection("vipPlayers").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String vip = config.getString("vipPlayers." + key + ".type");
                vipPlayers.put(uuid, vip);
            }
        }
    }

    private void saveVipData() {
        for (Map.Entry<UUID, String> entry : vipPlayers.entrySet()) {
            config.set("vipPlayers." + entry.getKey().toString() + ".type", entry.getValue());
        }
        saveConfig();
    }
}

class VipCommand implements CommandExecutor {
    private final VipPlugin plugin;

    public VipCommand(VipPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Somente jogadores podem usar este comando!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("vip.dar")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para isso!");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso correto: /darvip <jogador> <vip> <duração (dias)>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
            return true;
        }

        String vip = args[1].toLowerCase();
        if (!plugin.getConfig().getConfigurationSection("vips").contains(vip)) {
            player.sendMessage(ChatColor.RED + "VIP não encontrado na configuração!");
            return true;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[2]);
            if (duration <= 0) {
                player.sendMessage(ChatColor.RED + "A duração deve ser um número positivo!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Duração inválida! Use um número inteiro de dias.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        long expiration = Instant.now().getEpochSecond() + (duration * 86400L);

        plugin.vipPlayers.put(targetUUID, vip);
        plugin.getConfig().set("vipPlayers." + targetUUID.toString() + ".type", vip);
        plugin.getConfig().set("vipPlayers." + targetUUID.toString() + ".expires", expiration);
        plugin.saveConfig();

        target.sendMessage(ChatColor.GOLD + "Você recebeu o VIP " + ChatColor.AQUA + vip + " por " + duration + " dias!");
        player.sendMessage(ChatColor.GREEN + "VIP " + vip + " concedido para " + target.getName() + " por " + duration + " dias!");
        return true;
    }
}

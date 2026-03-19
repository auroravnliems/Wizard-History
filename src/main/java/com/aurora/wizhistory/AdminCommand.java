package com.aurora.wizhistory;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final WizardHistory plugin;

    public AdminCommand(WizardHistory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {

        if (!sender.hasPermission("wizhistory.admin")) {
            sender.sendMessage(color(msg("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reloadConfig();
                plugin.getCommandFileReader().reload();
                sender.sendMessage(color(msg("reload")));
            }

            case "clear" -> {
                if (args.length < 2) { sendUsage(sender, label); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                UUID uuid;
                String pName = args[1];
                if (target != null) {
                    uuid = target.getUniqueId();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                    uuid = offline.getUniqueId();
                }
                if (args.length >= 3 && args[2].equalsIgnoreCase("rolling")) {
                    plugin.getHistoryManager().clearRollingHistory(uuid);
                    sender.sendMessage(color(prefix() + "&aĐã xoá lịch sử gần đây của &e" + pName + "&a. (Số liệu toàn thời gian vẫn giữ nguyên)"));
                } else {
                    plugin.getHistoryManager().clearHistory(uuid);
                    sender.sendMessage(color(
                            msg("history-cleared").replace("%player%", pName)
                    ));
                }
            }

            case "view" -> {
                if (args.length < 2) { sendUsage(sender, label); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                UUID uuid;
                String name;
                if (target != null) {
                    uuid = target.getUniqueId();
                    name = target.getName();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                    uuid = offline.getUniqueId();
                    name = args[1];
                }

                int page = 1;
                if (args.length >= 3) {
                    try { page = Math.max(1, Integer.parseInt(args[2])); }
                    catch (NumberFormatException ignored) {}
                }

                List<HistoryEntry> history = plugin.getHistoryManager().getHistory(uuid);
                if (history.isEmpty()) {
                    sender.sendMessage(color(msg("no-history")));
                    return true;
                }

                int perPage = 10;
                int maxPage  = (int) Math.ceil((double) history.size() / perPage);
                page = Math.min(page, maxPage);

                sender.sendMessage(color(
                        msg("page-header")
                                .replace("%player%", name)
                                .replace("%page%", String.valueOf(page))
                                .replace("%max%", String.valueOf(maxPage))
                ));

                String pattern = plugin.getConfig().getString("date-format", "yyyy/MM/dd HH:mm");
                SimpleDateFormat sdf;
                try { sdf = new SimpleDateFormat(pattern); }
                catch (IllegalArgumentException e) { sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm"); }

                int start = (page - 1) * perPage;
                int end   = Math.min(start + perPage, history.size());

                for (int i = start; i < end; i++) {
                    HistoryEntry entry = history.get(i);
                    String displayName = plugin.getConfig()
                            .getString("display-names." + entry.getCommandName(),
                                    entry.getCommandName());
                    String line = msg("page-entry")
                            .replace("%index%",        String.valueOf(i + 1))
                            .replace("%display_name%", displayName)
                            .replace("%time%",         sdf.format(new Date(entry.getTimestamp())));
                    sender.sendMessage(color(line));
                }
            }

            default -> sendUsage(sender, label);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (!sender.hasPermission("wizhistory.admin")) return List.of();

        if (args.length == 1) {
            return filter(args[0], "reload", "clear", "view");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("clear") ||
                args[0].equalsIgnoreCase("view"))) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return filter(args[1], names.toArray(new String[0]));
        }
        return List.of();
    }


    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(color("&7/" + label + " reload"));
        sender.sendMessage(color("&7/" + label + " clear <player> [rolling]"));
        sender.sendMessage(color("&7/" + label + " view <player> [page]"));
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&bWizHistory&8] ");
    }

    private String msg(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bWizHistory&8] ");
        String val    = plugin.getConfig().getString("messages." + key, "&c[Missing: " + key + "]");
        return prefix + val;
    }

    private static String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    private static List<String> filter(String arg, String... options) {
        List<String> result = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(arg.toLowerCase())) result.add(o);
        }
        return result;
    }
}

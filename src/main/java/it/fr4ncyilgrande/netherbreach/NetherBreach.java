package it.fr4ncyilgrande.netherbreach;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plugin entry point. Uses Bukkit/Spigot APIs only, with no networking or NMS. */
public final class NetherBreach extends JavaPlugin implements TabExecutor {

    private RoofPerforatorManager perforatorManager;
    private String languageMode = "auto";
    private String itemLanguageMode = "holder";
    private PluginLanguage fallbackLanguage = PluginLanguage.EN;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLanguageSettings();

        perforatorManager = new RoofPerforatorManager(this);
        perforatorManager.reloadValues();
        rebuildRecipe();

        getServer().getPluginManager().registerEvents(new NetherBreachListener(perforatorManager), this);

        PluginCommand netherBreachCommand = getCommand("netherbreach");
        if (netherBreachCommand == null) {
            severe("netherbreach command missing from plugin.yml: plugin disabled.",
                    "Comando netherbreach assente in plugin.yml: plugin disabilitato.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        netherBreachCommand.setExecutor(this);
        netherBreachCommand.setTabCompleter(this);
        info("NetherBreach enabled. Owner: Fr4ncyILGrande.",
                "NetherBreach avviato. Proprietario: Fr4ncyILGrande.");
    }

    @Override
    public void onDisable() {
        if (perforatorManager != null) {
            perforatorManager.removeRecipe();
        }
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        reloadLanguageSettings();
        perforatorManager.reloadValues();
        rebuildRecipe();
        Bukkit.getOnlinePlayers().forEach(perforatorManager::sanitizeInventory);
    }

    private void rebuildRecipe() {
        perforatorManager.removeRecipe();
        if (!getConfig().getBoolean("crafting.enabled", true)) {
            info("Crafting recipe disabled in the configuration.",
                    "La ricetta è disattivata nella configurazione.");
            return;
        }

        perforatorManager.registerRecipe();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("netherbreach")) {
            return false;
        }

        if (args.length == 0) {
            send(sender, "messages.usage");
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("ricarica") || subcommand.equals("reload")) {
            if (args.length != 1) {
                send(sender, "messages.usage");
                return true;
            }
            if (!sender.hasPermission("netherbreach.admin")) {
                send(sender, "messages.no-permission");
                return true;
            }
            reloadPluginConfiguration();
            send(sender, "messages.config-reloaded");
            return true;
        }

        if (subcommand.equals("dai") || subcommand.equals("give")) {
            if (args.length > 2) {
                send(sender, "messages.usage");
                return true;
            }
            if (!sender.hasPermission("netherbreach.give")) {
                send(sender, "messages.no-permission");
                return true;
            }

            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "messages.player-not-found", "{player}", args[1]);
                    return true;
                }
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                send(sender, "messages.player-only");
                return true;
            }

            ItemStack item = perforatorManager.createItem(target);
            var leftovers = target.getInventory().addItem(item);
            leftovers.values().forEach(leftover ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));

            send(target, "messages.item-received");
            if (sender != target) {
                send(sender, "messages.item-given", "{player}", target.getName());
            }
            return true;
        }

        send(sender, "messages.usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("netherbreach")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("netherbreach.give")) {
                completions.add("give");
                completions.add("dai");
            }
            if (sender.hasPermission("netherbreach.admin")) {
                completions.add("reload");
                completions.add("ricarica");
            }
            return completions.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("dai") || args[0].equalsIgnoreCase("give"))
                && sender.hasPermission("netherbreach.give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT)
                            .startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private void send(CommandSender sender, String path, String... replacements) {
        String message = translatedMessage(resolveLanguage(sender), path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendMessage(Player player, String path, String... replacements) {
        send(player, path, replacements);
    }

    void info(String english, String italian) {
        getLogger().info(consoleText(english, italian));
    }

    void warning(String english, String italian) {
        getLogger().warning(consoleText(english, italian));
    }

    void severe(String english, String italian) {
        getLogger().severe(consoleText(english, italian));
    }

    public PluginLanguage resolveItemLanguage(Player holder) {
        return resolveItemLanguage(holder, holder == null ? null : holder.getLocale());
    }

    public PluginLanguage resolveItemLanguage(Player holder, String clientLocale) {
        PluginLanguage forced = configuredLanguage();
        if (forced != null) {
            return forced;
        }
        if ("holder".equals(itemLanguageMode) && holder != null) {
            return PluginLanguage.fromClientLocale(clientLocale, fallbackLanguage);
        }
        return fallbackLanguage;
    }

    public String localizedItemName(PluginLanguage language) {
        return localizedString("item", language, "name", "&6Roof Perforator");
    }

    public List<String> localizedItemLore(PluginLanguage language) {
        String localizedPath = "item." + language.configKey() + ".lore";
        if (getConfig().isSet(localizedPath)) {
            return getConfig().getStringList(localizedPath);
        }
        return getConfig().getStringList("item.lore");
    }

    private PluginLanguage resolveLanguage(CommandSender sender) {
        PluginLanguage forced = configuredLanguage();
        if (forced != null) {
            return forced;
        }
        if (sender instanceof Player player) {
            return PluginLanguage.fromClientLocale(player.getLocale(), fallbackLanguage);
        }
        return fallbackLanguage;
    }

    private String translatedMessage(PluginLanguage language, String path) {
        return localizedString("messages", language, path.replaceFirst("^messages\\.", ""), "");
    }

    private String localizedString(String root, PluginLanguage language, String key, String fallback) {
        String localizedPath = root + "." + language.configKey() + "." + key;
        String localized = getConfig().getString(localizedPath);
        if (localized != null) {
            return localized;
        }
        return getConfig().getString(root + "." + key, fallback);
    }

    private void reloadLanguageSettings() {
        String configuredMode = normalizedConfigString("language.mode", "auto");
        if (!configuredMode.equals("auto") && !configuredMode.equals("it") && !configuredMode.equals("en")
                && !configuredMode.equals("italiano") && !configuredMode.equals("inglese")
                && !configuredMode.equals("italian") && !configuredMode.equals("english")) {
            warning("Invalid language.mode: auto will be used.",
                    "Valore language.mode non valido: verrà usata la modalità auto.");
            configuredMode = "auto";
        }
        languageMode = configuredMode;
        String configuredFallback = normalizedConfigString("language.fallback", "en");
        fallbackLanguage = PluginLanguage.fromConfiguredValue(configuredFallback, PluginLanguage.EN);
        if (!configuredFallback.equals("it") && !configuredFallback.equals("en")
                && !configuredFallback.equals("italiano") && !configuredFallback.equals("inglese")
                && !configuredFallback.equals("italian") && !configuredFallback.equals("english")) {
            warning("Invalid language.fallback: en will be used.",
                    "Valore language.fallback non valido: verrà usato l'inglese (en).");
        }

        String configuredItemMode = normalizedConfigString("language.item-mode", "holder");
        if (!configuredItemMode.equals("holder") && !configuredItemMode.equals("server")) {
            warning("Invalid language.item-mode: holder will be used.",
                    "Valore language.item-mode non valido: verrà usata la modalità holder.");
            configuredItemMode = "holder";
        }
        itemLanguageMode = configuredItemMode;
    }

    private String normalizedConfigString(String path, String fallback) {
        String value = getConfig().getString(path);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private PluginLanguage configuredLanguage() {
        if ("auto".equals(languageMode)) {
            return null;
        }
        return PluginLanguage.fromConfiguredValue(languageMode, fallbackLanguage);
    }

    private String consoleText(String english, String italian) {
        PluginLanguage forced = configuredLanguage();
        return (forced == null ? fallbackLanguage : forced) == PluginLanguage.IT ? italian : english;
    }
}

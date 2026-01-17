package com.azk.pulse.modules.logs;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LookupCommand implements PulseSubcommand {
    private static final long DEFAULT_MAX_SELECTION_VOLUME = 200000L;

    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final LookupSelectionManager selectionManager;
    private final LookupService lookupService;
    private final LookupWandListener wandListener;

    public LookupCommand(JavaPlugin plugin, ConfigFiles configFiles, LookupSelectionManager selectionManager,
                         LookupService lookupService, LookupWandListener wandListener) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.selectionManager = selectionManager;
        this.lookupService = lookupService;
        this.wandListener = wandListener;
    }

    @Override
    public String name() {
        return "lookup";
    }

    @Override
    public String module() {
        return "logs";
    }

    @Override
    public String permission() {
        return "pulse.mod";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!configFiles.getMain().getBoolean("logs.enabled", true)) {
            MessageUtil.sendWarningKey(sender, plugin, "lookup.disabled");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "wand" -> giveWand(sender);
            case "area" -> lookupArea(sender, args);
            case "clear" -> clearSelection(sender);
            case "tp" -> teleportTo(sender, args);
            default -> lookupPlayer(sender, args);
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("wand");
            options.add("area");
            options.add("clear");
            options.add("tp");
            options.add("*");
            options.addAll(onlinePlayers());
            return filterByPrefix(options, args[0]);
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        if (mode.equals("wand") || mode.equals("clear")) {
            return List.of();
        }

        if (mode.equals("area")) {
            if (args.length == 2) {
                List<String> options = new ArrayList<>();
                options.add("*");
                options.addAll(onlinePlayers());
                options.addAll(lookupOptions(false));
                return filterByPrefix(options, args[1]);
            }
            return filterLookupOptions(args, false);
        }
        if (mode.equals("tp")) {
            if (args.length == 2) {
                return filterByPrefix(worldNames(), args[1]);
            }
            return List.of();
        }

        return filterLookupOptions(args, true);
    }

    private List<String> filterLookupOptions(String[] args, boolean includeLocation) {
        String current = args[args.length - 1];
        if (current.toLowerCase(Locale.ROOT).startsWith("type=")) {
            return typeSuggestions(current);
        }
        if (current.toLowerCase(Locale.ROOT).startsWith("action=")) {
            return actionSuggestions(current);
        }
        return filterByPrefix(lookupOptions(includeLocation), current);
    }

    private boolean giveWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }
        MessageUtil.sendTitleKey(sender, plugin, "lookup.wand.title");
        ItemStack wand = wandListener.createWand();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(wand);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            MessageUtil.sendWarningKey(sender, plugin, "lookup.wand.inventory-full");
        }
        MessageUtil.sendSuccessKey(sender, plugin, "lookup.wand.added");
        MessageUtil.sendKey(sender, plugin, "lookup.wand.instructions1");
        MessageUtil.sendKey(sender, plugin, "lookup.wand.instructions2");
        return true;
    }

    private boolean clearSelection(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }
        selectionManager.clear(player);
        MessageUtil.sendTitleKey(sender, plugin, "lookup.title");
        MessageUtil.sendSuccessKey(sender, plugin, "lookup.selection-cleared");
        return true;
    }

    private boolean teleportTo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }
        if (args.length != 4 && args.length != 5) {
            MessageUtil.sendWarningKey(sender, plugin, "lookup.tp.usage");
            return true;
        }
        String worldName = args.length == 5 ? args[1] : player.getWorld().getName();
        int startIndex = args.length == 5 ? 2 : 1;
        Integer x = parseInt(args[startIndex], null);
        Integer y = parseInt(args[startIndex + 1], null);
        Integer z = parseInt(args[startIndex + 2], null);
        if (x == null || y == null || z == null) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.tp.invalid-coordinates");
            return true;
        }
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.tp.world-not-found",
                    java.util.Map.of("world", worldName));
            return true;
        }
        player.teleport(new Location(world, x + 0.5, y, z + 0.5));
        MessageUtil.sendSuccessKey(sender, plugin, "lookup.tp.success",
                java.util.Map.of("world", worldName, "x", Integer.toString(x), "y", Integer.toString(y), "z", Integer.toString(z)));
        return true;
    }

    private boolean lookupArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }

        LookupSelection selection = selectionManager.getSelection(player);
        if (!selection.isComplete()) {
            MessageUtil.sendWarningKey(sender, plugin, "lookup.selection-incomplete");
            return true;
        }
        Location pos1 = selection.getPos1();
        Location pos2 = selection.getPos2();
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.selection-invalid");
            return true;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.selection-world-mismatch");
            return true;
        }

        LookupSelectionManager.SelectionRegion region = selectionManager.getRegion(player);
        if (region == null) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.selection-invalid");
            return true;
        }

        long maxVolume = configFiles.getMain().getLong("logs.max-selection-volume", DEFAULT_MAX_SELECTION_VOLUME);
        if (maxVolume > 0 && region.volume() > maxVolume) {
            MessageUtil.sendWarningKey(sender, plugin, "lookup.selection-too-large",
                    java.util.Map.of("volume", Long.toString(region.volume()), "max", Long.toString(maxVolume)));
            MessageUtil.sendKey(sender, plugin, "lookup.selection-too-large-hint");
            return true;
        }

        int startIndex = 1;
        String targetPlayer = null;
        if (args.length >= 2 && isPlayerArg(args[1])) {
            targetPlayer = parsePlayerArg(args[1]);
            startIndex = 2;
        }

        ParsedArgs parsed = parseArgs(args, startIndex);
        int limit = resolveLimit(parsed.limit);
        int page = parsed.page > 0 ? parsed.page : 1;
        int offset = (page - 1) * limit;

        if (parsed.typeInvalid) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.invalid-type");
            return true;
        }
        if (parsed.durationInvalid) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.invalid-since");
            return true;
        }

        LogRepository.LookupQuery query = new LogRepository.LookupQuery(
                targetPlayer,
                parsed.type,
                region.world(),
                null,
                null,
                null,
                null,
                region.minX(),
                region.maxX(),
                region.minY(),
                region.maxY(),
                region.minZ(),
                region.maxZ(),
                parsed.action,
                parsed.material,
                parsed.since,
                limit,
                offset
        );

        String title = buildTitle(MessageUtil.tr(plugin, "lookup.area-title"), targetPlayer, parsed.type, page)
                + " @ " + formatRegion(region);
        if (parsed.export) {
            lookupService.exportLookup(sender, query, title);
        } else {
            lookupService.sendLookup(sender, query, title);
        }
        return true;
    }

    private boolean lookupPlayer(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String targetPlayer = parsePlayerArg(args[0]);
        ParsedArgs parsed = parseArgs(args, 1);
        int limit = resolveLimit(parsed.limit);
        int page = parsed.page > 0 ? parsed.page : 1;
        int offset = (page - 1) * limit;

        if (parsed.typeInvalid) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.invalid-type");
            return true;
        }
        if (parsed.durationInvalid) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.invalid-since");
            return true;
        }

        String world = parsed.world;
        Integer x = parsed.x;
        Integer y = parsed.y;
        Integer z = parsed.z;
        Integer radius = parsed.radius;

        if (sender instanceof Player playerSender) {
            if (world == null && (x != null || z != null || radius != null)) {
                world = playerSender.getWorld().getName();
            }
            if ((x == null || z == null) && radius != null) {
                x = playerSender.getLocation().getBlockX();
                y = playerSender.getLocation().getBlockY();
                z = playerSender.getLocation().getBlockZ();
            }
        } else if (radius != null && (x == null || z == null)) {
            MessageUtil.sendErrorKey(sender, plugin, "lookup.radius-requires-coords");
            return true;
        }

        LogRepository.LookupQuery query = new LogRepository.LookupQuery(
                targetPlayer,
                parsed.type,
                world,
                x,
                y,
                z,
                radius,
                null,
                null,
                null,
                null,
                null,
                null,
                parsed.action,
                parsed.material,
                parsed.since,
                limit,
                offset
        );

        String title = buildTitle(MessageUtil.tr(plugin, "lookup.title"), targetPlayer, parsed.type, page);
        if (parsed.export) {
            lookupService.exportLookup(sender, query, title);
        } else {
            lookupService.sendLookup(sender, query, title);
        }
        return true;
    }

    private ParsedArgs parseArgs(String[] args, int startIndex) {
        ParsedArgs parsed = new ParsedArgs();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("export")) {
                parsed.export = true;
                continue;
            }
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                String key = parts[0].toLowerCase(Locale.ROOT);
                String value = parts.length > 1 ? parts[1] : "";
                switch (key) {
                    case "type" -> {
                        LogRepository.LogType type = LogRepository.LogType.fromString(value);
                        if (type == null) {
                            parsed.typeInvalid = true;
                        } else {
                            parsed.type = type;
                        }
                    }
                    case "limit" -> parsed.limit = parseInt(value, -1);
                    case "page" -> parsed.page = parseInt(value, 1);
                    case "world" -> parsed.world = value;
                    case "x" -> parsed.x = parseInt(value, null);
                    case "y" -> parsed.y = parseInt(value, null);
                    case "z" -> parsed.z = parseInt(value, null);
                    case "radius" -> parsed.radius = parseInt(value, null);
                    case "action" -> parsed.action = value;
                    case "material" -> parsed.material = value;
                    case "since" -> parsed.since = parseSince(value, parsed);
                    default -> {
                    }
                }
            } else if (parsed.type == null) {
                LogRepository.LogType type = LogRepository.LogType.fromString(arg);
                if (type == null) {
                    parsed.typeInvalid = true;
                } else {
                    parsed.type = type;
                }
            }
        }
        return parsed;
    }

    private Integer parseInt(String value, Integer fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Instant parseSince(String value, ParsedArgs parsed) {
        Duration duration = parseDuration(value);
        if (duration == null) {
            parsed.durationInvalid = true;
            return null;
        }
        return Instant.now().minus(duration);
    }

    private Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        long multiplier;
        if (trimmed.endsWith("s")) {
            multiplier = 1;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("m")) {
            multiplier = 60;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("h")) {
            multiplier = 3600;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("d")) {
            multiplier = 86400;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else {
            return null;
        }
        try {
            long amount = Long.parseLong(trimmed);
            if (amount <= 0) {
                return null;
            }
            return Duration.ofSeconds(amount * multiplier);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int resolveLimit(int requested) {
        int defaultLimit = configFiles.getMain().getInt("logs.lookup-default-limit", 10);
        int maxLimit = configFiles.getMain().getInt("logs.lookup-max-limit", 200);
        int limit = requested > 0 ? requested : defaultLimit;
        if (maxLimit > 0) {
            limit = Math.min(limit, maxLimit);
        }
        return Math.max(1, limit);
    }

    private String buildTitle(String base, String player, LogRepository.LogType type, int page) {
        String target = player == null ? MessageUtil.tr(plugin, "lookup.all") : player;
        String typeLabel = type == null
                ? null
                : MessageUtil.tr(plugin, "lookup.type." + type.name().toLowerCase(Locale.ROOT), type.name());
        String suffix = typeLabel == null ? "" : " (" + typeLabel + ")";
        String pageLabel = page > 1 ? " [page " + page + "]" : "";
        return base + ": " + target + suffix + pageLabel;
    }

    private String formatRegion(LookupSelectionManager.SelectionRegion region) {
        return region.world() + " " + region.minX() + "," + region.minY() + "," + region.minZ()
                + " -> " + region.maxX() + "," + region.maxY() + "," + region.maxZ();
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.sendTitleKey(sender, plugin, "lookup.title");
        MessageUtil.sendKey(sender, plugin, "lookup.usage.main");
        MessageUtil.sendKey(sender, plugin, "lookup.usage.area");
        MessageUtil.sendKey(sender, plugin, "lookup.usage.tp");
        MessageUtil.sendKey(sender, plugin, "lookup.usage.wand");
        MessageUtil.sendKey(sender, plugin, "lookup.usage.clear");
    }

    private boolean isPlayerArg(String arg) {
        if (arg == null || arg.isBlank()) {
            return false;
        }
        if (arg.equalsIgnoreCase("export")) {
            return false;
        }
        if (arg.contains("=")) {
            return false;
        }
        return LogRepository.LogType.fromString(arg) == null;
    }

    private String parsePlayerArg(String arg) {
        if (arg == null) {
            return null;
        }
        if (arg.equalsIgnoreCase("*") || arg.equalsIgnoreCase("all")) {
            return null;
        }
        return arg;
    }

    private List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        plugin.getServer().getWorlds().forEach(world -> names.add(world.getName()));
        return names;
    }

    private List<String> lookupOptions(boolean includeLocation) {
        List<String> options = new ArrayList<>();
        options.add("type=blocks");
        options.add("type=chests");
        options.add("type=commands");
        options.add("type=deaths");
        options.add("action=break");
        options.add("action=place");
        options.add("action=open");
        options.add("material=");
        options.add("limit=10");
        options.add("page=1");
        options.add("since=1h");
        options.add("export");
        if (includeLocation) {
            options.add("world=");
            options.add("x=");
            options.add("y=");
            options.add("z=");
            options.add("radius=");
        }
        return options;
    }

    private List<String> typeSuggestions(String current) {
        String prefix = current.substring("type=".length()).toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (LogRepository.LogType type : LogRepository.LogType.values()) {
            String name = type.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(prefix)) {
                results.add("type=" + name);
            }
        }
        return results;
    }

    private List<String> actionSuggestions(String current) {
        String prefix = current.substring("action=".length()).toLowerCase(Locale.ROOT);
        List<String> options = List.of("break", "place", "open");
        List<String> results = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                results.add("action=" + option);
            }
        }
        return results;
    }

    private List<String> filterByPrefix(List<String> options, String input) {
        if (input == null || input.isBlank()) {
            return options;
        }
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                results.add(option);
            }
        }
        return results;
    }

    private static class ParsedArgs {
        private LogRepository.LogType type;
        private int limit = -1;
        private int page = 1;
        private boolean typeInvalid = false;
        private boolean durationInvalid = false;
        private boolean export = false;
        private String action;
        private String material;
        private String world;
        private Integer x;
        private Integer y;
        private Integer z;
        private Integer radius;
        private Instant since;
    }
}

package com.azk.pulse.modules.moderation;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.MessageUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ReportsCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final ReportService reportService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public ReportsCommand(JavaPlugin plugin, ReportService reportService) {
        this.plugin = plugin;
        this.reportService = reportService;
    }

    @Override
    public String name() {
        return "reports";
    }

    @Override
    public String module() {
        return "moderation";
    }

    @Override
    public String permission() {
        return "pulse.mod";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int limit = parseLimit(args);
        List<ReportService.ReportEntry> reports = reportService.getReports().stream()
                .sorted(Comparator.comparingLong(ReportService.ReportEntry::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        MessageUtil.sendTitleKey(sender, plugin, "moderation.reports.title");
        if (reports.isEmpty()) {
            MessageUtil.sendWarningKey(sender, plugin, "moderation.reports.none");
            return true;
        }

        for (ReportService.ReportEntry report : reports) {
            String time = formatter.format(Instant.ofEpochMilli(report.timestamp()));
            String line = MessageUtil.tr(plugin, "moderation.reports.line", java.util.Map.of(
                    "time", time,
                    "reporter", report.reporter(),
                    "target", report.target(),
                    "reason", report.reason()
            ));
            MessageUtil.sendRaw(sender, line);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("limit=5", "limit=10");
    }

    private int parseLimit(String[] args) {
        int limit = 5;
        if (args.length >= 1) {
            String arg = args[0];
            if (arg.toLowerCase(Locale.ROOT).startsWith("limit=")) {
                arg = arg.substring("limit=".length());
            }
            try {
                limit = Integer.parseInt(arg);
            } catch (NumberFormatException ignored) {
                limit = 5;
            }
        }
        return Math.max(1, Math.min(limit, 20));
    }
}

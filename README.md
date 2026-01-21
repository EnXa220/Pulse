# Pulse

Pulse is a lightweight, modular all-in-one server management plugin for **Paper / Spigot / Purpur 1.21.x**.  
Built for **performance monitoring**, **lag diagnosis**, and **fast admin tools**, with a **simple config**.

## Highlights
- Live TPS/MSPT/RAM/CPU/player/chunk/entity stats: **/pulse status**
- Lag hotspots (worlds/chunks/entities/plugins) + smart diagnostics (health score + fixes)
- Performance history + CSV export
- Safe ClearLag (preview/confirm) + scheduled cleanup
- Chunk unload + entity cleanup tools
- Logs (block/chest/command/death) with area lookup + export
- Alerts & comms + moderation (**/report**) + modern GUI
- Modular (toggle features) • SQLite by default, optional MySQL

## Compatibility
- **Server:** Paper / Spigot / Purpur **1.21.x**
- **Java:** **21**  
Note: MSPT/CPU are Paper-only (shows **N/A** on Spigot).

## Languages
EN / FR (configurable)

## Support
Discord: https://discord.gg/cKXb4HM5DC

---

## Installation
1) Drop the jar in **/plugins**  
2) Restart the server  
3) Configure **config.yml / modules.yml / alerts.yml**  
4) Run **/pulse reload**

## Language
Set in **config.yml**:
```yml
general:
  language: "en"   # or "fr"
You can add more languages in /plugins/Pulse/lang/

Commands
/pulse status
/pulse lag
/pulse diagnose
/pulse history [24h|7d|30d]
/pulse history export [range]
/pulse health
/pulse clearlag preview
/pulse clearlag confirm
/pulse killentities radius=<number> type=<type>
/pulse unloadchunks world=<name>
/pulse alerttest
/pulse lookup <player|> [type=blocks|chests|commands|deaths] [action=break|place|open] [material=] [limit=] [page=] [world=] [x=] [y=] [z=] [radius=] [since=] [export]
/pulse lookup area [player|] [type=...] [action=] [material=] [limit=] [page=] [since=] [export]
/pulse lookup wand
/pulse lookup tp [world] <x> <y> <z>
/pulse lookup clear
/pulse gui
/report <player> <reason>
/pulse reports

Permissions
pulse.admin — full access (default: op)
pulse.mod — moderation tools
pulse.view — view performance info (default: true)
pulse.report — /report (default: true)
pulse.gui — open GUI
pulse.performance, pulse.logs, pulse.communication, pulse.moderation

Notes
Some metrics are Paper-only and show N/A on Spigot.

ClearLag can be scheduled via config.yml.

Logs support area selection and CSV export.

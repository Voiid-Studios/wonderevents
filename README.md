<img width="200" src="https://i.ibb.co/207HQVQc/welogof.png" alt="WonderEvents icon" align="right">

<div align="left">

# WonderEvents
The best framework for expansions & addons! Everything in one place.

[![Kofi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-plural_vector.svg "Support us on Ko-fi")](https://ko-fi.com/maxxvoiid/donate)
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg "Download on Modrinth")](https://modrinth.com/plugin/wonderevents)

[![VSPL](https://raw.githubusercontent.com/Voiid-Studios/voiidstudios/main/assets/buttons/vspl_license.svg "View the Voiid Studios Public License")](https://github.com/Voiid-Studios/voiidstudios/blob/main/LICENSE.md)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg "View WE Source Code on GitHub")](https://github.com/Voiid-Studios/wonderevents)

</div>

## ❓ What is WonderEvents?

WonderEvents (WE) is the core framework behind Voiid Studios' Minecraft events plugins. Instead of shipping one monolithic plugin per server, WE gives you a lightweight cross-platform base (Spigot/Paper/forks) that expansions and addons plug directly into.

Expansions and addons are just `.jar` files dropped into their respective folders — WonderEvents discovers them, resolves their dependencies, and loads them automatically, no server restart juggling needed.

## ✨ Features

- 🧩 **Expansions & Addons system** — drop-in `.jar` modules with their own `wonder-manifest.yml`, dependency resolution and versioned compatibility checks
- 📦 **Preinstalled content support** — expansions/addons can ship bundled inside the plugin itself and get installed automatically on first run
- 🔄 **Hot reload** — `/wonder reload <all|configs|expansions|addons>`, no full server restart required
- 🖥️ **Cross-platform core** — automatic Paper/Spigot platform adapters and scheduler handling
- 🎨 Adventure/MiniMessage-powered text formatting, with a YAML-driven multi-language message system
- 🛠️ Simple developer API (`WEAPI`, `WEABootstrap`, `WEACommand`, `WEAListener`, `WEAModule`) for building your own expansions/addons on top of WE.

## 📋 Requirements

- **Minecraft version:** `1.16+`
- **Server software:** Spigot, Paper or forks (CraftBukkit and Folia are NOT supported!)
- No external dependencies required — everything WonderEvents needs ships bundled inside the plugin

## 🚀 Installation

1. Download the [latest version here](https://github.com/Voiid-Studios/wonderevents/releases/latest)
2. Place the downloaded `.jar` file in your server's `plugins/` folder
3. Restart the server, WonderEvents is ready! ⚡
4. (Optional) Drop any additional expansions/addons into `plugins/WonderEvents/expansions/` and `plugins/WonderEvents/addons/`

## 📖 How to use it

- Manage everything through `/wonder` (alias `/wonderevents`):
  - `/wonder help` — shows all available commands
  - `/wonder reload <all|configs|expansions|addons>` — reloads WonderEvents without restarting the server
  - `/wonder expansions list` / `enable <id>` / `disable <id>` — manage installed expansions
  - `/wonder addons list` / `enable <id>` / `disable <id>` — manage installed addons
- Configure WonderEvents in `plugins/WonderEvents/config.yml`:
  - `faststats_metrics` — toggle anonymous metrics
  - `auto_update` — automatically download new stable releases when found
  - `update_notification` — notify players with `wonderevents.updatenotify` (and console) when an update is available
  - `language` — pick the language file used from `messages/origins/`

## 📚 WonderWiki VERY SOON!

## ⚡ fastStats
<a href="https://faststats.dev/project/wonderevents/stats"><img src="https://faststats.dev/embed/ba0fdc34-17ba-4aca-935a-9d63098c3527.svg?w=960&h=340&theme=dark" alt="Servers & Players"></a>
![WonderEvents Banner](https://voiid-studios.github.io/stuff/assets/proyect/we/banner.png)

<p align="center" style="text-align: center;">
  <a href="https://ko-fi.com/maxxvoiid/donate"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-plural_vector.svg" alt="Support us on Ko-fi" style="margin: 5px 10px;"></a>
  <a href="https://modrinth.com/plugin/wonderevents"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg" alt="Download on Modrinth" style="margin: 5px 10px;"></a>
  <a href="https://hangar.papermc.io/VoiidStudios/WonderEvents"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/available/hangar_vector.svg" alt="Download on Hangar" style="margin: 5px 10px;"></a>
  <br><a href="https://github.com/Voiid-Studios/voiidstudios/blob/main/LICENSE.md"><img src="https://voiid-studios.github.io/stuff/assets/buttons/vspl_license.svg" alt="View the Voiid Studios Public License" style="margin: 5px 10px;"></a>
  <a href="https://github.com/Voiid-Studios/wonderevents/issues/new/choose"><img src="https://voiid-studios.github.io/stuff/assets/buttons/reportbugs_compact.svg" alt="Report Bugs on GitHub" style="margin: 5px 10px;"></a>
  <a href="https://github.com/Voiid-Studios/wonderevents"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg" alt="View Source Code on GitHub" style="margin: 5px 10px;"></a>
</p>

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

## ❓ What is WonderEvents?

WonderEvents (WE) is the core framework behind Voiid Studios' Minecraft events plugins. Instead of shipping one monolithic plugin per server, WE gives you a lightweight cross-platform base (Spigot/Paper/forks) that expansions and addons plug directly into.

Expansions and addons are just `.jar` files dropped into their respective folders — WonderEvents discovers them, resolves their dependencies, and loads them automatically, no server restart juggling needed.

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

## ✨ Features

- 🧩 **Expansions & Addons system** — drop-in `.jar` modules with their own `wonder-manifest.yml`, dependency resolution and versioned compatibility checks
- 📦 **Preinstalled content support** — expansions/addons can ship bundled inside the plugin itself and get installed automatically on first run
- 🔄 **Hot reload** — `/wonder reload <all|configs|expansions|addons>`, no full server restart required
- 🖥️ **Cross-platform core** — automatic Paper/Spigot platform adapters and scheduler handling
- 🎨 Adventure/MiniMessage-powered text formatting, with a YAML-driven multi-language message system
- 🛠️ Simple developer API (`WEAPI`, `WEABootstrap`, `WEACommand`, `WEAListener`, `WEAModule`) for building your own expansions/addons on top of WE.

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

## 📋 Requirements

- **Minecraft version:** `1.16+`
- **Server software:** Spigot, Paper or forks (CraftBukkit and Folia are NOT supported!)
- No external dependencies required — everything WonderEvents needs ships bundled inside the plugin

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

## 🚀 Installation

1. Download the latest version.
2. Place the downloaded `.jar` file in your server's `plugins/` folder
3. Restart the server, WonderEvents is ready! ⚡
4. (Optional) Drop any additional expansions/addons into `plugins/WonderEvents/expansions/` and `plugins/WonderEvents/addons/`

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

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

![Divider](https://voiid-studios.github.io/stuff/assets/proyect/we/divider.png)

## 📚 WonderWiki VERY SOON!
While we're working on WonderWiki, you can access the [JDocs](https://voiid-studios.github.io/wonderevents/)!

## ⚡ fastStats
<a href="https://faststats.dev/project/wonderevents/stats"><img src="https://faststats.dev/embed/ba0fdc34-17ba-4aca-935a-9d63098c3527.svg?w=960&h=340&theme=dark" alt="Servers & Players"></a>
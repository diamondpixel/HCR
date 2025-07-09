# 💀 Hardcore Respawn(HCR)

A powerful, Paper/Spigot plugin that re-imagines the **Hardcore** experience – die once, and you are *really* gone…​ unless your friends have a **Revival Token**!  

---

## 🚀 Features

| | Description |
|---|---|
| 🧿 **Revival Token** | Craftable item (fully configurable recipe) that allow players to revive the fallen. |
| 📜 **/hcr Command Suite** | Give tokens, open the recipe configurator, or reload the plugin – all in one parent command. |
| 🖼️ **Beautiful GUIs** | Paginated list of dead players with custom skulls, smooth animations and one-click revive destinations (bed, spawn, eye-look, death-coords). |
| ⚡ **Zero-Lag Utilities** | Optimised caching, asynchronous tasks and more to ensure TPS friendly performance. |
| 🔄 **Hot Reload** | Change the recipe or config on the fly – `/hcr reload` seamlessly unloads & registers new recipes for every online player. |

## 🗒️ TODO
- ⚙️ **Optimise revive menu**
- 📦 **Place player's loot in a chest at death location**
- 🛡️ **Safe teleporting that avoids water or void on respawn**

---

## 📦 Installation

1. Compile the latest **`HCR-x.x.jar`**.
2. Drop it into your server’s `plugins/` folder.
3. Restart or **`/reload`** the server – that’s it! 🎉
4. *(Optional)* Edit `config.yml` to tweak debug mode, GUI shuffle interval or the token recipe.

> **Minimum requirements**  
> • Java 17+  
> • Paper / Spigot **1.21.5**  
> • `SpiGUI` is shaded automatically – no extra dependencies needed.

---

## 🕹️ Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/hcr` | `hcr.use` | Root command – shows interactive help. |
| `/hcr giveToken <player> [amount]` | `hcr.givetoken` | Give one or more Revival Tokens. |
| `/hcr configureRecipe` | `hcr.configurerecipe` | Opens the drag-&-drop recipe editor GUI. |
| `/hcr reload` | `hcr.reload` | Saves config, clears caches and reloads the plugin without rebooting the server. |

---

## 🔧 Configuration

`config.yml` ships with sensible defaults but is 100 % editable:

```yml
debug: false                 # Extra console spam for troubleshooting
config_gui_shuffle_interval: 10 # How often the recipe GUI glass panes shuffle (ticks)
recipe:                       # 3×3 crafting grid, top-left → bottom-right
  - AIR
  - DIAMOND
  - AIR
  - DIAMOND
  - NETHER_STAR
  - DIAMOND
  - AIR
  - DIAMOND
  - AIR
```

Change any material to whatever you like.  
If the recipe is invalid or entirely `AIR`, HCR safely rolls back to the default ✨

---

## 💻 Building from Source

```bash
# Clone & build
git clone https://github.com/your/repo.git && cd HCR
mvn clean package
# The shaded jar will be in target/HardcoreRespawn-Reworked-*.jar
```

The project uses **Java 21**, **Maven 3.8+**, and automatically shades **SpiGUI** & **SkullCreator**.

---

## 🙌 Contributing

PRs & feature requests are welcome!  
Please open an issue first to discuss major changes.

1. Fork the repository
2. Create your feature branch: `git checkout -b feat/amazing-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push to the branch: `git push origin feat/amazing-feature`
5. Open a Pull Request

---

## 📝 License

Distributed under the **MIT** license.

---

<p align="center">
  Made with ❤️  &  ☕  by **(diamondpixel/Liparakis/Takys)**
</p>

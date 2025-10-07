# KaizenPortals

A comprehensive Minecraft portal plugin that revolutionizes transportation with multiple exit points, advanced management systems, and rich customization options.

## 🌟 Features

### Core Portal System
- **Multiple Exit Points**: Create portals with unlimited exit destinations
- **Smart Exit Selection**: Choose how players exit with multiple modes:
  - `FIRST`: Always use the first exit point
  - `RANDOM`: Randomly select an exit point
  - `SEQUENTIAL`: Cycle through exits in order
  - `NEAREST`: Teleport to the closest exit point
- **Visual Effects**: Beautiful particle effects for both portals and exit points
- **Sound Effects**: Immersive audio feedback for portal usage

### GUI Management System
- **Intuitive Interface**: Manage all portals through a user-friendly GUI (`/portal`)
- **Portal Wand Tool**: Interactive wand for easy portal management
  - Left-click blocks to select exit locations
  - Right-click portals to add selected exits
  - Right-click air to create new portals
  - Visual feedback and duplicate detection
- **Visual Portal List**: Browse all portals with detailed information
- **Exit Point Manager**: Add, remove, and configure exit points per portal with pagination support

### Advanced Features

#### 🎒 Kit System
Distribute items to players when using portals:
- **13 Pre-configured Kits**:
  - Starter Kit - Basic survival items
  - Builder Kit - Construction materials
  - Warrior Kit - Combat equipment
  - Archer Kit - Ranged combat gear
  - Miner Kit - Mining tools and torches
  - Farmer Kit - Agricultural supplies
  - Explorer Kit - Navigation tools
  - Redstone Kit - Redstone components
  - Enchanter Kit - Enchanting materials
  - Alchemist Kit - Brewing supplies
  - Nether Kit - Nether survival gear
  - End Kit - End dimension equipment
  - VIP Kit - Premium items
- **Cooldown System**: Prevent kit spam with configurable cooldowns
- **One-time Kits**: Option for kits that can only be claimed once

#### 🔐 Permission System
- **Portal Limits**: Set maximum portals per permission group
- **World Restrictions**: Control which worlds players can create portals in
- **Usage Permissions**: Fine-grained control over portal usage
- **Default Groups**:
  - `portal2exit.basic` - 10 portals, overworld/nether access
  - `portal2exit.vip` - 20 portals, all dimensions
  - `portal2exit.mvp` - 50 portals, all dimensions
  - `portal2exit.admin` - Unlimited portals, all worlds

#### 💰 Economy Integration
- **Creation Costs**: Charge players to create portals
- **Usage Fees**: Set per-use costs for portals
- **Scaling Costs**: Increase prices for additional portals
- **Refund System**: Get partial refunds when removing portals
- **Owner Revenue**: Portal owners can earn from others using their portals
- **Vault Support**: Full integration with Vault economy

#### 🔧 Activation Requirements
Control who can use portals with:
- **Item Requirements**: Require specific items to activate portals
- **Permission Requirements**: Restrict portal usage by permission
- **Kit Requirements**: Require players to have received specific kits
- **Consumable Items**: Option to consume required items on use

### Technical Features
- **Cross-Version Support**: Compatible with Minecraft 1.12.2 - 1.21+
- **Performance Optimized**: Efficient particle rendering and portal detection
- **Data Persistence**: Automatic portal saving and loading
- **Safety Checks**: Teleportation safety with obstruction detection
- **Auto-Save**: Configurable automatic data saving

## 📦 Installation

1. Download the latest `portal2exit-1.0-SNAPSHOT.jar` from releases
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/Portal2Exit/config.yml`

### Requirements
- Paper/Spigot/Bukkit server 1.12.2 or higher
- Java 8 or higher
- Optional: Vault (for economy features)

## 🎮 Commands

### Basic Commands
- `/portal` - Open the portal management GUI
- `/portal create <name>` - Create a new portal at your location
- `/portal remove <name>` - Remove an existing portal
- `/portal list` - List all your portals
- `/portal tp <name>` - Teleport to a portal
- `/portal wand` - Get the portal wand tool

### Exit Management
- `/portal addexit <name>` - Add current location as exit point
- `/portal setexit <name>` - Set current location as the only exit
- `/portal removeexit <name> <index>` - Remove specific exit point
- `/portal setexitmode <name> <mode>` - Set exit selection mode

### Configuration
- `/portal reload` - Reload configuration
- `/portal setcost <name> <amount>` - Set portal usage cost
- `/portal setkit <name> <kit>` - Set kit given on portal use
- `/portal setrequiredkit <name> <kit>` - Set required kit for usage

### Admin Commands
- `/portal setowner <name> <player>` - Change portal owner
- `/portal info <name>` - View detailed portal information
- `/portal removeall` - Remove all portals (admin only)

## 🔑 Permissions

### Basic Permissions
- `portal2exit.use` - Use portals
- `portal2exit.create` - Create portals
- `portal2exit.remove` - Remove own portals
- `portal2exit.wand` - Use portal wand
- `portal2exit.gui` - Access portal GUI

### Advanced Permissions
- `portal2exit.unlimited` - Bypass portal limits
- `portal2exit.free` - Bypass economy costs
- `portal2exit.admin` - Full administrative access
- `portal2exit.remove.others` - Remove other players' portals
- `portal2exit.bypass.cooldown` - Bypass cooldowns
- `portal2exit.bypass.requirements` - Bypass activation requirements

### Kit Permissions
- `portal2exit.kit.<kitname>` - Access to specific kit
- `portal2exit.kit.all` - Access to all kits

## ⚙️ Configuration

### Basic Settings
```yaml
plugin:
  debug: false
  auto-save-interval: 5  # Minutes

portals:
  max-portals-per-player: 10
  cooldown: 3  # Seconds
  sounds-enabled: true
  particles-enabled: true
```

### Portal Creation
```yaml
creation:
  required-item: "minecraft:ender_eye"
  required-base-block: "minecraft:obsidian"
  consume-item: true
```

### Economy Settings
```yaml
economy:
  enabled: false
  creation-cost: 1000.0
  scaling-multiplier: 0.5
  default-usage-cost: 10.0
  removal-refund-percentage: 50
```

### Exit Portal Defaults
```yaml
exit-portals:
  default-exit-type: "spawn"  # Options: spawn, bed, custom
  custom-exit:
    world: "world"
    x: 0
    y: 64
    z: 0
```

## 🎯 Usage Examples

### Using the Portal Wand (Recommended)
1. Get the wand: `/portal wand`
2. **Select an exit location**: Left-click a block where you want players to teleport
3. **Create/manage portal**: Stand within 5 blocks of where you want the portal
4. Right-click with the wand to add the exit point to the portal
5. Repeat steps 2-4 to add multiple exit points
6. Left-click air to see your currently selected location

### Creating a Hub Portal (Command Method)
1. Stand at your hub location
2. Run `/portal create hub`
3. Travel to destination
4. Run `/portal addexit hub`
5. Repeat for multiple destinations

### Setting Up VIP Areas
1. Create portal: `/portal create vip-lounge`
2. Set permission: `/portal setrequiredpermission vip-lounge portal2exit.vip`
3. Add cost: `/portal setcost vip-lounge 100`
4. Add kit reward: `/portal setkit vip-lounge vip`

### Building a Dungeon System
1. Create entrance: `/portal create dungeon-entrance`
2. Add multiple exits at different dungeon levels
3. Set mode: `/portal setexitmode dungeon-entrance SEQUENTIAL`
4. Players progress through levels in order

## 🐛 Troubleshooting

### Portal Not Working
- Check if you have `portal2exit.use` permission
- Verify activation requirements are met
- Ensure cooldown has expired
- Check if portal has exit points

### Economy Not Working
- Install Vault plugin
- Install an economy plugin (EssentialsX, etc.)
- Enable economy in config.yml
- Check console for Vault errors

### Particles Not Showing
- Verify particles are enabled in config
- Check client particle settings
- Ensure you're within render distance

## 📊 Performance Tips

1. **Limit Particle Effects**: Reduce particle density for better performance
2. **Adjust Auto-Save**: Increase interval for large servers
3. **Portal Limits**: Set reasonable per-player limits
4. **Cooldowns**: Use cooldowns to prevent spam

## 🔄 Version History

### v1.0-SNAPSHOT (Latest)
- **Portal Wand Fixes**: Resolved critical event priority issues preventing exit point creation
- **Enhanced Wand Functionality**:
  - Left-click air support for location feedback
  - Duplicate exit detection with user feedback
  - Better validation for world loading and cross-world exits
- **Selection Mode Support**: Fixed teleportation to properly use FIRST, RANDOM, SEQUENTIAL, and NEAREST modes
- **Code Quality**: Uses constants for portal detection radius, improved maintainability
- Core portal system with unlimited exits
- Interactive GUI management interface
- Kit distribution system with 13 pre-configured kits
- Permission-based limits
- Economy integration via Vault
- Cross-version support (1.12.2 - 1.21+)
- Bug fixes for XSeries dependencies
- ConfigurationSection handling improvements

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or report issues on our GitHub repository.

### Development Setup
1. Clone repository: `git clone https://github.com/KaizenCore/portal2exit.git`
2. Open in your IDE
3. Run `./gradlew build` to compile
4. Test on local server

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Credits

- **Developer**: KaizenCore Team
- **Contributors**: Community members
- **Special Thanks**: Paper/Spigot development teams

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/KaizenCore/portal2exit/issues)
- **Discord**: [Join our server](https://discord.gg/your-invite)
- **Wiki**: [Documentation](https://github.com/KaizenCore/portal2exit/wiki)

## 🚀 Future Plans

- [ ] Portal networks with interconnected systems
- [ ] Custom portal blocks and structures
- [ ] Portal ownership transfer system
- [ ] Advanced particle customization
- [ ] Cross-server portal support (BungeeCord)
- [ ] API for developers
- [ ] Web interface for portal management
- [ ] Mobile app integration

---

**KaizenPortals** - Transform your Minecraft server's transportation system with continuous improvement!
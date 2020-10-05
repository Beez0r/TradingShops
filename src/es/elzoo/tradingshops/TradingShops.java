package es.elzoo.tradingshops;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import es.elzoo.tradingshops.gui.GUIEvent;
import net.milkbowl.vault.economy.Economy;

public class TradingShops extends JavaPlugin {
	File configFile;
	public static FileConfiguration config;
	public static WorldGuardLoader wgLoader = null;
	private static String chainConnect;
	private static Economy economy = null;
	private static Connection connection = null;

	@Override
	public void onLoad() {
		Plugin wgCheck = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if(wgCheck != null)
			wgLoader = new WorldGuardLoader();
	}

	@Override
	public void onEnable() {
		chainConnect = "jdbc:sqlite:"+getDataFolder().getAbsolutePath()+"/shops.db";
		this.setupEconomy();
		this.createConfig();

		if(config.getString("shopBlock") == null) {
			config.set("shopBlock", "minecraft:barrel");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TradingShops] shopBlock cannot be empty! Reverting to default minecraft:barrel");
		}
		if(config.getString("stockBlock") == null) {
			config.set("stockBlock", "minecraft:composter");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TradingShops] stockBlock cannot be empty! Reverting to default minecraft:composter");
		}

		getServer().getPluginManager().registerEvents(new EventShop(), this);
		getServer().getPluginManager().registerEvents(new GUIEvent(), this);
		getCommand("tradeshop").setExecutor(new CommandShop());

		try {
			connection = DriverManager.getConnection(chainConnect);
			this.createTables();
			Shop.loadData();
		} catch(Exception e) {
			e.printStackTrace();
		}

		// Tick shops every 2 seconds
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, Shop::tickShops, 40, 40);

		// Schedule saveData task every 5 minutes
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> Bukkit.getScheduler().runTaskAsynchronously(this, Shop::saveData), 6000, 6000);
	}

	@Override
	public void onDisable() {
		Shop.saveData();

		if(connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void createTables() {
		PreparedStatement[] stmts = new PreparedStatement[] {};
		try {
			stmts = new PreparedStatement[] {
				connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendas(id INTEGER PRIMARY KEY autoincrement, location varchar(64), owner varchar(64));"),
				connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendasFilas(itemIn text, itemOut text, idTienda INTEGER);"),
				connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaStocks(owner varchar(64), items JSON);")
			};
		} catch(Exception e) {
			e.printStackTrace();
		}

		for(PreparedStatement stmt : stmts) {
			try {
				stmt.execute();
				stmt.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		List<PreparedStatement> stmtsPatches = new ArrayList<>();
		try {
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN broadcast BOOLEAN DEFAULT 0"));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaStocks ADD COLUMN pag INTEGER DEFAULT 0"));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendas ADD COLUMN admin BOOLEAN DEFAULT FALSE;"));
		} catch(Exception ignored) { }

		for(PreparedStatement stmtsPatch : stmtsPatches) {
			try {
				stmtsPatch.execute();
				stmtsPatch.close();
			} catch (Exception ignored) {
			}
		}
	}

	public static Connection getConnection() {
		checkConnection();
		return connection;
	}

	public static void checkConnection() {
		try {
			if(connection == null || connection.isClosed() || !connection.isValid(0))
				connection = DriverManager.getConnection(chainConnect);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null)
			return false;

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null)
			return false;

		economy = rsp.getProvider();
		return economy != null;
	}

	public static Optional<Economy> getEconomy() {
		return Optional.ofNullable(economy);
	}

	public void createConfig() {
		this.configFile = new File(getDataFolder(), "config.yml");
		if(!this.configFile.exists()) {
			this.configFile.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}

		config = new YamlConfiguration();

		try {
			config.load(this.configFile);
			String ver = config.getDouble("configVersion")+"";
			switch (ver) {
				case "1.0":
					config.set("deleteBlock", false);
					config.set("showParticles", true);
				case "1.1":
					config.set("enableWorldGuardFlag", true);
				case "1.2":
					config.set("stockPages", 5);
				case "1.3":
					config.set("usePermissions", true);
					config.set("defaultShopLimit", 5);
				case "1.4":
					config.set("noPermissions", "&cYou do not have permission for this command!");
					config.set("shopNotOwned", "&cYou do not own this shop!");
					config.set("shopDeleted", "&7Your shop has been &cDELETED&7!");
					config.set("shopCreated", "&7Your shop has been &aCREATED&7!");
					config.set("shopLimit", "&cYou do not have permission or reached your shops amount limit!");
					config.set("noItems", "&cYou do not have enough item(s) to buy from this shop!");
					config.set("noStock", "&cThe shop is currently out of stock of that item(s)!");
					config.set("noStockNotify", "&cOne of your shops is currently out of stock! &o(%s)");
					config.set("buy", "&7You bought&a %in &7for&c %out&7!");
					config.set("sell", "&7%p has bought&7&c %in &7for&a %out&7!");
					config.set("noMoney", "&cYou do not have enough money to create a shop. You need at least &o$");
					config.set("reload", "&7Configuration file reloaded.");
				case "1.5":
					config.set("adminShop", "Admin Shop");
					config.set("normalShop", "%player%'s shop");
					config.set("sellTitle", "ITEMS FOR SALE");
					config.set("buyTitle", "PRICE TO BUY ITEMS");
					config.set("deleteTitle", "DELETE");
					config.set("createTitle", "CREATE");
					config.set("broadcastOn", "Broadcast ON");
					config.set("broadcastOff", "Broadcast OFF");
					config.set("createShopTitle", "Create Shop");
					config.set("stockTitle", "Shop Stock Inventory");
					config.set("page", "Page");
					config.set("buyAction", "BUY");
				case "1.6":
					config.set("shopBusy", "&6Shop is currently busy, try again soon!");
					config.set("stockCommandDisabled", "&cThe /stock command has been disabled!");
					config.set("noStockCooldown", 300);
					config.set("playerInventoryFull", "&cYour inventory is FULL! Cannot buy any items!");
					config.set("noPlayerShop", "&cPlayer name does not exist! Cannot list player shop!");
				case "2.0":
					config.set("remoteManage", true);
				case "2.1":
					config.set("enableStockBlock", true);
					config.set("enableAdminShop", true);
					config.set("enableShopBlock", true);
					config.set("remoteShopping", false);
					config.set("publicListCommand", false);
					config.set("noPlayerFound", "&cNo stock can be found for given player!");
					config.set("noRemoteManage", "&cRemote management of shops has been disabled!");
					config.set("noShopFound", "&cNo shop found with given id!");
					config.set("playerShopCreated", "&7Shop has been &aCREATED&7 for&a %p &7!");
					config.set("shopIDDeleted", "&cShop id&a %id &chas been DELETED!");
					config.set("shopIntegerError", "&cShop id must be an integer greater than 0!");
					config.set("targetMismatch", "&cTargeted block must match shop block set in config!");
					config.set("disabledShopBlock", "&cCannot create shop location when shop blocks are disabled!");
					config.set("adminShopDisabled", "&cAdmin shops have been disabled, cannot create!");
					config.set("existingShop","&cCannot create shop where existing shop is already located!");
					config.set("noRemoteShops","&cRemote shopping has been disabled!");
					config.set("configVersion", 2.2);
					config.save(configFile);
				case "2.2":
					break;
			}
		} catch(IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}
	public static TradingShops getPlugin() {
		return TradingShops.getPlugin(TradingShops.class);
	}
}

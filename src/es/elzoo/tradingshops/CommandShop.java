package es.elzoo.tradingshops;

import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.Scanner;
import java.util.UUID;

import es.elzoo.tradingshops.inventories.InvShop;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import es.elzoo.tradingshops.inventories.InvAdminShop;
import es.elzoo.tradingshops.inventories.InvStock;

public class CommandShop implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;

		if (args.length == 0) {
			listSubCmd(player, label);
		} else if (args[0].equalsIgnoreCase("create")) {
			createStore(player);
		} else if (args[0].equalsIgnoreCase("adminshop")) {
			adminShop(player);
		} else if (args[0].equalsIgnoreCase("delete")) {
			deleteShop(player);
		} else if (args[0].equalsIgnoreCase("deleteid") && args.length >= 2) {
			deleteShopID(player, args[1]);
		} else if (args[0].equalsIgnoreCase("stock")) {
			stockShop(player);
		} else if (args[0].equalsIgnoreCase("reload")) {
			reloadShop(player);
		} else if (args[0].equalsIgnoreCase("list") && args.length == 1) {
			Bukkit.getServer().getScheduler().runTaskAsynchronously(TradingShops.getPlugin(), () -> listShops(player, null));
		} else if (args[0].equalsIgnoreCase("list") && args.length >= 2) {
			Bukkit.getServer().getScheduler().runTaskAsynchronously(TradingShops.getPlugin(), () -> listShops(player, args[1]));
		} else if (args[0].equalsIgnoreCase("manage") && args.length >= 2) {
			shopManage(player, args[1]);
		} else if (args[0].equalsIgnoreCase("view") && args.length >= 2) {
			viewShop(player, args[1]);
		} else if (args[0].equalsIgnoreCase("manageshop") && args.length >= 2) {
			manageShop(player, args[1]);
		} else if (args[0].equalsIgnoreCase("managestock") && args.length >= 2) {
			manageStock(player, args[1]);
		} else if (args[0].equalsIgnoreCase("createshop") && args.length >= 2) {
			createShop(player, args[1]);
		} else {
			listSubCmd(player, label);
		}

		return true;
	}

	private void listSubCmd(Player player, String label) {
		player.sendMessage(ChatColor.GOLD + "TradingShops Commands:");
		player.sendMessage(ChatColor.GRAY + "/" + label + " create");
		player.sendMessage(ChatColor.GRAY + "/" + label + " delete");
		player.sendMessage(ChatColor.GRAY + "/" + label + " deleteid <id>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " list");
		player.sendMessage(ChatColor.GRAY + "/" + label + " manage <id>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " stock");
		player.sendMessage(ChatColor.GRAY + "/" + label + " view <id>");
		if (player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(ChatColor.GRAY + "/" + label + " adminshop");
			player.sendMessage(ChatColor.GRAY + "/" + label + " createshop <player>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " list <player>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " manageshop <id>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " managestock <player>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " reload");
		}
	}

	private void createStore(Player player) {
		if(!player.hasPermission(Permission.SHOP_CREATE.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(!TradingShops.config.getBoolean("enableShopBlock")) {
			player.sendMessage(Messages.DISABLED_SHOP_BLOCK.toString());
			return;
		}

		Block block = player.getTargetBlock((Set<Material>) null, 5);
		String shopBlock = TradingShops.config.getString("shopBlock");

		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }

			if(match == null) {
				match = Material.JUKEBOX;
			}
		}
		boolean isShopLoc;
		if(TradingShops.wgLoader != null)
			isShopLoc = TradingShops.wgLoader.checkRegion(block);
		else
			isShopLoc = true;

		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(shop.isPresent() || !isShopLoc) {
			player.sendMessage(Messages.EXISTING_SHOP.toString());
			return;
		}

		if(!block.getType().equals(match)) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}

		int maxShops = 0;
		String permPrefix = Permission.SHOP_LIMIT_PREFIX.toString();
		for(PermissionAttachmentInfo attInfo : player.getEffectivePermissions()) {
			String perm = attInfo.getPermission();
			if(perm.startsWith(permPrefix)) {
				int num = 0;
				try {
					num = Integer.parseInt(perm.substring(perm.lastIndexOf(".")+1));
				} catch(Exception e) { num = 0; }
					if(num > maxShops)
						maxShops = num;
			}
		}

		boolean limitShops = true;
		int numShops = Shop.getNumShops(player.getUniqueId());
		if(TradingShops.config.getBoolean("usePermissions")) {
			limitShops = numShops >= maxShops;
		} else {
			int numConfig = TradingShops.config.getInt("defaultShopLimit");
			limitShops = numShops >= numConfig && numConfig >= 0;
		}

		if(player.hasPermission(Permission.SHOP_LIMIT_BYPASS.toString()))
			limitShops = false;

		if(limitShops) {
			player.sendMessage(Messages.SHOP_MAX.toString());
			return;
		}

		double cost = TradingShops.config.getDouble("createCost");
		Optional<Economy> economy = TradingShops.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
			EconomyResponse res = economy.get().withdrawPlayer(offPlayer, cost);
			if(!res.transactionSuccess()) {
				player.sendMessage(Messages.SHOP_CREATE_NO_MONEY.toString()+cost);
				return;
			}
		}

		Shop newShop = Shop.createShop(block.getLocation(), player.getUniqueId());
		player.sendMessage(Messages.SHOP_CREATED.toString());
		InvAdminShop inv = new InvAdminShop(newShop);
		inv.open(player, newShop.getOwner());
	}

	private void createShop(Player player, String playerShop) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(!TradingShops.config.getBoolean("enableShopBlock")) {
			player.sendMessage(Messages.DISABLED_SHOP_BLOCK.toString());
			return;
		}

		Block block = player.getTargetBlock((Set<Material>) null, 5);
		String shopBlock = TradingShops.config.getString("shopBlock");

		Material match = Material.matchMaterial(shopBlock);
			if(match == null) {
				try {
					match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
				} catch(Exception ignored) { }

				if(match == null) {
					match = Material.JUKEBOX;
				}
			}

		if(!block.getType().equals(match)) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}
		UUID shopOwner;
		if(playerShop == null) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		} else {
			try {
				shopOwner = getUUID(playerShop);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TradingShops] " +  Messages.NO_PLAYER_FOUND.toString());
				return;
			}
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(shopOwner);
		if(!offlinePlayer.hasPlayedBefore()) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(!shop.isPresent()) {
			Shop newShop = Shop.createShop(block.getLocation(), shopOwner);
			player.sendMessage(Messages.PLAYER_SHOP_CREATED.toString()
					.replaceAll("%p", playerShop));
		} else {
			player.sendMessage(Messages.EXISTING_SHOP.toString());
		}
	}

	private void adminShop(Player player) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		if(!TradingShops.config.getBoolean("enableAdminShop")) {
			player.sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
			return;
		}

		Block block = player.getTargetBlock((Set<Material>) null, 5);
		String shopBlock = TradingShops.config.getString("shopBlock");

		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }

			if(match == null)
				match = Material.JUKEBOX;
		}

		if(!block.getType().equals(match)) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(shop.isPresent()) {
			player.sendMessage(Messages.EXISTING_SHOP.toString());
			return;
		}

		Shop newShop = Shop.createShop(block.getLocation(), player.getUniqueId(), true);
		player.sendMessage(Messages.SHOP_CREATED.toString());
		InvAdminShop inv = new InvAdminShop(newShop);
		inv.open(player, newShop.getOwner());
	}

	private void deleteShop(Player player) {
		Block block = player.getTargetBlock((Set<Material>) null, 5);
		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}

		if(shop.get().isAdmin()) {
			if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
				player.sendMessage(Messages.NO_PERMISSION.toString());
				return;
			}
		} else if(!shop.get().isOwner(player.getUniqueId()) && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}

		double cost = TradingShops.config.getDouble("returnAmount");
		Optional<Economy> economy = TradingShops.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(shop.get().getOwner());
			economy.get().depositPlayer(offPlayer, cost);
		}

		shop.get().deleteShop();
		player.sendMessage(Messages.SHOP_DELETED.toString());
	}

	private void deleteShopID(Player player, String shopId) {
		int sID = -1;
		try {
			sID = Integer.parseInt(shopId);
		} catch (Exception e) { sID = -1; }

		if(sID < 0) {
			player.sendMessage(Messages.SHOP_ID_INTEGER.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopById(sID);
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}

		if(!shop.get().isOwner(player.getUniqueId()) && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
				player.sendMessage(Messages.SHOP_NO_SELF.toString());
				return;
		}

		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}

		if(shop.get().isAdmin()) {
			if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
				player.sendMessage(Messages.NO_PERMISSION.toString());
				return;
			}
		}

		double cost = TradingShops.config.getDouble("returnAmount");
		Optional<Economy> economy = TradingShops.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
			economy.get().depositPlayer(offPlayer, cost);
		}

		shop.get().deleteShop();
		player.sendMessage(Messages.SHOP_IDDELETED.toString()
				.replaceAll("%id", shopId));
	}

	private void listShops(Player player, String playerName) {
		if(playerName != null && !TradingShops.config.getBoolean("publicListCommand") && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		UUID sOwner;
		if(playerName == null) {
			sOwner = player.getUniqueId();
			playerName = player.getDisplayName();
		} else {
			try {
				sOwner = getUUID(playerName);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_SHOP.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TradingShops] " +  Messages.NO_PLAYER_SHOP.toString());
				return;
			}
		}
		Shop.getShopList(player, sOwner, playerName);
	}

	private void stockShop(Player player) {
		if(!TradingShops.config.getBoolean("enableStockCommand")) {
			player.sendMessage(Messages.STOCK_COMMAND_DISABLED.toString());
			return;
		}

		if(InvStock.inShopInv.containsValue(player.getUniqueId())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		} else { InvStock.inShopInv.put(player, player.getUniqueId()); }

		InvStock inv = InvStock.getInvStock(player.getUniqueId());
		inv.open(player);
	}

	private void reloadShop(Player player) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		TradingShops plugin = (TradingShops) Bukkit.getPluginManager().getPlugin("TradingShops");
		if(plugin != null)
			plugin.createConfig();
		player.sendMessage(Messages.SHOP_RELOAD.toString());
	}

	private static UUID getUUID(String name) throws Exception {
		Scanner scanner = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream());
		String input = scanner.nextLine();
		scanner.close();

		JSONObject UUIDObject = (JSONObject) JSONValue.parseWithException(input);
		String uuidString = UUIDObject.get("id").toString();
		String uuidSeparation = uuidString.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
		return UUID.fromString(uuidSeparation);
	}

	private static void shopManage(Player player, String shopID) {
		if(!TradingShops.config.getBoolean("remoteManage")) {
			player.sendMessage(Messages.SHOP_REMOTE.toString());
			return;
		}

		int shopId = -1;

		try {
			shopId = Integer.parseInt(shopID);
		} catch (Exception e) { shopId = -1; }

		if(shopId < 0) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopById(shopId);
		if((!shop.isPresent() || !shop.get().isOwner(player.getUniqueId()))) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}

		if(shop.get().isAdmin() && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}

		InvAdminShop inv = new InvAdminShop(shop.get());
		inv.open(player, shop.get().getOwner());
	}

	private static void viewShop(Player player, String shopId) {
		if(!TradingShops.config.getBoolean("remoteShopping")) {
			player.sendMessage(Messages.SHOP_NO_REMOTE.toString());
			return;
		}

		int sID = -1;
		try {
			sID = Integer.parseInt(shopId);
		} catch (Exception e) { sID = -1; }

		if(sID < 0) {
			player.sendMessage(Messages.SHOP_ID_INTEGER.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopById(sID);
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}

		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}

		if((shop.get().isAdmin() && player.hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(player.getUniqueId())) {
			InvAdminShop inv = new InvAdminShop(shop.get());
			inv.open(player, shop.get().getOwner());
		} else {
			InvShop inv = new InvShop(shop.get());
			inv.open(player, shop.get().getOwner());
		}
	}

	private void manageStock(Player player, String stockOwner) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		UUID sOwner;
		if(stockOwner == null) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		} else {
			try {
				sOwner = getUUID(stockOwner);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TradingShops] " +  Messages.NO_PLAYER_FOUND.toString());
				return;
			}
		}

		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(sOwner);
		if(!offlinePlayer.hasPlayedBefore()) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		}

		if(InvStock.inShopInv.containsValue(sOwner)) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		} else { InvStock.inShopInv.put(player, sOwner); }

		InvStock inv = InvStock.getInvStock(sOwner);
		inv.open(player);
	}

	private void manageShop(Player player, String shopId) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}

		int sID = -1;
		try {
			sID = Integer.parseInt(shopId);
		} catch (Exception e) { sID = -1; }

		if(sID < 0) {
			player.sendMessage(Messages.SHOP_ID_INTEGER.toString());
			return;
		}

		Optional<Shop> shop = Shop.getShopById(sID);
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}

		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}

		InvAdminShop inv = new InvAdminShop(shop.get());
		inv.open(player, shop.get().getOwner());
	}
}

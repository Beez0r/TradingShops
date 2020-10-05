package es.elzoo.tradingshops;

import es.elzoo.tradingshops.inventories.InvAdminShop;
import es.elzoo.tradingshops.inventories.InvShop;
import es.elzoo.tradingshops.inventories.InvStock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventShop implements Listener {
	public static final List<String> playersCreating = new ArrayList<>();
	public static final List<String> playersCreatingAdmin = new ArrayList<>();
	public static final List<String> playersDeleting = new ArrayList<>();
	private static boolean isShopLoc = false;
	
	@EventHandler
	public void onPlayerInteractStock(PlayerInteractEvent event) {
		if(!TradingShops.config.getBoolean("enableStockBlock"))
			return;

		Block block = event.getClickedBlock();
		String stockBlock = TradingShops.config.getString("stockBlock");

		Material match = Material.matchMaterial(stockBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(stockBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception e) {
				match = null;
			}

			if(match == null) {
				match = Material.JUKEBOX;
			}
		}

		if(block == null || !block.getType().equals(match))
			return;

		// WorldGuard Check
		if(TradingShops.wgLoader != null)
			isShopLoc = TradingShops.wgLoader.checkRegion(block);
		else
			isShopLoc = true;

		if(!isShopLoc)
			return;

		if(event.getPlayer().isSneaking())
			return;

		event.setCancelled(true);

		if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if(InvStock.inShopInv.containsValue(event.getPlayer().getUniqueId())) {
				event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				return;
			} else { InvStock.inShopInv.put(event.getPlayer(), event.getPlayer().getUniqueId()); }

			InvStock inv = InvStock.getInvStock(event.getPlayer().getUniqueId());
			inv.setPag(0);
			inv.open(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(!TradingShops.config.getBoolean("enableShopBlock"))
			return;

		Block block = event.getClickedBlock();
		String shopBlock = TradingShops.config.getString("shopBlock");

		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception e) {
				match = null;
			}

			if(match == null) {
				match = Material.JUKEBOX;
			}
		}

		if(block == null || !block.getType().equals(match)) {
			return;
		}

		if(TradingShops.wgLoader != null)
			isShopLoc = TradingShops.wgLoader.checkRegion(block);
		else
			isShopLoc = true;

		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(!shop.isPresent() || !isShopLoc)
			return;

		event.setCancelled(true);
		if(InvStock.inShopInv.containsValue(shop.get().getOwner()) && (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
			event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}

		if((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
			InvAdminShop inv = new InvAdminShop(shop.get());
			inv.open(event.getPlayer(), shop.get().getOwner());
		} else {
			InvShop inv = new InvShop(shop.get());
			inv.open(event.getPlayer(), shop.get().getOwner());
		}
	}
}

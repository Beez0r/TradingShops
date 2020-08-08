package es.elzoo.tradingshops.inventories;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import es.elzoo.tradingshops.RowStore;
import es.elzoo.tradingshops.Messages;
import es.elzoo.tradingshops.Shop;
import es.elzoo.tradingshops.gui.GUI;

public class InvAdminShop extends GUI {
	private final Shop shop;
	
	public InvAdminShop(Shop shop) {
		super(54, getShopName(shop));
		
		this.shop = shop;
		updateItems();
	}
	
	private static String getShopName(Shop shop) {
		if(shop.isAdmin())
			return Messages.SHOP_TITLE_ADMIN_SHOP.toString();
		
		String msg = Messages.SHOP_TITLE_NORMAL_SHOP.toString();
		OfflinePlayer pl = Bukkit.getOfflinePlayer(shop.getOwner());
		if(pl == null)
			return msg.replaceAll("%player%", "<unknown>");
		
		return msg.replaceAll("%player%", Objects.requireNonNull(pl.getName()));
	}
	
	private void updateItems() {
		for(int x=0; x<9; x++) {
			for(int y=0; y<6; y++) {
				if(x == 1) {
					if(y == 0 || y == 5) {
						placeItem(y*9+x, GUI.createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN+ Messages.SHOP_TITLE_SELL.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemOut());
						}
					}
				} else if(x == 4) {
					if(y == 0 || y == 5) {
						placeItem(y*9+x, GUI.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED+ Messages.SHOP_TITLE_BUY.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemIn());
						}
					}
				} else if(x == 7 && y >= 1 && y <= 4) {
					Optional<RowStore> row = shop.getRow(y-1);
					final int index = y-1;
					
					if(row.isPresent()) {
						placeItem(y*9+x, GUI.createItem(Material.TNT, ChatColor.BOLD+ Messages.SHOP_TITLE_DELETE.toString()), p -> {
							shop.delete(p, index);
							InvAdminShop inv = new InvAdminShop(shop);
							inv.open(p);
						});
					} else {
						placeItem(y*9+x, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD+ Messages.SHOP_TITLE_CREATE.toString()), p -> {
							InvCreateRow inv = new InvCreateRow(shop, index);
							inv.open(p);
						});
					}
				} else if(x == 8 && y >= 1 && y <= 4 && shop.isAdmin()) {
					final Optional<RowStore> row = shop.getRow(y-1);
					if(row.isPresent()) {
						if(row.get().broadcast) {
							placeItem(y*9+x, GUI.createItem(Material.REDSTONE_TORCH, Messages.SHOP_TITLE_BROADCAST_ON.toString(), (short) 15), p -> {
								row.get().toggleBroadcast();
								updateItems();
							});
						} else {
							placeItem(y*9+x, GUI.createItem(Material.LEVER, Messages.SHOP_TITLE_BROADCAST_OFF.toString(), (short) 15), p -> {
								row.get().toggleBroadcast();
								updateItems();
							});
						}
					} else {
						placeItem(y*9+x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
					}
				} else {
					placeItem(y*9+x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
				}
			}
		}
	}
}

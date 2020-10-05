package es.elzoo.tradingshops;

import org.bukkit.ChatColor;

public enum Messages {
	ADMIN_SHOP_DISABLED("adminShopDisabled"),
	DISABLED_SHOP_BLOCK("disabledShopBlock"),
	EXISTING_SHOP("existingShop"),
	NO_PERMISSION("noPermissions"),
	NO_PLAYER_SHOP("noPlayerShop"),
	NO_PLAYER_FOUND("noPlayerFound"),
	PLAYER_INV_FULL("playerInventoryFull"),
	PLAYER_SHOP_CREATED("playerShopCreated"),
	SHOP_BUSY("shopBusy"),
	SHOP_CREATED("shopCreated"),
	SHOP_CREATE_NO_MONEY("noMoney"),
	SHOP_DELETED("shopDeleted"),
	SHOP_IDDELETED("shopIDDeleted"),
	SHOP_ID_INTEGER("shopIntegerError"),
	SHOP_MAX("shopLimit"),
	SHOP_NO_ITEMS("noItems"),
	SHOP_NO_SELF("shopNotOwned"),
	SHOP_NO_STOCK("noStock"),
	SHOP_NO_STOCK_SHELF("noStockNotify"),
	SHOP_NOT_FOUND("noShopFound"),
	SHOP_PAGE("page"),
	SHOP_PURCHASE("buy"),
	SHOP_RELOAD("reload"),
	SHOP_REMOTE("noRemoteManage"),
	SHOP_SELL("sell"),
	SHOP_TITLE_ADMIN_SHOP("adminShop"),
	SHOP_TITLE_BROADCAST_OFF("broadcastOff"),
	SHOP_TITLE_BROADCAST_ON("broadcastOn"),
	SHOP_TITLE_BUY("buyTitle"),
	SHOP_TITLE_BUYACTION("buyAction"),
	SHOP_TITLE_CREATE("createTitle"),
	SHOP_TITLE_CREATESHOP("createShopTitle"),
	SHOP_TITLE_DELETE("deleteTitle"),
	SHOP_TITLE_NORMAL_SHOP("normalShop"),
	SHOP_TITLE_SELL("sellTitle"),
	SHOP_TITLE_STOCK("stockTitle"),
	STOCK_COMMAND_DISABLED("stockCommandDisabled"),
	TARGET_MISMATCH("targetMismatch");

	private final String msg;

	Messages(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		String trans = TradingShops.config.getString(msg);
		if(trans == null)
			trans = "&cError retrieving config message!";
		return ChatColor.translateAlternateColorCodes('&', trans);
	}
}

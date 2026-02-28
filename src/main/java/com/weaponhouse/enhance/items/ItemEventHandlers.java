package com.weaponhouse.enhance.items;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
public class ItemEventHandlers {
    public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
        EnhancePillHandler.onEntityRightClick(event);
    }
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        EnhancePillHandler.onItemRightClick(event);
    }
}
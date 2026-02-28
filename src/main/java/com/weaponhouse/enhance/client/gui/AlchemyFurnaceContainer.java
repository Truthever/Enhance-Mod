package com.weaponhouse.enhance.client.gui;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
public class AlchemyFurnaceContainer extends Container {
    private final AlchemyFurnaceTileEntity tileEntity;
    private final BlockPos pos;
    public static final int SLOT_COUNT = 7;
    public static final int PLAYER_INVENTORY_START = SLOT_COUNT;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;
    public static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    public static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END;
    private static final int[][] SLOT_POSITIONS = {
            {84, 4},
            {55, 21},
            {113, 20},
            {84, 34},
            {55, 49},
            {113, 49},
            {84, 64}
    };
    public AlchemyFurnaceContainer(int windowId, PlayerInventory playerInventory, PacketBuffer extraData) {
        this(windowId, playerInventory, extraData.readBlockPos());
    }
    public AlchemyFurnaceContainer(int windowId, PlayerInventory playerInventory, BlockPos pos) {
        super(RegistryHandler.ALCHEMY_FURNACE_CONTAINER.get(), windowId);
        this.pos = pos;
        World world = playerInventory.player.world;
        this.tileEntity = (AlchemyFurnaceTileEntity) world.getTileEntity(pos);
        if (this.tileEntity == null) {
            throw new IllegalStateException("Tile entity not found at " + pos);
        }
        IItemHandler itemHandler = tileEntity.getItemHandler();
        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slotIndex = i;
            this.addSlot(new SlotItemHandler(itemHandler,
                    slotIndex, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return isItemValidForSlot(slotIndex, stack);
                }
            });
        }
        addPlayerInventory(playerInventory);
    }
    private boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case 0:
                return stack.getItem() == RegistryHandler.ENHANCE_DUST.get();
            case 3:
                return stack.getItem() == RegistryHandler.GREEN_GIFT.get() ||
                        stack.getItem() == RegistryHandler.BLUE_GIFT.get() ||
                        stack.getItem() == RegistryHandler.RED_GIFT.get() ||
            stack.getItem() == RegistryHandler.PURPLE_GIFT.get();
            case 6:
                return stack.getItem() == Items.GLASS_BOTTLE;
            default:
                return true;
        }
    }
    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = 8 + col * 18;
                int y = 84 + row * 18;
                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            int y = 142;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }
    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return tileEntity.isUsableByPlayer(playerIn);
    }
    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < SLOT_COUNT) {
                if (!this.mergeItemStack(itemstack1, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            } else {
                boolean transferred = false;
                for (int i = 0; i < SLOT_COUNT; i++) {
                    Slot furnaceSlot = this.inventorySlots.get(i);
                    if (!furnaceSlot.getHasStack() && furnaceSlot.isItemValid(itemstack1)) {
                        if (this.mergeItemStack(itemstack1, i, i + 1, false)) {
                            transferred = true;
                            break;
                        }
                    }
                }
                if (!transferred) {
                    if (index < PLAYER_HOTBAR_START) {
                        if (!this.mergeItemStack(itemstack1, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index < PLAYER_INVENTORY_END) {
                        if (!this.mergeItemStack(itemstack1, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }
    public AlchemyFurnaceTileEntity getTileEntity() {
        return tileEntity;
    }
    public BlockPos getPos() {
        return pos;
    }
}
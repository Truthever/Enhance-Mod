package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.client.gui.AlchemyFurnaceContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import java.util.function.Supplier;
public class OpenAlchemyFurnacePacket {
    private final BlockPos pos;
    public OpenAlchemyFurnacePacket(BlockPos pos) {
        this.pos = pos;
    }
    public static void encode(OpenAlchemyFurnacePacket msg, PacketBuffer buffer) {
        buffer.writeBlockPos(msg.pos);
    }
    public static OpenAlchemyFurnacePacket decode(PacketBuffer buffer) {
        return new OpenAlchemyFurnacePacket(buffer.readBlockPos());
    }
    public static void handle(OpenAlchemyFurnacePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                NetworkHooks.openGui(player, new INamedContainerProvider() {
                    @Override
                    public ITextComponent getDisplayName() {
                        return new StringTextComponent("Alchemy Furnace");
                    }
                    @Override
                    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
                        return new AlchemyFurnaceContainer(windowId, playerInventory, msg.pos);
                    }
                }, msg.pos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
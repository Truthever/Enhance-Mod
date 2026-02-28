package com.weaponhouse.enhance.network;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import com.weaponhouse.enhance.client.gui.AlchemyFurnaceTileEntity;
import java.util.function.Supplier;
public class AlchemyFurnaceSyncPacket {
    private final BlockPos pos;
    private final boolean isCrafting;
    private final int craftingProgress;
    private final int craftingTotalTime;
    public AlchemyFurnaceSyncPacket(BlockPos pos, boolean isCrafting, int craftingProgress, int craftingTotalTime) {
        this.pos = pos;
        this.isCrafting = isCrafting;
        this.craftingProgress = craftingProgress;
        this.craftingTotalTime = craftingTotalTime;
    }
    public static void encode(AlchemyFurnaceSyncPacket msg, PacketBuffer buffer) {
        buffer.writeBlockPos(msg.pos);
        buffer.writeBoolean(msg.isCrafting);
        buffer.writeInt(msg.craftingProgress);
        buffer.writeInt(msg.craftingTotalTime);
    }
    public static AlchemyFurnaceSyncPacket decode(PacketBuffer buffer) {
        return new AlchemyFurnaceSyncPacket(
                buffer.readBlockPos(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readInt()
        );
    }
    public static void handle(AlchemyFurnaceSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().world != null) {
                TileEntity te = Minecraft.getInstance().world.getTileEntity(msg.pos);
                if (te instanceof AlchemyFurnaceTileEntity) {
                    AlchemyFurnaceTileEntity furnace = (AlchemyFurnaceTileEntity) te;
                    furnace.updateClientState(msg.isCrafting, msg.craftingProgress, msg.craftingTotalTime);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
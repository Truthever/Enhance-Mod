package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.client.BlockLinkRenderer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;
public class BlockLinkEffectPacket {
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final boolean create;
    private final int color;
    public BlockPos getPos1() { return pos1; }
    public BlockPos getPos2() { return pos2; }
    public boolean isCreate() { return create; }
    public int getColor() { return color; }
    public BlockLinkEffectPacket(BlockPos pos1, BlockPos pos2, boolean create, int color) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.create = create;
        this.color = color;
    }
    public static void encode(BlockLinkEffectPacket msg, PacketBuffer buffer) {
        buffer.writeBlockPos(msg.pos1);
        buffer.writeBlockPos(msg.pos2);
        buffer.writeBoolean(msg.create);
        buffer.writeInt(msg.color);
    }
    public static BlockLinkEffectPacket decode(PacketBuffer buffer) {
        return new BlockLinkEffectPacket(
                buffer.readBlockPos(),
                buffer.readBlockPos(),
                buffer.readBoolean(),
                buffer.readInt()
        );
    }
    public static void handle(BlockLinkEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.isCreate()) {
                BlockLinkRenderer.addLink(msg.getPos1(), msg.getPos2(), msg.getColor());
            } else {
                BlockLinkRenderer.removeLink(msg.getPos1(), msg.getPos2());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
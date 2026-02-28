
package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import com.weaponhouse.enhance.session.ServerBlockSession;
import com.weaponhouse.enhance.session.ServerSessionManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
public class ExchangeBuffPacket {
    private final BlockPos sessionPos;
    private final UUID initiatorUUID;
    private final UUID targetUUID;
    private final int rowIndex;
    private final ExchangeDirection direction;
    private final String initiatorBuffName;
    private final String targetBuffName;
    public ExchangeBuffPacket(BlockPos sessionPos, UUID initiatorUUID, UUID targetUUID,
                              int rowIndex, ExchangeDirection direction,
                              String initiatorBuffName, String targetBuffName) {
        this.sessionPos = sessionPos;
        this.initiatorUUID = initiatorUUID;
        this.targetUUID = targetUUID;
        this.rowIndex = rowIndex;
        this.direction = direction;
        this.initiatorBuffName = initiatorBuffName;
        this.targetBuffName = targetBuffName;
    }
    public static void encode(ExchangeBuffPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.sessionPos);
        buffer.writeUniqueId(packet.initiatorUUID);
        buffer.writeUniqueId(packet.targetUUID);
        buffer.writeInt(packet.rowIndex);
        buffer.writeEnumValue(packet.direction); // 枚举直接写入
        buffer.writeString(packet.initiatorBuffName, 32767);
        buffer.writeString(packet.targetBuffName, 32767);
    }
    public static ExchangeBuffPacket decode(PacketBuffer buffer) {
        BlockPos sessionPos = buffer.readBlockPos();
        UUID initiatorUUID = buffer.readUniqueId();
        UUID targetUUID = buffer.readUniqueId();
        int rowIndex = buffer.readInt();
        ExchangeDirection direction = buffer.readEnumValue(ExchangeDirection.class);
        String initiatorBuffName = buffer.readString(32767);
        String targetBuffName = buffer.readString(32767);
        return new ExchangeBuffPacket(sessionPos, initiatorUUID, targetUUID,
                rowIndex, direction, initiatorBuffName, targetBuffName);
    }
    public static void handle(ExchangeBuffPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity initiator = ctx.get().getSender();
            if (initiator == null || initiator.server == null) {
                return;
            }
            ServerSessionManager sessionManager = ServerSessionManager.getInstance();
            ServerBlockSession session = sessionManager.getSession(packet.sessionPos);
            if (session == null) {
                return;
            }
            Set<UUID> sessionPlayers = session.getPlayers();
            if (!sessionPlayers.contains(packet.initiatorUUID)) {
                return;
            }
            if (!sessionPlayers.contains(packet.targetUUID)) {
                return;
            }
            ServerPlayerEntity target = null;
            for (ServerPlayerEntity onlinePlayer : initiator.server.getPlayerList().getPlayers()) {
                if (onlinePlayer.getUniqueID().equals(packet.targetUUID)) {
                    target = onlinePlayer;
                    break;
                }
            }
            if (target == null) {
                return;
            }
            CompoundNBT initiatorNBT = initiator.getPersistentData();
            CompoundNBT targetNBT = target.getPersistentData();
            CompoundNBT initiatorBuffs = initiatorNBT.contains("WeaponHouseBuffs")
                    ? initiatorNBT.getCompound("WeaponHouseBuffs")
                    : new CompoundNBT();
            CompoundNBT targetBuffs = targetNBT.contains("WeaponHouseBuffs")
                    ? targetNBT.getCompound("WeaponHouseBuffs")
                    : new CompoundNBT();
            boolean initiatorHadLife = initiatorBuffs.contains("life");
            boolean initiatorHadAttack = initiatorBuffs.contains("attack");
            boolean targetHadLife = targetBuffs.contains("life");
            boolean targetHadAttack = targetBuffs.contains("attack");
            int initiatorLifeLevel = initiatorHadLife ? initiatorBuffs.getInt("life") : 0;
            int initiatorAttackLevel = initiatorHadAttack ? initiatorBuffs.getInt("attack") : 0;
            int targetLifeLevel = targetHadLife ? targetBuffs.getInt("life") : 0;
            int targetAttackLevel = targetHadAttack ? targetBuffs.getInt("attack") : 0;
            String buffToExchange;
            int buffLevel;
            switch (packet.direction) {
                case LEFT_TO_RIGHT:
                    if (initiatorBuffs.contains(packet.initiatorBuffName)) {
                        buffToExchange = packet.initiatorBuffName;
                        buffLevel = initiatorBuffs.getInt(buffToExchange);
                        initiatorBuffs.remove(buffToExchange);
                        initiatorNBT.put("WeaponHouseBuffs", initiatorBuffs);
                        targetBuffs.putInt(buffToExchange, buffLevel);
                        targetNBT.put("WeaponHouseBuffs", targetBuffs);
                    } else {
                        return;
                    }
                    break;
                case RIGHT_TO_LEFT:
                    if (targetBuffs.contains(packet.targetBuffName)) {
                        buffToExchange = packet.targetBuffName;
                        buffLevel = targetBuffs.getInt(buffToExchange);
                        targetBuffs.remove(buffToExchange);
                        targetNBT.put("WeaponHouseBuffs", targetBuffs);
                        initiatorBuffs.putInt(buffToExchange, buffLevel);
                        initiatorNBT.put("WeaponHouseBuffs", initiatorBuffs);
                    } else {
                        return;
                    }
                    break;
            }
            boolean needSyncInitiatorLife = initiatorHadLife != initiatorBuffs.contains("life") ||
                    (initiatorHadLife && initiatorLifeLevel != (initiatorBuffs.contains("life") ? initiatorBuffs.getInt("life") : 0));
            boolean needSyncInitiatorAttack = initiatorHadAttack != initiatorBuffs.contains("attack") ||
                    (initiatorHadAttack && initiatorAttackLevel != (initiatorBuffs.contains("attack") ? initiatorBuffs.getInt("attack") : 0));
            boolean needSyncTargetLife = targetHadLife != targetBuffs.contains("life") ||
                    (targetHadLife && targetLifeLevel != (targetBuffs.contains("life") ? targetBuffs.getInt("life") : 0));
            boolean needSyncTargetAttack = targetHadAttack != targetBuffs.contains("attack") ||
                    (targetHadAttack && targetAttackLevel != (targetBuffs.contains("attack") ? targetBuffs.getInt("attack") : 0));
            if (needSyncInitiatorLife) {
                LifeHandler.syncLifeBuff(initiator);
            }
            if (needSyncInitiatorAttack) {
                AttackHandler.syncAttackBuff(initiator);
            }
            if (needSyncTargetLife) {
                LifeHandler.syncLifeBuff(target);
            }
            if (needSyncTargetAttack) {
                AttackHandler.syncAttackBuff(target);
            }
            SendBuffPacket.sendBuffData(initiator);
            SendBuffPacket.sendBuffData(target);
        });
        ctx.get().setPacketHandled(true);
    }
    public enum ExchangeDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }
}
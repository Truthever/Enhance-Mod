package com.weaponhouse.enhance.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.util.Constants;
import com.weaponhouse.enhance.util.RegistryHandler;
public class EnhanceLandPortalFrameTileEntity extends TileEntity {
    private ItemStack pillStack = ItemStack.EMPTY;
    private String pillType;
    public EnhanceLandPortalFrameTileEntity() {
        this(RegistryHandler.ENHANCE_LAND_PORTAL_FRAME_TE.get());
    }
    public EnhanceLandPortalFrameTileEntity(TileEntityType<?> type) {
        super(type);
    }
    public boolean hasPill() {
        return !pillStack.isEmpty();
    }
    public ItemStack getPillStack() {
        return pillStack.copy();
    }
    public void setPillStack(ItemStack stack) {
        this.pillStack = stack.copy();
        this.pillStack.setCount(1);
        this.pillType = extractPillTypeFromStack(stack);
        this.markDirty();
    }
    public ItemStack removePill() {
        ItemStack stack = pillStack.copy();
        pillStack = ItemStack.EMPTY;
        pillType = null;
        this.markDirty();
        return stack;
    }
    public String getPillType() {
        return pillType;
    }
    private String extractPillTypeFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getTag() != null && stack.hasTag() && stack.getTag().contains("EnhancePillBuffs")) {
            CompoundNBT pillBuffs = stack.getTag().getCompound("EnhancePillBuffs");
            if (pillBuffs.contains("PillTextureType", Constants.NBT.TAG_STRING)) {
                return pillBuffs.getString("PillTextureType");
            }
            return determinePillTypeFromBuffs(pillBuffs);
        }
        return null;
    }
    private String determinePillTypeFromBuffs(CompoundNBT pillBuffs) {
        int attackCount = 0;
        int lifeCount = 0;
        int defenseCount = 0;
        int speedCount = 0;
        for (String buffKey : pillBuffs.keySet()) {
            if (!pillBuffs.contains(buffKey, Constants.NBT.TAG_INT)) {
                continue;
            }
            switch (buffKey) {
                case "attack":
                case "annihilation":
                case "combo":
                case "thunder":
                case "vampire":
                case "phantom":
                    attackCount++;
                    break;
                case "life":
                case "inspiration":
                case "harmony":
                case "photosynthesis":
                case "unyielding":
                    lifeCount++;
                    break;
                case "defense":
                case "spirit_shield":
                case "thorns":
                case "frost":
                case "aura":
                    defenseCount++;
                    break;
                case "speed":
                case "displacement":
                case "fasting":
                case "tracking":
                    speedCount++;
                    break;
            }
        }
        if (attackCount > lifeCount && attackCount > defenseCount && attackCount > speedCount) {
            return "ATTACK";
        } else if (lifeCount > attackCount && lifeCount > defenseCount && lifeCount > speedCount) {
            return "LIFE";
        } else if (defenseCount > attackCount && defenseCount > lifeCount && defenseCount > speedCount) {
            return "DEFENSE";
        } else if (speedCount > attackCount && speedCount > lifeCount && speedCount > defenseCount) {
            return "SPEED";
        } else {
            return "HARMONY";
        }
    }
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.contains("Pill", Constants.NBT.TAG_COMPOUND)) {
            pillStack = ItemStack.read(nbt.getCompound("Pill"));
        } else {
            pillStack = ItemStack.EMPTY;
        }
        if (nbt.contains("PillType", Constants.NBT.TAG_STRING)) {
            pillType = nbt.getString("PillType");
        } else {
            pillType = extractPillTypeFromStack(pillStack);
        }
    }
    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        if (!pillStack.isEmpty()) {
            nbt.put("Pill", pillStack.write(new CompoundNBT()));
        }
        if (pillType != null) {
            nbt.putString("PillType", pillType);
        }
        return nbt;
    }

}
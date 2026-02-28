package com.weaponhouse.enhance.events;
import com.weaponhouse.enhance.client.ClientConfigCache;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.lang.reflect.Field;
import java.util.*;
@Mod.EventBusSubscriber
public class PlayerRespawnHandler {
    private static Field flySpeedField=null;
    public static final String BUFF_TAG="WeaponHouseBuffs";
    public static final String BUFF_SNAPSHOT_TAG="WeaponHouseBuffs_DeathSnapshot";
    public static final String BUFF_APPLY_TAG="WeaponHouseBuffs_ApplyOnRespawn";
    private static final String ENHANCE_CANDY_TAG="EnhanceCandy";
    private static final List<String> EXCLUDE_DEFAULT_BUFFS=Arrays.asList("one_enhance","two_enhance","three_enhance","enhance_level");
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event){
        if(!(event.getEntityLiving() instanceof ServerPlayerEntity))return;
        ServerPlayerEntity player=(ServerPlayerEntity)event.getEntityLiving();
        CompoundNBT data=player.getPersistentData();
        CompoundNBT buffs=data.contains(BUFF_TAG)?data.getCompound(BUFF_TAG):new CompoundNBT();
        CompoundNBT snapshot=new CompoundNBT();
        for(String k:buffs.keySet())snapshot.putInt(k,buffs.getInt(k));
        data.put(BUFF_SNAPSHOT_TAG,snapshot);
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event){
        ServerPlayerEntity oldPlayer = (ServerPlayerEntity) event.getOriginal();
        ServerPlayerEntity newPlayer = (ServerPlayerEntity) event.getPlayer();
        if(!event.isWasDeath()){
            CompoundNBT oldData = oldPlayer.getPersistentData();
            CompoundNBT newData = newPlayer.getPersistentData();
            if(oldData.contains(BUFF_TAG)){
                newData.put(BUFF_TAG, oldData.getCompound(BUFF_TAG).copy());
            }
            if(oldData.contains("PlayerSpawn")) newData.put("PlayerSpawn", oldData.getCompound("PlayerSpawn"));
            if(oldData.contains("BaseMaxHealth")) newData.putFloat("BaseMaxHealth", oldData.getFloat("BaseMaxHealth"));
            if(oldData.contains("BaseAttackDamage")) newData.putFloat("BaseAttackDamage", oldData.getFloat("BaseAttackDamage"));
            if(oldData.contains("naturalArmor")) newData.putFloat("naturalArmor", oldData.getFloat("naturalArmor"));
            if(oldData.contains("BaseMovementSpeed")) newData.putFloat("BaseMovementSpeed", oldData.getFloat("BaseMovementSpeed"));
            if(oldData.contains(ENHANCE_CANDY_TAG)) newData.putBoolean(ENHANCE_CANDY_TAG, oldData.getBoolean(ENHANCE_CANDY_TAG));
            try{
                float oldFlySpeed = getPlayerFlySpeed(oldPlayer);
                newData.putFloat("CustomFlySpeed", oldFlySpeed);
            }catch(Exception ignored){}

            return;
        }
        ServerPlayerEntity deadPlayer=(ServerPlayerEntity)event.getOriginal();
        CompoundNBT oldData=deadPlayer.getPersistentData();
        CompoundNBT oldBuffsSnapshot=oldData.contains(BUFF_SNAPSHOT_TAG)?oldData.getCompound(BUFF_SNAPSHOT_TAG):(oldData.contains(BUFF_TAG)?oldData.getCompound(BUFF_TAG):new CompoundNBT());
        CompoundNBT modifiedBuffs=new CompoundNBT();
        for(String key:oldBuffsSnapshot.keySet())modifiedBuffs.putInt(key,oldBuffsSnapshot.getInt(key));
        List<String> validBuffs=new ArrayList<>();
        for(String key:modifiedBuffs.keySet()){
            if(!EXCLUDE_DEFAULT_BUFFS.contains(key)&&modifiedBuffs.getInt(key)>0)validBuffs.add(key);
        }
        boolean hasCandy=oldData.getBoolean(ENHANCE_CANDY_TAG);
        boolean candyUsed=false;
        int threshold=Math.max(0,ClientConfigCache.getDeathPenaltyThreshold());
        if(ClientConfigCache.isDeathPenaltyEnabled()){
            if(validBuffs.size()>threshold){
                if(hasCandy){
                    candyUsed=true;
                    newPlayer.getPersistentData().remove(ENHANCE_CANDY_TAG);
                    newPlayer.sendMessage(new TranslationTextComponent("message.enhance_candy.death_penalty_blocked").mergeStyle(TextFormatting.GOLD),newPlayer.getUniqueID());
                }else{
                    String removedKey=validBuffs.get(new Random().nextInt(validBuffs.size()));
                    int removedLevel=modifiedBuffs.getInt(removedKey);
                    handleSpecialBuffRemoval(newPlayer,removedKey);
                    modifiedBuffs.remove(removedKey);
                    newPlayer.sendMessage(new TranslationTextComponent("message.player_respawn.death_penalty.lose_buff",getBuffName(removedKey),removedLevel).mergeStyle(TextFormatting.RED),newPlayer.getUniqueID());
                }
            }else if(!validBuffs.isEmpty()){
                newPlayer.sendMessage(new TranslationTextComponent("message.player_respawn.death_penalty.exempt",threshold).mergeStyle(TextFormatting.GREEN),newPlayer.getUniqueID());
            }
        }else{
            if(!validBuffs.isEmpty()){
                newPlayer.sendMessage(new TranslationTextComponent("message.player_respawn.death_penalty.disabled").mergeStyle(TextFormatting.GREEN),newPlayer.getUniqueID());
            }
        }
        if(hasCandy&&!candyUsed)newPlayer.getPersistentData().putBoolean(ENHANCE_CANDY_TAG,true);
        CompoundNBT newData=newPlayer.getPersistentData();
        newData.put(BUFF_APPLY_TAG,modifiedBuffs);
        if(oldData.contains("PlayerSpawn"))newData.put("PlayerSpawn",oldData.getCompound("PlayerSpawn"));
        if(oldData.contains("BaseMaxHealth"))newData.putFloat("BaseMaxHealth",oldData.getFloat("BaseMaxHealth"));
        if(oldData.contains("BaseAttackDamage"))newData.putFloat("BaseAttackDamage",oldData.getFloat("BaseAttackDamage"));
        if(oldData.contains("naturalArmor"))newData.putFloat("naturalArmor",oldData.getFloat("naturalArmor"));
        if(oldData.contains("BaseMovementSpeed"))newData.putFloat("BaseMovementSpeed",oldData.getFloat("BaseMovementSpeed"));
        if(!hasAnyHealthBuff(modifiedBuffs)&&newData.contains("BaseMaxHealth"))newData.remove("BaseMaxHealth");
        if(!hasAnyAttackBuff(modifiedBuffs)&&newData.contains("BaseAttackDamage"))newData.remove("BaseAttackDamage");
        try{
            float oldFlySpeed=getPlayerFlySpeed(deadPlayer);
            newData.putFloat("CustomFlySpeed",oldFlySpeed);
        }catch(Exception e){
            e.fillInStackTrace();
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event){
        ServerPlayerEntity player=(ServerPlayerEntity)event.getPlayer();
        CompoundNBT playerData=player.getPersistentData();
        if(playerData.contains(BUFF_APPLY_TAG)){
            CompoundNBT apply=playerData.getCompound(BUFF_APPLY_TAG);
            CompoundNBT finalBuffs=new CompoundNBT();
            for(String k:apply.keySet())finalBuffs.putInt(k,apply.getInt(k));
            playerData.put(BUFF_TAG,finalBuffs);
            playerData.remove(BUFF_APPLY_TAG);
        }
        playerData.remove(BUFF_SNAPSHOT_TAG);
        if(playerData.contains("PlayerSpawn")){
            CompoundNBT spawnData=playerData.getCompound("PlayerSpawn");
            int spawnX=spawnData.getInt("SpawnX");
            int spawnY=spawnData.getInt("SpawnY");
            int spawnZ=spawnData.getInt("SpawnZ");
            String spawnDimension=spawnData.getString("SpawnDimension");
            try{
                List<ServerWorld> worlds=getLoadedWorlds();
                ServerWorld targetWorld=null;
                for(ServerWorld world:worlds){
                    RegistryKey<World> dimensionKey=world.getDimensionKey();
                    String dimensionId=dimensionKey.getLocation().toString();
                    if(dimensionId.equals(spawnDimension)){
                        targetWorld=world;
                        break;
                    }
                }
                if(targetWorld!=null){
                    BlockPos targetPos=new BlockPos(spawnX,spawnY,spawnZ);
                    if(!targetWorld.equals(player.getServerWorld()))player.teleport(targetWorld,targetPos.getX(),targetPos.getY(),targetPos.getZ(),player.rotationYaw,player.rotationPitch);
                    player.sendMessage(new TranslationTextComponent("message.player_respawn.respawn_at_location",spawnDimension,spawnX,spawnY,spawnZ),player.getUniqueID());
                }else{
                    player.sendMessage(new TranslationTextComponent("message.player_respawn.dimension_not_found",spawnDimension).mergeStyle(TextFormatting.RED),player.getUniqueID());
                }
            }catch(Exception e){
                player.sendMessage(new TranslationTextComponent("message.player_respawn.teleport_failed").mergeStyle(TextFormatting.RED),player.getUniqueID());
            }
        }
        if(playerData.contains(BUFF_TAG)){
            CompoundNBT buffs=playerData.getCompound(BUFF_TAG);
            if(buffs.contains("life")&&buffs.getInt("life")>0)LifeHandler.applyLifeBuff(player);
            if(buffs.contains("attack")&&buffs.getInt("attack")>0)AttackHandler.applyAttackBuff(player);
        }
        CompoundNBT buffs=playerData.contains(BUFF_TAG)?playerData.getCompound(BUFF_TAG):new CompoundNBT();
        if(playerData.contains("BaseMaxHealth")){
            if(hasAnyHealthBuff(buffs)){
                float baseMaxHealth=playerData.getFloat("BaseMaxHealth");
                Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(baseMaxHealth);
                player.setHealth(baseMaxHealth);
            }else{
                playerData.remove("BaseMaxHealth");
            }
        }
        if(playerData.contains("BaseAttackDamage")){
            if(hasAnyAttackBuff(buffs)){
                float baseAttackDamage=playerData.getFloat("BaseAttackDamage");
                Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(baseAttackDamage);
            }else{
                playerData.remove("BaseAttackDamage");
            }
        }
        if(playerData.contains("BaseMovementSpeed")){
            float baseMovementSpeed=playerData.getFloat("BaseMovementSpeed");
            Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(baseMovementSpeed);
        }
        if(playerData.contains("CustomFlySpeed")){
            try{
                float savedFlySpeed=playerData.getFloat("CustomFlySpeed");
                setPlayerFlySpeed(player,savedFlySpeed);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    private static boolean hasAnyHealthBuff(CompoundNBT buffs){
        return(buffs.contains("life")&&buffs.getInt("life")>0)||(buffs.contains("photosynthesis")&&buffs.getInt("photosynthesis")>0)||(buffs.contains("vampire")&&buffs.getInt("vampire")>0)||(buffs.contains("curse")&&buffs.getInt("curse")>0)||(buffs.contains("unyielding")&&buffs.getInt("unyielding")>0);
    }
    private static boolean hasAnyAttackBuff(CompoundNBT buffs){
        return(buffs.contains("attack")&&buffs.getInt("attack")>0);
    }
    private static void handleSpecialBuffRemoval(LivingEntity living,String buffType){
        switch(buffType){
            case"life":
            case"photosynthesis":
            case"vampire":
            case"curse":
            case"unyielding":
                LifeHandler.restoreOriginalMaxHealth(living,living.getAttribute(Attributes.MAX_HEALTH));
                break;
            case"attack":
                AttackHandler.restoreOriginalAttack(living,living.getAttribute(Attributes.ATTACK_DAMAGE));
                break;
            case"inspiration":
                clearInspirationMarkers(living);
                break;
        }
    }
    private static void clearInspirationMarkers(LivingEntity entity){
        CompoundNBT data=entity.getPersistentData();
        if(data.contains("InspirationMarker"))data.remove("InspirationMarker");
    }
    private static String getBuffName(String buffKey){
        return new TranslationTextComponent("buff.enhance."+buffKey).getString();
    }
    private static float getPlayerFlySpeed(ServerPlayerEntity player)throws Exception{
        PlayerAbilities abilities=player.abilities;
        if(flySpeedField==null)findFlySpeedField(abilities);
        if(flySpeedField!=null){
            flySpeedField.setAccessible(true);
            return flySpeedField.getFloat(abilities);
        }else return abilities.getFlySpeed();
    }
    private static void setPlayerFlySpeed(ServerPlayerEntity player,float speed)throws Exception{
        PlayerAbilities abilities=player.abilities;
        if(flySpeedField==null)findFlySpeedField(abilities);
        if(flySpeedField!=null){
            flySpeedField.setAccessible(true);
            flySpeedField.setFloat(abilities,speed);
        }else callSetFlySpeedMethod(abilities,speed);
        player.sendPlayerAbilities();
    }
    private static void findFlySpeedField(PlayerAbilities abilities){
        try{
            flySpeedField=findFieldByDefaultValue(abilities);
            if(flySpeedField==null){
                String[] possibleFieldNames={"flySpeed","field_75097_g","field_75096_f","d","flySpeedMultiplier"};
                for(String fieldName:possibleFieldNames){
                    try{
                        flySpeedField=abilities.getClass().getDeclaredField(fieldName);
                        break;
                    }catch(NoSuchFieldException ignored){}
                }
            }
            if(flySpeedField==null){
                for(Field field:abilities.getClass().getDeclaredFields()){
                    if(field.getType()==float.class){
                        flySpeedField=field;
                        break;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private static Field findFieldByDefaultValue(PlayerAbilities abilities){
        try{
            for(Field field:abilities.getClass().getDeclaredFields()){
                if(field.getType()==float.class){
                    field.setAccessible(true);
                    float value=field.getFloat(abilities);
                    if(Math.abs(value-(float)0.05)<0.001f)return field;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private static void callSetFlySpeedMethod(PlayerAbilities abilities,float speed){
        try{
            java.lang.reflect.Method setFlySpeed=abilities.getClass().getDeclaredMethod("setFlySpeed",float.class);
            setFlySpeed.setAccessible(true);
            setFlySpeed.invoke(abilities,speed);
        }catch(NoSuchMethodException e){
            try{
                java.lang.reflect.Method setFlySpeed=abilities.getClass().getDeclaredMethod("func_75092_a",float.class);
                setFlySpeed.setAccessible(true);
                setFlySpeed.invoke(abilities,speed);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private static List<ServerWorld> getLoadedWorlds(){
        Collection<ServerWorld> worlds=(Collection<ServerWorld>)ServerLifecycleHooks.getCurrentServer().getWorlds();
        return new ArrayList<>(worlds);
    }
}

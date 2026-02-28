package com.weaponhouse.enhance.client;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
public class ClientConfigCache{
    private static boolean bossbarEnabled=true;
    private static boolean blindMode=false;
    private static boolean portableUiEnabled=false;
    private static double maxHealthCap=100000.0D;
    private static boolean translucentMode=false;
    private static boolean disableOriginalBossBar=true;
    private static boolean deathPenaltyEnabled=true;
    private static int deathPenaltyThreshold=3;
    private static boolean fireResTier1=true;
    private static boolean fireResTier2=true;
    private static boolean fireResTier3=true;
    private static boolean noFallTier1=true;
    private static boolean noFallTier2=true;
    private static boolean noFallTier3=true;
    private static final Path CONFIG_PATH=FMLLoader.getGamePath().resolve("config").resolve("enhance").resolve("settings.json");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static{ensureConfigFileExists();loadConfigFromFile();}
    public static boolean isBossbarEnabled(){return bossbarEnabled;}
    public static void setBossbarEnabled(boolean enabled){bossbarEnabled=enabled;saveConfigToFile();}
    public static boolean isBlindModeEnabled(){return blindMode;}
    public static void setBlindModeEnabled(boolean enabled){blindMode=enabled;saveConfigToFile();}
    public static boolean isPortableUiEnabled(){return portableUiEnabled;}
    public static void setPortableUiEnabled(boolean enabled){portableUiEnabled=enabled;saveConfigToFile();}
    public static double getMaxHealthCap(){return maxHealthCap;}
    public static void setMaxHealthCap(double cap){if(cap<1.0D)cap=1.0D;maxHealthCap=cap;saveConfigToFile();}
    public static boolean isTranslucentModeEnabled(){return translucentMode;}
    public static void setTranslucentModeEnabled(boolean enabled){translucentMode=enabled;saveConfigToFile();}
    public static boolean isOriginalBossBarDisabled(){return disableOriginalBossBar;}
    public static void setOriginalBossBarDisabled(boolean disabled){disableOriginalBossBar=disabled;saveConfigToFile();}
    public static boolean isDeathPenaltyEnabled(){return deathPenaltyEnabled;}
    public static void setDeathPenaltyEnabled(boolean enabled){deathPenaltyEnabled=enabled;saveConfigToFile();}
    public static int getDeathPenaltyThreshold(){return deathPenaltyThreshold;}
    public static void setDeathPenaltyThreshold(int threshold){deathPenaltyThreshold=Math.max(0,threshold);saveConfigToFile();}
    public static boolean isFireResistanceEnabled(int tier){switch(tier){case 1:return fireResTier1;case 2:return fireResTier2;case 3:return fireResTier3;default:return true;}}
    public static void setFireResistanceEnabled(int tier,boolean enabled){switch(tier){case 1:fireResTier1=enabled;break;case 2:fireResTier2=enabled;break;case 3:fireResTier3=enabled;break;default:return;}saveConfigToFile();}
    public static boolean isNoFallDamageEnabled(int tier){switch(tier){case 1:return noFallTier1;case 2:return noFallTier2;case 3:return noFallTier3;default:return true;}}
    public static void setNoFallDamageEnabled(int tier,boolean enabled){switch(tier){case 1:noFallTier1=enabled;break;case 2:noFallTier2=enabled;break;case 3:noFallTier3=enabled;break;default:return;}saveConfigToFile();}
    private static void ensureConfigFileExists(){
        File configFile=CONFIG_PATH.toFile();
        if(!configFile.exists()){
            try{
                File parent=configFile.getParentFile();
                if(parent!=null)parent.mkdirs();
                createDefaultClientConfig(configFile);
            }catch(IOException e){
                System.err.println("Failed to create default client config: "+e.getMessage());
            }
        }
    }
    private static void createDefaultClientConfig(File configFile)throws IOException{
        Map<String,Object> defaultConfig=new HashMap<>();
        defaultConfig.put("enhance_bossbar",true);
        defaultConfig.put("blind_mode",false);
        defaultConfig.put("portable_ui_enabled",false);
        defaultConfig.put("translucent_mode",false);
        defaultConfig.put("disable_original_bossbar",true);
        defaultConfig.put("max_health_cap",100000.0);
        defaultConfig.put("death_penalty_enabled",true);
        defaultConfig.put("death_penalty_threshold",3);
        Map<String,Object> fireNode=new HashMap<>();
        fireNode.put("tier1",true);
        fireNode.put("tier2",true);
        fireNode.put("tier3",true);
        defaultConfig.put("fire_resistance",fireNode);
        Map<String,Object> fallNode=new HashMap<>();
        fallNode.put("tier1",true);
        fallNode.put("tier2",true);
        fallNode.put("tier3",true);
        defaultConfig.put("no_fall_damage",fallNode);
        try(FileWriter writer=new FileWriter(configFile)){GSON.toJson(defaultConfig,writer);}
    }
    private static void loadConfigFromFile(){
        File configFile=CONFIG_PATH.toFile();
        if(!configFile.exists())return;
        try(FileReader reader=new FileReader(configFile)){
            Type type=new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> config=GSON.fromJson(reader,type);
            setDefaultTierBooleans();
            if(config!=null){
                bossbarEnabled=getBoolean(config,"enhance_bossbar",true);
                blindMode=getBoolean(config,"blind_mode",false);
                portableUiEnabled=getBoolean(config,"portable_ui_enabled",false);
                translucentMode=getBoolean(config,"translucent_mode",false);
                disableOriginalBossBar=getBoolean(config,"disable_original_bossbar",true);
                maxHealthCap=getDouble(config);
                if(maxHealthCap<1.0D)maxHealthCap=1.0D;
                deathPenaltyEnabled=getBoolean(config,"death_penalty_enabled",true);
                deathPenaltyThreshold=Math.max(0,getInt(config));
                loadTierBooleanNode(config,"fire_resistance",true);
                loadTierBooleanNode(config,"no_fall_damage",false);
            }
        }catch(IOException e){
            System.err.println("Failed to load client config: "+e.getMessage());
            bossbarEnabled=true;
            blindMode=false;
            portableUiEnabled=false;
            translucentMode=false;
            disableOriginalBossBar=true;
            maxHealthCap=100000.0D;
            deathPenaltyEnabled=true;
            deathPenaltyThreshold=3;
            setDefaultTierBooleans();
        }
    }
    private static void setDefaultTierBooleans(){
        fireResTier1=true;fireResTier2=true;fireResTier3=true;
        noFallTier1=true;noFallTier2=true;noFallTier3=true;
    }
    @SuppressWarnings("unchecked")
    private static void loadTierBooleanNode(Map<String,Object> root,String nodeKey,boolean isFire){
        Object nodeObj=root.get(nodeKey);
        if(!(nodeObj instanceof Map))return;
        Map<String,Object> node=(Map<String,Object>)nodeObj;
        boolean t1=getBoolean(node,"tier1",true);
        boolean t2=getBoolean(node,"tier2",true);
        boolean t3=getBoolean(node,"tier3",true);
        if(isFire){fireResTier1=t1;fireResTier2=t2;fireResTier3=t3;}else{noFallTier1=t1;noFallTier2=t2;noFallTier3=t3;}
    }
    private static void saveConfigToFile(){
        try{
            Map<String,Object> config=new HashMap<>();
            config.put("enhance_bossbar",bossbarEnabled);
            config.put("blind_mode",blindMode);
            config.put("portable_ui_enabled",portableUiEnabled);
            config.put("translucent_mode",translucentMode);
            config.put("disable_original_bossbar",disableOriginalBossBar);
            config.put("max_health_cap",maxHealthCap);
            config.put("death_penalty_enabled",deathPenaltyEnabled);
            config.put("death_penalty_threshold",deathPenaltyThreshold);
            config.put("fire_resistance",buildTierBooleanJson(fireResTier1,fireResTier2,fireResTier3));
            config.put("no_fall_damage",buildTierBooleanJson(noFallTier1,noFallTier2,noFallTier3));
            try(FileReader reader=new FileReader(CONFIG_PATH.toFile())){
                Type type=new TypeToken<Map<String,Object>>(){}.getType();
                Map<String,Object> existingConfig=GSON.fromJson(reader,type);
                if(existingConfig!=null){
                    for(Map.Entry<String,Object> entry:existingConfig.entrySet()){
                        if(!config.containsKey(entry.getKey()))config.put(entry.getKey(),entry.getValue());
                    }
                }
            }catch(IOException ignored){}
            try(FileWriter writer=new FileWriter(CONFIG_PATH.toFile())){GSON.toJson(config,writer);}
        }catch(IOException e){
            System.err.println("Failed to save client config: "+e.getMessage());
        }
    }
    private static Map<String,Object> buildTierBooleanJson(boolean t1,boolean t2,boolean t3){
        Map<String,Object> node=new HashMap<>();
        node.put("tier1",t1);
        node.put("tier2",t2);
        node.put("tier3",t3);
        return node;
    }
    private static boolean getBoolean(Map<String,Object> map,String key,boolean def){
        try{
            Object v=map.get(key);
            if(v instanceof Boolean)return(Boolean)v;
            if(v instanceof String)return Boolean.parseBoolean((String)v);
            if(v instanceof Number)return((Number)v).intValue()!=0;
        }catch(Exception ignored){}
        return def;
    }
    private static int getInt(Map<String,Object> map){
        try{
            Object v=map.get("death_penalty_threshold");
            if(v instanceof Number)return((Number)v).intValue();
            if(v instanceof String)return Integer.parseInt((String)v);
        }catch(Exception ignored){}
        return 3;
    }
    private static double getDouble(Map<String,Object> map){
        try{
            Object v=map.get("max_health_cap");
            if(v instanceof Number)return((Number)v).doubleValue();
            if(v instanceof String)return Double.parseDouble((String)v);
        }catch(Exception ignored){}
        return 100000.0;
    }
}

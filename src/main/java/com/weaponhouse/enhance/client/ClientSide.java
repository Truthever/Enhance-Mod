package com.weaponhouse.enhance.client;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.client.gui.*;
import com.weaponhouse.enhance.network.*;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
@OnlyIn(Dist.CLIENT)
public class ClientSide {
    private static KeyBinding OPEN_GUI_KEY;
    private static KeyBinding OPEN_DISPLAY_KEY;
    public ClientSide() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onTextureStitch);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onTextureStitchPost);
    }
    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerKeyBindings();
            registerEventHandlers();
            RenderingRegistry.registerEntityRenderingHandler(
                    RegistryHandler.CHAOTIC_MERCHANT.get(),
                    ChaoticMerchantRenderer::new
            );
            RenderTypeLookup.setRenderLayer(RegistryHandler.ETERNAL_SACRED_FIRE.get(),
                    RenderType.getCutoutMipped());
            net.minecraft.client.gui.ScreenManager.registerFactory(
                    RegistryHandler.ALCHEMY_FURNACE_CONTAINER.get(),
                    AlchemyFurnaceScreen::new
            );
        });
    }
    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        ResourceLocation atlas = event.getMap().getTextureLocation();
        if (atlas.toString().contains("blocks") || atlas.toString().contains("block")) {
            for (int i = 0; i < 2; i++) {
                ResourceLocation texture = new ResourceLocation("enhance", "block/eternal_sacred_fire_" + i);
                event.addSprite(texture);
            }
            event.addSprite(new ResourceLocation("enhance", "block/eternal_sacred_fire"));
        }
    }
    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        for (int i = 0; i < 2; i++) {
            ResourceLocation texture = new ResourceLocation("enhance", "block/eternal_sacred_fire_" + i);
            event.getMap().getSprite(texture);
        }
    }
    @OnlyIn(Dist.CLIENT)
    private void registerKeyBindings() {
        OPEN_GUI_KEY = new KeyBinding(
                "key.enhance.open_portable_ui",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.enhance"
        );
        ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);
        OPEN_DISPLAY_KEY = new KeyBinding(
                "key.enhance.show_buffs",
                GLFW.GLFW_KEY_Z,
                "key.categories.enhance"
        );
        ClientRegistry.registerKeyBinding(OPEN_DISPLAY_KEY);
    }
    @OnlyIn(Dist.CLIENT)
    private void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new EnhancementHUD());
        MinecraftForge.EVENT_BUS.register(new com.weaponhouse.enhance.effects.DarknessEffectRenderer());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }
    @OnlyIn(Dist.CLIENT)
    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
        }
    }
    @OnlyIn(Dist.CLIENT)
    private void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_GUI_KEY == null || OPEN_DISPLAY_KEY == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (OPEN_GUI_KEY.isPressed() && event.getAction() == GLFW.GLFW_PRESS) {
            if (ClientConfigCache.isPortableUiEnabled()) {
                openEnhanceRemoveScreen(mc);
                Enhance.sendToServer(new com.weaponhouse.enhance.network.RequestBuffPacket());
            } else {
                mc.player.sendMessage(
                        new net.minecraft.util.text.TranslationTextComponent("message.enhance.portable_ui_disabled"),
                        java.util.UUID.randomUUID()
                );
            }
        }
        if (OPEN_DISPLAY_KEY.isPressed() && event.getAction() == GLFW.GLFW_PRESS) {
            if (mc.currentScreen instanceof com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen) {
                mc.displayGuiScreen(null);
            } else {
                openEnhanceDisplayScreen(mc);
                Enhance.sendToServer(new com.weaponhouse.enhance.network.RequestBuffPacket());
            }
        }
    }
    @OnlyIn(Dist.CLIENT)
    private void openEnhanceRemoveScreen(Minecraft mc) {
        mc.displayGuiScreen(new com.weaponhouse.enhance.client.gui.EnhanceRemoveScreen());
    }
    @OnlyIn(Dist.CLIENT)
    private void openEnhanceDisplayScreen(Minecraft mc) {
        com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen displayScreen =
                new com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen();
        mc.displayGuiScreen(displayScreen);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        PlayerEntity player = event.getPlayer();
        ItemStack heldItem = event.getItemStack();
        Block clickedBlock = world.getBlockState(pos).getBlock();
        if (isEnhanceBlock(clickedBlock)) {
            event.setCanceled(true);
            if (heldItem.getItem() instanceof BlockItem) {
                if (world.isRemote) {
                    player.swingArm(event.getHand());
                }
            }
            if (world.isRemote) {
                handleClientInteraction(clickedBlock, pos, player);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        World world = event.getWorld();
        PlayerEntity player = event.getPlayer();
        ItemStack heldItem = event.getItemStack();
        if (world.isRemote && heldItem.getItem() instanceof BlockItem) {
            BlockPos lookingAt = getTargetedBlockPos(player);
            if (lookingAt != null) {
                Block targetBlock = world.getBlockState(lookingAt).getBlock();
                if (isEnhanceBlock(targetBlock)) {
                    event.setCanceled(true);
                    player.swingArm(event.getHand());
                }
            }
        }
    }
    private static boolean isEnhanceBlock(Block block) {
        if (block == null) return false;
        ResourceLocation blockId = block.getRegistryName();
        return blockId != null && "enhance".equals(blockId.getNamespace()) &&
                (blockId.getPath().equals("sacrifice_block") ||
                        blockId.getPath().equals("replace_block") ||
                        blockId.getPath().equals("speed_reducer") ||
                        blockId.getPath().equals("alchemy_furnace"));
    }
    private static void handleClientInteraction(Block block, BlockPos pos, PlayerEntity player) {
        ResourceLocation blockId = block.getRegistryName();
        if (blockId == null) return;
        String path = blockId.getPath();
        Minecraft mc = Minecraft.getInstance();
        switch (path) {
            case "sacrifice_block":
                Enhance.sendToServer(new RequestBuffPacket());
                mc.enqueue(() -> {
                    if (mc.currentScreen == null) {
                        mc.displayGuiScreen(new SacrificeScreen());
                    }
                });
                break;
            case "replace_block":
                Enhance.sendToServer(new RequestSessionPacket(pos, player.getUniqueID()));
                mc.enqueue(() -> {
                    if (mc.currentScreen == null) {
                        mc.displayGuiScreen(new ReplaceScreen(pos));
                    }
                });
                break;
            case "speed_reducer":
                mc.enqueue(() -> {
                    if (mc.currentScreen == null) {
                        mc.displayGuiScreen(new SpeedReducerScreen());
                    }
                });
                break;
            case "alchemy_furnace":
                Enhance.sendToServer(new OpenAlchemyFurnacePacket(pos));
                break;
        }
    }
    private static BlockPos getTargetedBlockPos(PlayerEntity player) {
        try {
            BlockRayTraceResult rayTrace = (BlockRayTraceResult) player.pick(5.0D, 1.0F, false);
            return rayTrace.getPos();
        } catch (Exception e) {
            return null;
        }
    }
}
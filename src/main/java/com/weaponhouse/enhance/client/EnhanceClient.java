package com.weaponhouse.enhance.client;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen;
import com.weaponhouse.enhance.client.gui.EnhanceRemoveScreen;
import com.weaponhouse.enhance.network.RequestBuffPacket;
import com.weaponhouse.enhance.util.ConfigLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "enhance", value = Dist.CLIENT)
public class EnhanceClient {
    public static KeyBinding OPEN_GUI_KEY;
    public static KeyBinding OPEN_DISPLAY_KEY;

    public static void initClient() {
        OPEN_GUI_KEY = new KeyBinding(
                "key.enhance.open_portable_ui",
                GLFW.GLFW_KEY_Y,
                "key.categories.enhance"
        );
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);

        OPEN_DISPLAY_KEY = new KeyBinding(
                "key.enhance.show_buffs",
                GLFW.GLFW_KEY_Z,
                "key.categories.enhance"
        );
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(OPEN_DISPLAY_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (OPEN_GUI_KEY.isPressed() && event.getAction() == GLFW.GLFW_PRESS) {
            if (ConfigLoader.PORTABLE_UI_ENABLED) {
                mc.displayGuiScreen(new EnhanceRemoveScreen());
                Enhance.sendToServer(new RequestBuffPacket());
            } else {
                mc.player.sendMessage(
                        new net.minecraft.util.text.TranslationTextComponent("message.enhance.portable_ui_disabled"),
                        java.util.UUID.randomUUID()
                );
            }
        }

        if (OPEN_DISPLAY_KEY.isPressed() && event.getAction() == GLFW.GLFW_PRESS) {
            if (mc.currentScreen instanceof EnhanceDisplayScreen) {
                mc.displayGuiScreen(null);
            } else {
                EnhanceDisplayScreen displayScreen = new EnhanceDisplayScreen();
                mc.displayGuiScreen(displayScreen);
                Enhance.sendToServer(new RequestBuffPacket());
            }
        }
    }
}

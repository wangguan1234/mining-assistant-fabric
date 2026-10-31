package cn.wangguan.miningassistant;

import cn.wangguan.miningassistant.model.BlockFavorites;
import cn.wangguan.miningassistant.model.EmergencyPreferences;
import cn.wangguan.miningassistant.model.FoodPreferences;
import cn.wangguan.miningassistant.model.ProtectionPreferences;
import cn.wangguan.miningassistant.ui.BaritoneScreen;
import cn.wangguan.miningassistant.ui.RegionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MiningAssistantClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("mining_assistant", "tools"));
    public static final KeyMapping OPEN = new KeyMapping("key.mining_assistant.open", GLFW.GLFW_KEY_B, CATEGORY);
    public static ProtectionPreferences protection;
    public static BlockFavorites blockFavorites;
    public static FoodPreferences foodPreferences;
    public static EmergencyPreferences emergencyPreferences;
    private static Object greetedConnection;

    @Override
    public void onInitializeClient() {
        var config = FabricLoader.getInstance().getConfigDir();
        protection = new ProtectionPreferences(config.resolve("mining-assistant-protection.json"));
        blockFavorites = new BlockFavorites(config.resolve("mining-assistant-block-favorites.json"));
        foodPreferences = new FoodPreferences(config.resolve("mining-assistant-food.json"));
        emergencyPreferences = new EmergencyPreferences(config.resolve("mining-assistant-home.json"));
    }

    public static void tick(Minecraft client) {
        RegionPick.validate(client);
        LayeredClear.tick(client);
        EmergencyController.tick(client);
        ProtectionController.tick(client);
        if (client.player != null && client.level != null && client.getConnection() != null) {
            Object connection = client.getConnection().getConnection();
            if (connection != greetedConnection) {
                greetedConnection = connection;
                client.player.sendSystemMessage(Component.literal("[挖矿辅助] 按 ")
                    .append(OPEN.getTranslatedKeyMessage()).append(" 打开挖矿辅助面板。"));
            }
        }
        while (OPEN.consumeClick()) {
            if (client.player != null && client.level != null && client.gui.screen() == null) {
                if (RegionPick.active()) {
                    var screen = RegionPick.returnScreen();
                    client.gui.setScreen(screen != null ? screen : new RegionScreen(null));
                } else {
                    client.gui.setScreen(new BaritoneScreen(null));
                }
            }
        }
    }

    public static void baritoneStarted(Minecraft client) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                "[挖矿辅助] 请求已提交，请查看聊天栏确认结果。需要停止时，按 B 进入挖矿辅助面板并点“停止当前任务”。"));
        }
    }
}

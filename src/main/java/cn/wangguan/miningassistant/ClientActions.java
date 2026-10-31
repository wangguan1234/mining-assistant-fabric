package cn.wangguan.miningassistant;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.storage.LevelResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.brigadier.ParseResults;

public final class ClientActions {
    public record Pick(String value, String label) {}

    public static Object connection(Minecraft client) {
        return client.getConnection() == null ? null : client.getConnection().getConnection();
    }
    public static String dimension(Minecraft client) {
        return client.level == null ? "" : client.level.dimension().identifier().toString();
    }
    public static boolean sameContext(Minecraft client, Object expectedConnection, Object expectedWorld, String expectedDimension) {
        return expectedConnection != null && expectedWorld != null && expectedConnection == connection(client)
            && expectedWorld == client.level && expectedDimension.equals(dimension(client));
    }
    public static String scope(Minecraft client) {
        return serverScope(client) + "|dimension:" + dimension(client);
    }
    public static String serverScope(Minecraft client) {
        String place = client.getCurrentServer() != null ? "server:" + client.getCurrentServer().ip
            : client.getSingleplayerServer() != null ? "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
            : "offline";
        return client.getUser().getProfileId() + "|" + place;
    }
    public static boolean available(Minecraft client, String root) {
        return resolve(client, root) != null;
    }
    public static String resolve(Minecraft client, String root) {
        var listener = client.getConnection();
        if (listener == null) return null;
        var tree = listener.getCommands().getRoot();
        if (tree.getChild("minecraft:" + root) != null) return "minecraft:" + root;
        return tree.getChild(root) != null ? root : null;
    }
    public static String prepare(Minecraft client, String command) {
        String[] split = command.split(" ", 2);
        String root = resolve(client, split[0]);
        return root == null ? null : root + (split.length == 2 ? " " + split[1] : "");
    }
    public static String parserError(Minecraft client, String command) {
        if (client.getConnection() == null || client.player == null || client.level == null) return "请先进一个世界或服务器";
        var listener = client.getConnection();
        var result = listener.getCommands().parse(command, listener.getSuggestionsProvider());
        if (!complete(result)) {
            return "服务器没有接受这些选项，请返回检查，或确认你有权限";
        }
        return "";
    }
    public static boolean complete(ParseResults<?> result) {
        if (result.getReader().canRead()) return false;
        var context = result.getContext();
        while (context.getChild() != null) context = context.getChild();
        return context.getCommand() != null;
    }
    public static List<Pick> blocks(Minecraft client) {
        List<Pick> result = new ArrayList<>();
        BuiltInRegistries.BLOCK.entrySet().forEach(entry ->
            result.add(new Pick(entry.getKey().identifier().toString(), entry.getValue().getName().getString())));
        result.sort(Comparator.comparing(Pick::label).thenComparing(Pick::value));
        return List.copyOf(result);
    }
    public static String position(Minecraft client) {
        if (client.player == null) return "~ ~ ~";
        var pos = client.player.blockPosition();
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}


package cn.wangguan.miningassistant;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.gui.screens.Screen;
import cn.wangguan.miningassistant.model.RegionVolume;

/** Temporary selection mode. It intercepts clicks only while the user holds a stick. */
public final class RegionPick {
    private static Object connection;
    private static String dimension;
    private static Object world;
    private static Screen returnScreen;
    private static BlockPos first, second;
    private static boolean active;
    private static Object previewManager, previewSelection;
    private RegionPick() {}
    public static void begin(Minecraft mc) { begin(mc,null); }
    public static void begin(Minecraft mc, Screen origin) {
        if (mc.player==null || mc.level==null || ClientActions.connection(mc)==null) return;
        boolean same=connection==ClientActions.connection(mc) && world==mc.level && ClientActions.dimension(mc).equals(dimension);
        if(!same) {removePreview();first=null;second=null;}
        connection=ClientActions.connection(mc); dimension=ClientActions.dimension(mc);
        world=mc.level;returnScreen=origin;active=true;
        if(mc.gameMode!=null)mc.gameMode.stopDestroyBlock();
        mc.gui.setScreen(null);
        say(mc,"选区模式：手持木棍，左键点第一个角，右键点第二个角。按 "+MiningAssistantClient.OPEN.getTranslatedKeyMessage().getString()+" 返回菜单。");
    }
    public static void clear() { removePreview();active=false;first=null;second=null;connection=null;dimension=null;world=null;returnScreen=null; }
    public static void validate(Minecraft mc) {
        if (connection!=null && (connection!=ClientActions.connection(mc) || world!=mc.level || !dimension.equals(ClientActions.dimension(mc)))) clear();
    }
    public static boolean active() { return active; }
    public static Screen returnScreen() { return returnScreen; }
    public static boolean suppressAttack(Minecraft mc) {validate(mc);return active && mc.gui.screen()==null;}
    public static BlockPos first() { return first; }
    public static BlockPos second() { return second; }
    public static void finish() { active=false; }
    public static boolean click(Minecraft mc, boolean left) {
        validate(mc);
        if (!active || mc.gui.screen()!=null || mc.player==null) return false;
        if (!mc.player.getMainHandItem().is(Items.STICK)) {
            say(mc,"选区模式中，请先在主手拿木棍；按 "+MiningAssistantClient.OPEN.getTranslatedKeyMessage().getString()+" 可返回菜单。");return true;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.hitResult.getType()!=HitResult.Type.BLOCK) {
            say(mc,"请拿木棍对准一个方块，再点击选点。");return true;
        }
        BlockPos pos=hit.getBlockPos().immutable();
        if(left) first=pos; else second=pos;
        if(first!=null && second!=null) {
            if(RegionVolume.withinLimit(first.getX(),first.getY(),first.getZ(),second.getX(),second.getY(),second.getZ()).isEmpty()) {
                removePreview();say(mc,"选区超过 10 万格，请重新选择较小的范围。");
            } else if(!updatePreview()) say(mc,"坐标已记录，但没有显示选区框；请检查 Baritone 设置。");
        }
        say(mc,(left?"第一个角：":"第二个角：")+pos.getX()+", "+pos.getY()+", "+pos.getZ()+"。按 "+MiningAssistantClient.OPEN.getTranslatedKeyMessage().getString()+" 查看选区。");
        return true;
    }
    private static void say(Minecraft mc,String s) { mc.player.sendSystemMessage(Component.literal("[选区] "+s)); }
    private static void removePreview() {
        if(previewManager!=null && previewSelection!=null) try {
            Class.forName("baritone.api.selection.ISelectionManager").getMethod("removeSelection",Class.forName("baritone.api.selection.ISelection")).invoke(previewManager,previewSelection);
        } catch(ReflectiveOperationException | LinkageError ignored) { }
        previewManager=null;previewSelection=null;
    }
    private static boolean updatePreview() {
        removePreview();
        try {
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Object manager=Class.forName("baritone.api.IBaritone").getMethod("getSelectionManager").invoke(bot);
            Class<?> pos=Class.forName("baritone.api.utils.BetterBlockPos");
            Object a=pos.getConstructor(BlockPos.class).newInstance(first),b=pos.getConstructor(BlockPos.class).newInstance(second);
            Object selected=Class.forName("baritone.api.selection.ISelectionManager").getMethod("addSelection",pos,pos).invoke(manager,a,b);
            previewManager=manager;previewSelection=selected;return selected!=null;
        } catch(ReflectiveOperationException | LinkageError e) {return false;}
    }
}


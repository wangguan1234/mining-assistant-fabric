package cn.wangguan.miningassistant.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import java.lang.reflect.Method;
import java.util.List;
import cn.wangguan.miningassistant.RegionPick;
import cn.wangguan.miningassistant.ClientActions;

/** Baritone is optional; no classes from it are loaded when it is absent. */
public final class BaritoneScreen extends HelperScreen {
    private List<ClientActions.Pick> targets=List.of(new ClientActions.Pick("minecraft:diamond_ore","钻石矿石"));
    private String x="", y="", z="";
    final Object connection,world;
    final String dimension;
    private boolean ready;
    private int page;
    public BaritoneScreen(Screen parent) {
        super("Baritone 自动操作", parent);Minecraft mc=Minecraft.getInstance();
        connection=ClientActions.connection(mc);world=mc.level;dimension=ClientActions.dimension(mc);
    }
    @Override protected void init() {
        super.init();
        ready=FabricLoader.getInstance().isModLoaded("baritone");
        int left=layout.x(), w=layout.width(), half=(w-6)/2;
        paragraph(ready?"控制的是你自己的角色。开始前请检查附近地形和工具。":"没有检测到 Baritone。请安装适用于当前版本的 Baritone 后重新启动游戏。",left,35,w,2,MUTED);
        boolean compact=height<234;
        if(!compact || page==0) {
            int mineY=60, labelY=84, inputY=99, goY=121;
            String name=targets.size()==1?targets.getFirst().label():targets.size()+" 种";
            String names=String.join("、",targets.stream().map(ClientActions.Pick::label).toList());
            icon(tip(button("选择方块："+font.plainSubstrByWidth(name,Math.max(20,half-55)),left,mineY,half,()->minecraft.gui.setScreen(new BaritoneMinePickerScreen(this))),"已选："+names+"。\n可同时选择两种方块。"),blockIcon(targets.getFirst().value())).active=ready;
            icon(tip(button("设置挖矿数量",left+half+6,mineY,half,()->minecraft.gui.setScreen(new BaritoneMineOptionsScreen(this,targets))),"选择数量并检查后，才会开始挖矿。"),Items.IRON_PICKAXE).active=ready;
            paragraph("走到坐标：填写 X、Y、Z，确认后角色会自动寻路。",left,labelY,w,1,MUTED);
            int col=(w-12)/3;
            input(left,inputY,col,"X",x,v->x=v);input(left+col+6,inputY,col,"Y",y,v->y=v);input(left+2*(col+6),inputY,w-2*(col+6),"Z",z,v->z=v);
            icon(button("检查坐标并开始走路",left,goY,w,()->{
                try { int a=Integer.parseInt(x.trim()),b=Integer.parseInt(y.trim()),c=Integer.parseInt(z.trim());
                    if(Math.abs((long)a)>30000000 || Math.abs((long)c)>30000000 || b < -2048 || b > 2048) throw new NumberFormatException();
                    confirm("走到 "+a+", "+b+", "+c,"goto "+a+" "+b+" "+c);
                } catch(NumberFormatException e) { status="请填写有效的整数坐标。"; }
            }),Items.COMPASS).active=ready;
        }
        if(!compact || page==1) {
            int row=compact?60:145, tools=compact?87:169;
            if(!compact)icon(tip(button("停止当前任务",left,row,half,()->run("stop")),"停止正在进行的 Baritone 任务。"),Items.BARRIER).active=ready;
            icon(tip(button("木棍选区",compact?left:left+half+6,row,compact?w:half,()->minecraft.gui.setScreen(new RegionScreen(this))),"手持木棍选择两个角，检查后可清理区域。"),Items.STICK).active=ready;
            int third=(w-12)/3;
            icon(button("行为设置",left,tools,third,()->minecraft.gui.setScreen(new BaritoneSettingsScreen(this))),Items.COMPARATOR).active=ready;
            icon(button("跟随玩家",left+third+6,tools,third,()->minecraft.gui.setScreen(new BaritoneFollowScreen(this))),Items.PLAYER_HEAD).active=ready;
            icon(button("暂停 / 继续",left+2*(third+6),tools,w-2*(third+6),()->minecraft.gui.setScreen(new BaritoneControlScreen(this))),Items.CLOCK).active=ready;
            if(compact){
                icon(button("挖隧道",left,114,half,()->minecraft.gui.setScreen(new BaritoneTunnelScreen(this))),Items.IRON_PICKAXE).active=ready;
                icon(button("自动保护",left+half+6,114,half,()->minecraft.gui.setScreen(new ProtectionScreen(this))),Items.SHIELD).active=ready;
            }
        }
        if(compact) {
            int third=(w-12)/3;
            icon(button("返回",left,layout.footerY(),third,this::onClose),Items.ARROW);
            icon(button("停止任务",left+third+6,layout.footerY(),third,()->run("stop")),Items.BARRIER).active=ready;
            button(page==0?"更多功能":"挖矿 / 走路",left+2*(third+6),layout.footerY(),w-2*(third+6),()->{page=1-page;rebuildWidgets();});
        } else {
            int third=(w-12)/3;
            icon(button("返回上一页",left,layout.footerY(),third,this::onClose),Items.ARROW);
            icon(button("挖隧道",left+third+6,layout.footerY(),third,()->minecraft.gui.setScreen(new BaritoneTunnelScreen(this))),Items.IRON_PICKAXE).active=ready;
            icon(button("自动保护",left+2*(third+6),layout.footerY(),w-2*(third+6),()->minecraft.gui.setScreen(new ProtectionScreen(this))),Items.SHIELD).active=ready;
        }
    }
    private void input(int x,int y,int w,String name,String value,java.util.function.Consumer<String> update) {
        EditBox box=addRenderableWidget(new EditBox(font,x,y,w,20,Component.literal(name)));
        box.setMaxLength(12);box.setHint(Component.literal(name));box.setValue(value);box.setResponder(update);box.active=ready;
    }
    List<ClientActions.Pick> targets(){return targets;}
    void selectTargets(List<ClientActions.Pick> picks) {if(!picks.isEmpty() && picks.size()<=2)targets=List.copyOf(picks);}
    private void confirm(String description,String command) { minecraft.gui.setScreen(new ConfirmBaritoneScreen(this,description,command)); }
    void follow(String player,Screen from) {if(player.matches("[A-Za-z0-9_]{1,16}"))minecraft.gui.setScreen(new ConfirmBaritoneScreen(from,this,"跟随玩家 "+player,"follow player "+player,player));else status="玩家名字无效。";}
    boolean run(String command) {
        try {
            Class<?> api=Class.forName("baritone.api.BaritoneAPI");
            Object provider=api.getMethod("getProvider").invoke(null);
            Class<?> providerType=Class.forName("baritone.api.IBaritoneProvider");
            Object bot=providerType.getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> botType=Class.forName("baritone.api.IBaritone");
            Object manager=botType.getMethod("getCommandManager").invoke(bot);
            Method execute=Class.forName("baritone.api.command.manager.ICommandManager").getMethod("execute",String.class);
            boolean accepted=(boolean)execute.invoke(manager,command);
            status=accepted?"已提交，请留意游戏内提示。":"Baritone 未接受操作，请查看游戏提示。";
            return accepted;
        } catch (ReflectiveOperationException | LinkageError e) { status="无法连接 Baritone，请检查模组版本。";return false; }
    }
}


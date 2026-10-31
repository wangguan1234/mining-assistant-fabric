package cn.wangguan.miningassistant.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import cn.wangguan.miningassistant.ClientActions;

final class BaritoneControlScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private String activity="正在读取任务状态…";
    private int ticks;
    BaritoneControlScreen(BaritoneScreen parent){super("暂停或继续任务",parent);previous=parent;}
    @Override protected void init(){
        super.init();
        activity=readActivity();
        paragraph("暂停会暂时停止当前自动行为；继续会尝试恢复。彻底停止会取消当前任务。",layout.x(),40,layout.width(),3,MUTED);
        int half=(layout.width()-6)/2;
        button("暂停任务",layout.x(),85,half,()->{status=previous.run("pause")?"已请求暂停，请留意游戏提示。":"暂停请求未接受，请检查 Baritone。";});
        button("继续任务",layout.x()+half+6,85,half,()->{
            if(!ClientActions.sameContext(minecraft,previous.connection,previous.world,previous.dimension)){status="世界已改变，请重新打开菜单。";return;}
            if(previous.run("resume")){
                minecraft.gui.setScreen(null);
                if(minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal("[挖矿辅助] 已请求继续，请留意聊天栏和角色动作。"));
            } else status="继续请求未接受，请检查 Baritone。";
        });
        button("彻底停止任务",layout.x(),115,layout.width(),()->{status=previous.run("stop")?"已请求停止，请留意游戏提示。":"停止请求未接受，请检查 Baritone。";});
        button("返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose);
    }
    @Override public void tick(){super.tick();if(++ticks%20==0)activity=readActivity();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        g.text(font,font.plainSubstrByWidth(activity,layout.width()),layout.x(),68,GOLD,false);
    }
    private String readActivity(){
        try {
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> botType=Class.forName("baritone.api.IBaritone");
            var active=Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive");
            String[][] tasks={{"getMineProcess","挖矿"},{"getBuilderProcess","清除区域或挖隧道"},
                {"getFollowProcess","跟随玩家"},{"getCustomGoalProcess","走路"},
                {"getFarmProcess","种植"},{"getExploreProcess","探索"},
                {"getGetToBlockProcess","寻找方块"},{"getElytraProcess","鞘翅飞行"}};
            for(var task:tasks)if((boolean)active.invoke(botType.getMethod(task[0]).invoke(bot)))
                return "当前任务："+task[1]+"（可能正在计算或暂停）";
            return "当前没有检测到任务，请检查实际结果。";
        } catch(ReflectiveOperationException | LinkageError | RuntimeException e){return "暂时无法读取任务状态，请查看聊天栏。";}
    }
}


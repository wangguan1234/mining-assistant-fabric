package cn.wangguan.miningassistant.ui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import cn.wangguan.miningassistant.ClientActions;
import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.FoodInventory;
import cn.wangguan.miningassistant.EmergencyController;

final class ConfirmBaritoneScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private final String description, command;
    private final String followTarget;
    private final Object connection;
    private final String dimension;
    private final Object world;
    private boolean submitted;
    ConfirmBaritoneScreen(BaritoneScreen parent,String description,String command) {
        this(parent,parent,description,command,null);
    }
    ConfirmBaritoneScreen(Screen parent,BaritoneScreen actionTarget,String description,String command) {
        this(parent,actionTarget,description,command,null);
    }
    ConfirmBaritoneScreen(Screen parent,BaritoneScreen actionTarget,String description,String command,String followTarget) {
        super("确认开始",parent);previous=actionTarget;this.description=description;this.command=command;this.followTarget=followTarget;
        connection=actionTarget.connection;dimension=actionTarget.dimension;world=actionTarget.world;
    }
    @Override protected void init() {
        super.init();
        String risk=command.startsWith("tunnel ")?"寻路也可能改变隧道外的方块。":"可能挖掘或放置方块。";
        paragraph("即将"+description+"。Baritone 会控制你的角色移动，"+risk+"请检查周围环境。",layout.x(),45,layout.width(),4,MUTED);
        paragraph(FoodInventory.summary(minecraft),layout.x(),90,layout.width(),1,MUTED);
        button("检查食物和顺序",layout.x(),108,layout.width(),()->minecraft.gui.setScreen(new FoodSelectionScreen(this)));
        int half=(layout.width()-6)/2;
        button("返回修改",layout.x(),layout.footerY(),half,this::onClose);
        button("确认开始",layout.x()+half+6,layout.footerY(),half,()->{
            if(submitted)return;
            if(EmergencyController.active()){status="正在处理紧急回家，请先取消本次返程。";return;}
            if(!ClientActions.sameContext(minecraft,connection,world,dimension)){status="世界已改变，请返回重新选择。";return;}
            if(followTarget!=null && minecraft.level.players().stream().noneMatch(p->p!=minecraft.player && p.getGameProfile().name().equals(followTarget))){status="对方已不在附近，请返回并刷新。";return;}
            if(command.startsWith("tunnel ") && !BuilderSafety.ready(minecraft,this))return;
            submitted=true;
            if(previous.run(command)) {
                minecraft.gui.setScreen(null);
                MiningAssistantClient.baritoneStarted(minecraft);
            } else minecraft.gui.setScreen(previous);
        });
    }
}


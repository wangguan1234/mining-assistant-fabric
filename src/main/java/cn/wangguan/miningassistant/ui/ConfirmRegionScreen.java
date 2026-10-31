package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.ClientActions;
import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.RegionPick;
import cn.wangguan.miningassistant.LayeredClear;
import cn.wangguan.miningassistant.FoodInventory;
import cn.wangguan.miningassistant.EmergencyController;
import net.minecraft.core.BlockPos;

final class ConfirmRegionScreen extends HelperScreen {
    private final BlockPos first,second;
    private final long volume;
    private final Object connection;
    private final String dimension;
    private final Object world;
    private boolean submitted;
    ConfirmRegionScreen(RegionScreen parent,BlockPos first,BlockPos second,long volume,Object connection,String dimension) {
        super("确认清除区域",parent);this.first=first;this.second=second;this.volume=volume;this.connection=connection;this.dimension=dimension;this.world=net.minecraft.client.Minecraft.getInstance().level;
    }
    @Override protected void init() {
        super.init();
        paragraph("目标范围共"+volume+"格，优先从上往下。寻路也可能改变范围外；此操作无法撤销。",layout.x(),37,layout.width(),3,RED);
        paragraph("第一个角："+first.toShortString()+"\n第二个角："+second.toShortString(),layout.x(),70,layout.width(),2,MUTED);
        paragraph(FoodInventory.summary(minecraft),layout.x(),94,layout.width(),1,MUTED);
        button("检查食物和顺序",layout.x(),110,layout.width(),()->minecraft.gui.setScreen(new FoodSelectionScreen(this)));
        int half=(layout.width()-6)/2;
        button("返回检查",layout.x(),layout.footerY(),half,this::onClose);
        button("确认清除",layout.x()+half+6,layout.footerY(),half,this::start);
    }
    private void start() {
        if(submitted) {status="任务已经提交，请勿重复点击。";return;}
        if(EmergencyController.active()){status="正在处理紧急回家，请先取消本次返程。";return;}
        if(!ClientActions.sameContext(minecraft,connection,world,dimension) || !first.equals(RegionPick.first()) || !second.equals(RegionPick.second())) {
            status="世界或选区已改变，请返回重新选择。";return;
        }
        if(!BuilderSafety.ready(minecraft,this))return;
        try {
            LayeredClear.start(minecraft,first,second);
            submitted=true;
            minecraft.gui.setScreen(null);
            MiningAssistantClient.baritoneStarted(minecraft);
        } catch(IllegalStateException e) {status=e.getMessage();}
          catch(ReflectiveOperationException | LinkageError e) {status="无法开始，请检查 Baritone 是否正常加载。";}
    }
}


package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.ClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class BuilderSafetyScreen extends HelperScreen {
    private final BuilderSafety.Inspection report;
    private final Object connection,world;
    private final String dimension;
    BuilderSafetyScreen(Screen parent,BuilderSafety.Inspection report){
        super("施工设置需要检查",parent);this.report=report;
        Minecraft mc=Minecraft.getInstance();connection=ClientActions.connection(mc);world=mc.level;dimension=ClientActions.dimension(mc);
    }
    @Override protected void init(){
        super.init();
        String detail=report.available()?String.join("、",report.descriptions()):"无法读取 Baritone 设置";
        paragraph("检测到："+detail+"。这些设置可能改变施工的位置、次数或内容，已阻止开始。",layout.x(),39,layout.width(),4,RED);
        paragraph("可选择恢复相关设置，再返回重新确认。这会影响本次游戏中的所有 Baritone 任务。",layout.x(),90,layout.width(),4,MUTED);
        int half=(layout.width()-6)/2;
        button("返回检查",layout.x(),layout.footerY(),half,this::onClose);
        button("本次恢复这些设置",layout.x()+half+6,layout.footerY(),half,()->{
            if(!ClientActions.sameContext(minecraft,connection,world,dimension)){status="世界已改变，请重新打开菜单。";return;}
            var latest=BuilderSafety.inspect();
            if(!latest.available() || !BuilderSafety.reset(latest.names())){status="未能全部恢复，请检查 Baritone 设置。";return;}
            minecraft.gui.setScreen(parent);
            if(parent instanceof HelperScreen helper)helper.status="已恢复本次设置，请再次确认。";
        }).active=report.available();
    }
}


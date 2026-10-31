package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.MiningAssistantClient;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ProtectionScreen extends HelperScreen {
    ProtectionScreen(Screen parent){super("自动保护",parent);}
    @Override protected void init(){
        super.init();
        boolean compact=height<200;
        paragraph(compact?"设置自动吃饭和被怪打后的还击。":"可设置背包里的食物顺序。被怪打后找剑或斧还击；挖掘时先停手，打完继续。",layout.x(),35,layout.width(),compact?2:4,MUTED);
        var settings=MiningAssistantClient.protection;
        if(settings==null || !settings.readable())status="保护设置文件无法读取，已保留原文件。";
        boolean ready=settings!=null && settings.readable();
        var food=CycleButton.<Boolean>builder(v->Component.literal(v?"开启":"关闭"),ready&&settings.autoEat()).withValues(false,true)
            .create(layout.x(),compact?66:82,layout.width(),20,Component.literal("自动吃东西"),(button,value)->{
                try{settings.setAutoEat(value);status="设置已保存。";}catch(Exception e){status="保存失败，请检查保护设置文件。";rebuildWidgets();}
            });food.active=ready;addRenderableWidget(food);
        var defend=CycleButton.<Boolean>builder(v->Component.literal(v?"开启":"关闭"),ready&&settings.retaliate()).withValues(false,true)
            .create(layout.x(),compact?91:108,layout.width(),20,Component.literal("被怪打后自动还击"),(button,value)->{
                try{settings.setRetaliate(value);status="设置已保存。";}catch(Exception e){status="保存失败，请检查保护设置文件。";rebuildWidgets();}
            });defend.active=ready;addRenderableWidget(defend);
        int half=(layout.width()-6)/2;
        button("食物顺序",layout.x(),compact?116:134,half,()->minecraft.gui.setScreen(new FoodSelectionScreen(this)));
        button("紧急回家",layout.x()+half+6,compact?116:134,half,()->minecraft.gui.setScreen(new EmergencyScreen(this)));
        button("返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose);
    }
}


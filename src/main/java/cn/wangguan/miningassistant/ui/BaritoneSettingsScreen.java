package cn.wangguan.miningassistant.ui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read and change the loaded Baritone's current settings only after a user action. */
final class BaritoneSettingsScreen extends HelperScreen {
    BaritoneSettingsScreen(Screen parent){super("Baritone 行为设置",parent);}
    @Override protected void init(){
        super.init();
        paragraph("开关影响寻路；清除和隧道仍会挖目标方块。仅本次游戏生效。",layout.x(),35,layout.width(),2,MUTED);
        try {
            boolean compact=height<225;int half=(layout.width()-6)/2;
            toggle("allowBreak","寻路时可挖方块",layout.x(),66,compact?half:layout.width());
            toggle("allowPlace","寻路时可放方块",compact?layout.x()+half+6:layout.x(),compact?66:92,compact?half:layout.width());
            toggle("allowSprint","允许奔跑",layout.x(),compact?92:118,layout.width());
            int radius=((Number)read("followRadius")).intValue();
            var slider=new AbstractSliderButton(layout.x(),compact?118:144,layout.width(),20,Component.empty(),Math.max(0,Math.min(1,radius/64.0))) {
                private boolean changed;
                {updateMessage();}
                @Override protected void updateMessage(){int shown=changed?(int)Math.round(value*64):radius;setMessage(Component.literal("跟随距离："+shown+" 格"+(shown<0||shown>64?"（拖动可调整）":"")));}
                @Override protected void applyValue(){try{write("followRadius",(int)Math.round(value*64));changed=true;status="设置已应用。";}catch(ReflectiveOperationException | LinkageError | RuntimeException e){status="设置失败，请检查 Baritone。";rebuildWidgets();}}
            };
            addRenderableWidget(slider);
        } catch(ReflectiveOperationException | LinkageError | RuntimeException e) {status="无法读取设置，请检查 Baritone 版本。";}
        button("返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose);
    }
    private void toggle(String key,String title,int x,int y,int width) throws ReflectiveOperationException {
        boolean selected=(boolean)read(key);
        addRenderableWidget(CycleButton.<Boolean>builder(v->Component.literal(v?"开启":"关闭"),selected).withValues(false,true)
            .create(x,y,width,20,Component.literal(title),(button,value)->{
                try {write(key,value);status="设置已应用。";}catch(ReflectiveOperationException | LinkageError | RuntimeException e){status="设置失败，请返回后重试。";rebuildWidgets();}
            }));
    }
    private static Object setting(String name) throws ReflectiveOperationException {
        Object settings=Class.forName("baritone.api.BaritoneAPI").getMethod("getSettings").invoke(null);
        return Class.forName("baritone.api.Settings").getField(name).get(settings);
    }
    private static Object read(String name) throws ReflectiveOperationException {return Class.forName("baritone.api.Settings$Setting").getField("value").get(setting(name));}
    private static void write(String name,Object value) throws ReflectiveOperationException {Class.forName("baritone.api.Settings$Setting").getField("value").set(setting(name),value);}
}


package cn.wangguan.miningassistant.ui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

final class BaritoneTunnelScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private int width=2, tunnelHeight=3, length=16;
    private String preset="普通 2×3";
    private CycleButton<String> presets;
    BaritoneTunnelScreen(BaritoneScreen parent){super("挖隧道",parent);previous=parent;}
    @Override protected void init(){
        super.init();
        paragraph("沿角色面朝的方向挖，选预设或拖动滑块。",layout.x(),35,layout.width(),2,MUTED);
        int half=(layout.width()-6)/2;
        presets=addRenderableWidget(CycleButton.<String>builder(Component::literal,preset)
            .withValues("自定义","窄道 1×2","普通 2×3","宽道 3×3")
            .create(layout.x(),57,layout.width(),20,Component.literal("尺寸预设"),(button,value)->{
                preset=value;
                switch(value){
                    case "窄道 1×2" -> {width=1;tunnelHeight=2;}
                    case "普通 2×3" -> {width=2;tunnelHeight=3;}
                    case "宽道 3×3" -> {width=3;tunnelHeight=3;}
                }
                rebuildWidgets();
            }));
        slider("宽",layout.x(),82,half,1,8,width,v->{width=v;custom();});
        slider("高",layout.x()+half+6,82,half,2,8,tunnelHeight,v->{tunnelHeight=v;custom();});
        slider("长度",layout.x(),107,layout.width(),1,64,length,v->length=v);
        button("返回上一页",layout.x(),layout.footerY(),half,this::onClose);
        button("下一步：检查",layout.x()+half+6,layout.footerY(),half,()->{
            String detail="沿角色面朝的方向挖隧道，宽 "+width+"、高 "+tunnelHeight+"、长 "+length+" 格";
            minecraft.gui.setScreen(new ConfirmBaritoneScreen(this,previous,detail,"tunnel "+tunnelHeight+" "+width+" "+length));
        });
    }
    private void custom(){preset="自定义";presets.setValue(preset);}
    private void slider(String title,int x,int y,int w,int min,int max,int current,java.util.function.IntConsumer changed){
        addRenderableWidget(new AbstractSliderButton(x,y,w,20,Component.empty(),(current-min)/(double)(max-min)){
            {updateMessage();}
            private int amount(){return min+(int)Math.round(value*(max-min));}
            @Override protected void updateMessage(){setMessage(Component.literal(title+"："+amount()+" 格"));}
            @Override protected void applyValue(){changed.accept(amount());}
        });
    }
}


package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.model.BlockSearch;
import cn.wangguan.miningassistant.ClientActions;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.util.List;

final class BaritoneMineOptionsScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private final List<ClientActions.Pick> targets;
    private final ItemStack targetIcon;
    private int amount;
    BaritoneMineOptionsScreen(BaritoneScreen parent,List<ClientActions.Pick> targets){
        super("设置挖矿数量",parent);previous=parent;this.targets=List.copyOf(targets);this.targetIcon=blockIcon(targets.getFirst().value());
    }
    @Override protected void init(){
        super.init();
        String label=String.join("、",targets.stream().map(ClientActions.Pick::label).toList());
        String hint=targets.size()==1?BlockSearch.hint(targets.getFirst().value()):"将同时寻找这两种方块。";
        paragraph("目标："+label+"。"+hint,layout.x()+22,35,layout.width()-22,2,MUTED);
        int gap=4, quarter=(layout.width()-gap*3)/4;
        int[] counts={0,1,32,64};String[] labels={"不限","1 个","32 个","64 个"};
        for(int i=0;i<counts.length;i++){
            int n=counts[i];int w=i==3?layout.width()-(quarter+gap)*3:quarter;
            button(labels[i],layout.x()+i*(quarter+gap),62,w,()->{amount=n;rebuildWidgets();});
        }
        var slider=new AbstractSliderButton(layout.x(),90,layout.width(),20,Component.empty(),amount/64.0){
            {updateMessage();}
            @Override protected void updateMessage(){int n=(int)Math.round(value*64);setMessage(Component.literal("背包相关物品："+(n==0?"不限":n+" 个")));}
            @Override protected void applyValue(){amount=(int)Math.round(value*64);}
        };
        addRenderableWidget(slider);
        icon(button("下一步：检查后开始",layout.x(),120,layout.width(),()->{
            String detail="挖掘 "+label+(amount==0?"，数量不限":"，背包里的相关物品达到 "+amount+" 个时停止（包括已有物品）");
            String command="mine "+(amount==0?"":amount+" ")+String.join(" ",targets.stream().map(ClientActions.Pick::value).toList());
            minecraft.gui.setScreen(new ConfirmBaritoneScreen(this,previous,detail,command));
        }),Items.PAPER);
        icon(button("返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose),Items.ARROW);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int x,int y,float delta){
        super.extractRenderState(graphics,x,y,delta);
        if(!targetIcon.isEmpty())graphics.fakeItem(targetIcon,layout.x()+2,37);
        graphics.text(font,font.plainSubstrByWidth("数量按背包相关物品算，包括原来就有的。",layout.width()),layout.x(),111,MUTED,false);
    }
}


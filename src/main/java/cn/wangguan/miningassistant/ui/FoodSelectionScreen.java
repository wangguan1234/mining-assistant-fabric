package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.FoodInventory;
import cn.wangguan.miningassistant.model.UiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FoodSelectionScreen extends HelperScreen {
    private final List<String> order;
    private final Map<String,FoodInventory.Food> foods=new LinkedHashMap<>();
    private final List<AbstractWidget> rows=new ArrayList<>();
    private record ItemRow(Button button,ItemStack item) {}
    private final List<ItemRow> itemRows=new ArrayList<>();
    private int page,pages;
    FoodSelectionScreen(Screen parent){
        super("选择自动吃的食物",parent);
        var prefs=MiningAssistantClient.foodPreferences;
        order=new ArrayList<>(prefs!=null && prefs.readable()?prefs.order():List.of());
        foods.putAll(FoodInventory.available(minecraft));
        for(String id:order)if(!foods.containsKey(id)){
            var key=Identifier.tryParse(id);var item=key==null?null:BuiltInRegistries.ITEM.getValue(key);
            if(item!=null)foods.put(id,new FoodInventory.Food(id,Component.translatable(item.getDescriptionId()).getString(),0,new ItemStack(item)));
        }
    }
    @Override protected void init(){
        super.init();rows.clear();itemRows.clear();
        paragraph("默认会吃副手或背包里的食物。保存自定义顺序后，只吃列表里选中的食物；点击一次加入，再点一次取消。",layout.x(),35,layout.width(),3,MUTED);
        refresh();
    }
    private void refresh(){
        for(var row:rows)removeWidget(row);rows.clear();itemRows.clear();
        List<FoodInventory.Food> entries=new ArrayList<>(foods.values());
        entries.sort((a,b)->{
            int ai=order.indexOf(a.id()),bi=order.indexOf(b.id());
            if(ai<0)ai=Integer.MAX_VALUE;if(bi<0)bi=Integer.MAX_VALUE;
            int rank=Integer.compare(ai,bi);return rank!=0?rank:a.name().compareTo(b.name());
        });
        int rowStart=61,size=Math.max(1,Math.min(8,(statusY()-rowStart-20)/26+1));
        page=UiLayout.clampPage(page,entries.size(),size);pages=Math.max(1,(entries.size()+size-1)/size);
        int small=Math.max(25,Math.min(38,layout.width()/5)),gap=4,nameWidth=layout.width()-2*(small+gap);
        for(int i=0;i<size;i++){
            int at=page*size+i;if(at>=entries.size())break;
            var food=entries.get(at);int rank=order.indexOf(food.id()),y=rowStart+i*26;
            String label=(rank<0?"[不吃] ":"[第"+(rank+1)+"] ")+food.name()+" ×"+food.count();
            Button choice=tip(button(font.plainSubstrByWidth(label,nameWidth-10),layout.x(),y,nameWidth,()->{
                if(order.contains(food.id()))order.remove(food.id());else order.add(food.id());
                status="";refresh();
            }),food.name()+"：点击"+(rank<0?"加入自动吃顺序":"移出自动吃顺序")+"。\n保存自定义顺序后，只有列表里的食物会自动使用。");
            rows.add(choice);itemRows.add(new ItemRow(choice,food.icon()));
            var up=button("↑",layout.x()+nameWidth+gap,y,small,()->{int n=order.indexOf(food.id());if(n>0){java.util.Collections.swap(order,n,n-1);refresh();}});
            up.active=rank>0;rows.add(up);
            var down=button("↓",layout.x()+nameWidth+gap+small+gap,y,small,()->{int n=order.indexOf(food.id());if(n>=0 && n+1<order.size()){java.util.Collections.swap(order,n,n+1);refresh();}});
            down.active=rank>=0 && rank+1<order.size();rows.add(down);
        }
        int part=(layout.width()-12)/4;boolean narrow=layout.width()<240;
        rows.add(button("取消",layout.x(),layout.footerY(),part,this::onClose));
        var save=button("保存",layout.x()+part+4,layout.footerY(),part,()->{
            try{MiningAssistantClient.foodPreferences.saveCustom(order);minecraft.gui.setScreen(parent);}
            catch(Exception e){status="保存失败，原设置还在。";}
        });
        save.active=MiningAssistantClient.foodPreferences!=null && MiningAssistantClient.foodPreferences.readable();rows.add(save);
        var prev=button(narrow?"上页":"上一页",layout.x()+2*(part+4),layout.footerY(),part,()->{page--;refresh();});prev.active=page>0;rows.add(prev);
        var next=button(narrow?"下页":"下一页",layout.x()+3*(part+4),layout.footerY(),layout.width()-3*(part+4),()->{page++;refresh();});next.active=page+1<pages;rows.add(next);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        for(var row:itemRows){
            int width=font.width(row.button().getMessage());
            if(!row.item().isEmpty() && row.button().getWidth()>=width+42){
                int iconX=row.button().getX()+(row.button().getWidth()-width)/2-20;
                g.fakeItem(row.item(),iconX,row.button().getY()+2);
            }
        }
        if(foods.isEmpty())g.text(font,font.plainSubstrByWidth("背包没有食物，放进食物后再打开。",layout.width()),layout.x(),64,MUTED,false);
        if(status.isEmpty())g.text(font,"已允许 "+order.size()+" 种 · "+(page+1)+"/"+pages+" 页",layout.x(),statusY(),MUTED,false);
    }
}


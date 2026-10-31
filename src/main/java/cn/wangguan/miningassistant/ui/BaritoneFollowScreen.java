package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.ClientActions;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.ArrayList;

final class BaritoneFollowScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private final List<AbstractWidget> rows=new ArrayList<>();
    private List<ClientActions.Pick> players=List.of();
    private int page,pages;
    BaritoneFollowScreen(BaritoneScreen parent){super("选择附近的玩家",parent);previous=parent;}
    @Override protected void init(){super.init();refresh();}
    private void refresh(){
        for(var row:rows)removeWidget(row);rows.clear();
        players=minecraft.level==null?List.of():minecraft.level.players().stream().filter(p->p!=minecraft.player)
            .map(p->new ClientActions.Pick(p.getGameProfile().name(),p.getGameProfile().name()))
            .sorted(java.util.Comparator.comparing(ClientActions.Pick::label,String.CASE_INSENSITIVE_ORDER)).toList();
        rows.add(button("刷新附近玩家",layout.x(),52,Math.min(150,layout.width()),this::refresh));
        int capacity=Math.max(1,Math.min(8,(layout.footerY()-90)/28));
        pages=Math.max(1,(players.size()+capacity-1)/capacity);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<capacity;i++){
            int index=page*capacity+i;if(index>=players.size())break;
            var player=players.get(index);rows.add(button(player.label(),layout.x(),80+i*28,layout.width(),()->previous.follow(player.value(),this)));
        }
        rows.add(button("返回上一页",layout.x(),layout.footerY(),90,this::onClose));
        Button prev=button("上一页",layout.x()+layout.width()-150,layout.footerY(),70,()->{page--;refresh();});prev.active=page>0;rows.add(prev);
        Button next=button("下一页",layout.x()+layout.width()-74,layout.footerY(),74,()->{page++;refresh();});next.active=page+1<pages;rows.add(next);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        g.text(font,players.isEmpty()?"请先靠近对方，再点刷新。":"点选附近玩家，确认后开始跟随。",layout.x(),35,MUTED,false);
        g.text(font,(page+1)+" / "+pages+" 页",layout.x(),layout.footerY()-13,MUTED,false);
    }
}


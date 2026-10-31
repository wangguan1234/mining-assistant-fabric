package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.ClientActions;
import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.model.BlockSearch;
import cn.wangguan.miningassistant.model.UiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;

final class BaritoneMinePickerScreen extends HelperScreen {
    private final BaritoneScreen previous;
    private final List<ClientActions.Pick> all;
    private final List<AbstractWidget> rows=new ArrayList<>();
    private record ItemRow(Button button,ItemStack icon) {}
    private final List<ItemRow> itemRows=new ArrayList<>();
    private final Map<String,Button> tabs=new HashMap<>();
    private CycleButton<String> categoryCycle;
    private boolean showTabs;
    private int rowStart;
    private static final List<String> CATEGORIES=List.of("收藏","全部","矿石","木材");
    private List<ClientActions.Pick> found=List.of();
    private final Map<String,String> pinyin=new HashMap<>();
    private final Map<String,ClientActions.Pick> selected=new LinkedHashMap<>();
    private String query="", category="收藏";
    private int page,pages;
    BaritoneMinePickerScreen(BaritoneScreen parent){
        super("选择要挖的方块",parent);previous=parent;
        all=ClientActions.blocks(minecraft).stream().filter(p->!p.value().equals("minecraft:air")).toList();
        for(var pick:parent.targets())selected.put(pick.value(),pick);
        if(MiningAssistantClient.blockFavorites==null || !MiningAssistantClient.blockFavorites.readable())category="全部";
    }
    @Override protected void init(){
        super.init();rows.clear();itemRows.clear();tabs.clear();categoryCycle=null;
        showTabs=height>=220;
        rowStart=showTabs?101:82;
        int categoryWidth=Math.min(96,layout.width()/3);
        var search=addRenderableWidget(new EditBox(font,layout.x(),40,showTabs?layout.width():layout.width()-categoryWidth-6,20,Component.literal("搜索方块")));
        search.setMaxLength(60);search.setHint(Component.literal("中文、拼音或方块代码"));search.setValue(query);
        search.setSuggestion(query.isEmpty()?"中文、拼音或代码，如 zuanshi":null);
        if(showTabs){
            int gap=4,part=(layout.width()-gap*3)/4;
            for(int i=0;i<CATEGORIES.size();i++){
                String name=CATEGORIES.get(i);int x=layout.x()+i*(part+gap),w=i==3?layout.width()-3*(part+gap):part;
                tabs.put(name,button(category.equals(name)?"["+name+"]":name,x,65,w,()->changeCategory(name)));
            }
        }else{
            categoryCycle=addRenderableWidget(CycleButton.<String>builder(Component::literal,category).withValues(CATEGORIES)
                .create(layout.x()+layout.width()-categoryWidth,40,categoryWidth,20,Component.literal("类别"),(button,value)->changeCategory(value)));
        }
        search.setResponder(v->{query=v;search.setSuggestion(v.isEmpty()?"中文、拼音或代码，如 zuanshi":null);
            if(!v.isBlank() && category.equals("收藏")){category="全部";updateCategories();}
            page=0;refresh();});
        if(MiningAssistantClient.blockFavorites==null || !MiningAssistantClient.blockFavorites.readable())status="方块收藏无法读取，原文件已保留。";
        refresh();setInitialFocus(search);
    }
    private void changeCategory(String name){category=name;page=0;updateCategories();refresh();}
    private void updateCategories(){
        if(categoryCycle!=null)categoryCycle.setValue(category);
        tabs.forEach((name,button)->button.setMessage(Component.literal(category.equals(name)?"["+name+"]":name)));
    }
    private void refresh(){
        for(var row:rows)removeWidget(row);rows.clear();itemRows.clear();
        String needle=query.strip().toLowerCase(Locale.ROOT);
        found=all.stream().filter(this::inCategory).filter(p->needle.isEmpty() || BlockSearch.matches(p.label(),p.value(),
            pinyin.computeIfAbsent(p.label(),BlockSearch::pinyin),needle)).toList();
        if(category.equals("收藏") && MiningAssistantClient.blockFavorites!=null)
            found=found.stream().sorted(Comparator.comparingInt(p->MiningAssistantClient.blockFavorites.all().indexOf(p.value()))).toList();
        int size=Math.max(1,Math.min(8,(statusY()-rowStart-20)/28+1));page=UiLayout.clampPage(page,found.size(),size);pages=Math.max(1,(found.size()+size-1)/size);
        for(int i=0;i<size;i++){
            int index=page*size+i;if(index>=found.size())break;
            var pick=found.get(index);
            boolean narrow=layout.width()<240;
            int favoriteWidth=narrow?48:68, y=rowStart+i*28, nameWidth=layout.width()-favoriteWidth-6;
            boolean chosen=selected.containsKey(pick.value());
            String display=(chosen?"[已选] ":"")+pick.label();
            var choice=tip(button(font.plainSubstrByWidth(display,Math.max(10,nameWidth-48)),layout.x(),y,nameWidth,()->{
                if(selected.containsKey(pick.value())){selected.remove(pick.value());status="";}
                else if(selected.size()<2){selected.put(pick.value(),pick);status="";}
                else status="最多选两种，请先取消一种。";
                refresh();
            }),(chosen?"已选择，再点一次取消。":"点击选择，最多选两种。")+"\n"+pick.label()+"\n"+BlockSearch.hint(pick.value())+"\n"+pick.value());
            rows.add(choice);itemRows.add(new ItemRow(choice,blockIcon(pick.value())));
            var favorites=MiningAssistantClient.blockFavorites;
            boolean saved=favorites!=null && favorites.contains(pick.value());
            var favorite=tip(button(saved?(narrow?"已收藏":"取消收藏"):"收藏",layout.x()+layout.width()-favoriteWidth,y,favoriteWidth,()->{
                try{favorites.toggle(pick.value());status="";refresh();}catch(Exception e){status="收藏未保存，请检查收藏文件。";}
            }),saved?"点击取消收藏这个方块。":"点击收藏这个方块。");
            favorite.active=favorites!=null && favorites.readable();rows.add(favorite);
        }
        int part=(layout.width()-12)/4;
        boolean narrow=layout.width()<240;
        rows.add(tip(button(narrow?"取消":"取消选择",layout.x(),layout.footerY(),part,this::onClose),"放弃这次选择并返回。"));
        var done=tip(button(narrow?"完成":"完成选择",layout.x()+part+4,layout.footerY(),part,()->{
            previous.selectTargets(List.copyOf(selected.values()));minecraft.gui.setScreen(previous);
        }),"已选 "+selected.size()+"/2："+String.join("、",selected.values().stream().map(ClientActions.Pick::label).toList())+"。点击保存并返回。");
        done.active=!selected.isEmpty();rows.add(done);
        var prev=button(narrow?"上页":"上一页",layout.x()+2*(part+4),layout.footerY(),part,()->{page--;refresh();});prev.active=page>0;rows.add(prev);
        var next=button(narrow?"下页":"下一页",layout.x()+3*(part+4),layout.footerY(),layout.width()-3*(part+4),()->{page++;refresh();});next.active=page+1<pages;rows.add(next);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        String names=selected.isEmpty()?"请至少选一种":String.join("、",selected.values().stream().map(ClientActions.Pick::label).toList());
        g.text(font,font.plainSubstrByWidth("已选 "+selected.size()+"/2："+names,layout.width()),layout.x(),showTabs?90:69,MUTED,false);
        for(var row:itemRows){
            int iconX=row.button().getX()+(row.button().getWidth()-font.width(row.button().getMessage()))/2-20;
            if(!row.icon().isEmpty())g.fakeItem(row.icon(),iconX,row.button().getY()+2);
            else if(minecraft.level!=null)g.text(font,"?",iconX+5,row.button().getY()+6,MUTED,false);
        }
        if(found.isEmpty())g.text(font,font.plainSubstrByWidth(category.equals("收藏")?"还没有收藏，切换到全部后点击收藏。":"没有找到，换个名称或类别试试。",layout.width()),layout.x(),rowStart+3,MUTED,false);
        if(status.isEmpty())g.text(font,(page+1)+" / "+pages+" 页 · "+found.size()+" 项",layout.x(),layout.footerY()-13,MUTED,false);
    }
    private boolean inCategory(ClientActions.Pick pick){
        String id=pick.value();
        return switch(category){
            case "收藏" -> MiningAssistantClient.blockFavorites!=null && MiningAssistantClient.blockFavorites.contains(id);
            case "矿石" -> id.endsWith("_ore") || id.equals("minecraft:ancient_debris");
            case "木材" -> id.endsWith("_log") || id.endsWith("_wood") || id.endsWith("_stem") || id.endsWith("_hyphae") || id.equals("minecraft:bamboo_block");
            default -> true;
        };
    }
}


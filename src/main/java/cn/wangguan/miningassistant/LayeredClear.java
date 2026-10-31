package cn.wangguan.miningassistant;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Temporarily enable Baritone's own top-down builder for one region clear. */
public final class LayeredClear {
    private static final Map<String,Object> REQUIRED=Map.of(
        "buildInLayers",true,"layerOrder",true,"layerHeight",1,"startAtLayer",0,"skipFailedLayers",false);
    private static Session session;
    private static boolean cancellationPending;
    private record Session(Object builder,Object bot,Field value,Map<String,Object> entries,Map<String,Object> before,
                           Object connection,Object world,String dimension) {}
    private LayeredClear() {}
    public static void start(Minecraft client,BlockPos first,BlockPos second)throws ReflectiveOperationException {
        if(session!=null)throw new IllegalStateException("已有平地任务，请先停止当前任务。");
        Class<?> api=Class.forName("baritone.api.BaritoneAPI");
        Object provider=api.getMethod("getProvider").invoke(null);
        Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
        Object builder=Class.forName("baritone.api.IBaritone").getMethod("getBuilderProcess").invoke(bot);
        if(active(builder))throw new IllegalStateException("已有施工任务，请先停止再开始平地。");
        Object settings=api.getMethod("getSettings").invoke(null);
        Class<?> settingsType=Class.forName("baritone.api.Settings");
        Field value=Class.forName("baritone.api.Settings$Setting").getField("value");
        Map<String,Object> entries=new LinkedHashMap<>(),before=new LinkedHashMap<>();
        for(var item:REQUIRED.entrySet()){
            Object entry=settingsType.getField(item.getKey()).get(settings),old=value.get(entry);
            if(old==null || !item.getValue().getClass().isInstance(old))throw new IllegalStateException("无法读取分层设置，请检查 Baritone 版本。");
            entries.put(item.getKey(),entry);before.put(item.getKey(),old);
        }
        Session pending=new Session(builder,bot,value,entries,before,ClientActions.connection(client),client.level,ClientActions.dimension(client));
        try{
            for(var item:REQUIRED.entrySet())value.set(entries.get(item.getKey()),item.getValue());
            Class.forName("baritone.api.process.IBuilderProcess").getMethod("clearArea",BlockPos.class,BlockPos.class).invoke(builder,first,second);
            if(!active(builder))throw new IllegalStateException("Baritone 没有开始平地，请检查聊天栏。");
            session=pending;
            cancellationPending=false;
        }catch(ReflectiveOperationException | RuntimeException | LinkageError e){restore(pending);throw e;}
    }
    public static void tick(Minecraft client){
        Session current=session;if(current==null)return;
        try{
            if(!active(current.builder)){
                boolean restored=restore(current);session=null;cancellationPending=false;
                say(client,restored?"平地任务已结束或停止，请检查现场是否有漏挖。临时设置已处理。":"平地任务已停止，但部分设置未能恢复，请检查 Baritone 设置。");
                return;
            }
            boolean changed=!ClientActions.sameContext(client,current.connection,current.world,current.dimension);
            for(var item:REQUIRED.entrySet())if(!Objects.equals(current.value.get(current.entries.get(item.getKey())),item.getValue()))changed=true;
            if(changed || cancellationPending){
                if(!cancellationPending)say(client,"世界或施工设置发生变化，正在停止平地任务。请检查现场。");
                cancellationPending=true;
                cancel(current);
                if(!active(current.builder)){
                    boolean restored=restore(current);session=null;cancellationPending=false;
                    if(!restored)say(client,"部分设置未能恢复，请检查 Baritone 设置。");
                }
            }
        }catch(ReflectiveOperationException | RuntimeException | LinkageError e){
            if(!cancellationPending)say(client,"无法检查平地任务，正在请求停止，请检查现场。");
            cancellationPending=true;
            try{
                cancel(current);
                if(!active(current.builder)){
                    boolean restored=restore(current);session=null;cancellationPending=false;
                    if(!restored)say(client,"部分设置未能恢复，请检查 Baritone 设置。");
                }
            }catch(ReflectiveOperationException | RuntimeException | LinkageError ignored){}
        }
    }
    private static boolean active(Object builder)throws ReflectiveOperationException{
        return (boolean)Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive").invoke(builder);
    }
    private static void cancel(Session s)throws ReflectiveOperationException{
        Object manager=Class.forName("baritone.api.IBaritone").getMethod("getCommandManager").invoke(s.bot);
        Class.forName("baritone.api.command.manager.ICommandManager").getMethod("execute",String.class).invoke(manager,"stop");
    }
    private static boolean restore(Session s){
        boolean success=true;
        for(var item:REQUIRED.entrySet())try{
            Object entry=s.entries.get(item.getKey());
            if(Objects.equals(s.value.get(entry),item.getValue()))s.value.set(entry,s.before.get(item.getKey()));
        }catch(ReflectiveOperationException | RuntimeException ignored){success=false;}
        return success;
    }
    private static void say(Minecraft client,String message){if(client.player!=null)client.player.sendSystemMessage(Component.literal("[挖矿辅助] "+message));}
}


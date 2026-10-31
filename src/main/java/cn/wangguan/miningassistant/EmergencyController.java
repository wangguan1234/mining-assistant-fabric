package cn.wangguan.miningassistant;

import cn.wangguan.miningassistant.model.EmergencyPreferences;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

/** Stops automation first, then verifies both server-controlled teleports. */
public final class EmergencyController {
    private enum Phase { STOPPING, GOING_HOME, HEALING, RETURNING, FAILED }
    private record Point(String dimension,double x,double y,double z) {}
    private record Session(Object connection,LocalPlayer player,Point home,Point work) {}
    private static Session session;
    private static Phase phase;
    private static long ticks,deadline,fullSince,positionSerial,requestedAfter;
    private EmergencyController() {}
    private static void say(Minecraft mc,String message){if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("[挖矿辅助] "+message));}
    public static boolean active(){return session!=null;}
    public static void onPosition(Minecraft mc){
        if(session!=null && mc.player==session.player && ClientActions.connection(mc)==session.connection)positionSerial++;
    }
    /** The client replaces its LocalPlayer when the server moves it between dimensions. */
    public static void onRespawn(Minecraft mc,ClientboundRespawnPacket packet){
        if(session==null)return;
        Point expected=phase==Phase.GOING_HOME?session.home:phase==Phase.RETURNING?session.work:null;
        String destination=packet.commonPlayerSpawnInfo().dimension().identifier().toString();
        if(expected!=null && mc.player!=null && ClientActions.connection(mc)==session.connection
            && mc.player.getUUID().equals(session.player.getUUID())
            && destination.equals(expected.dimension) && destination.equals(ClientActions.dimension(mc))){
            session=new Session(session.connection,mc.player,session.home,session.work);
        }else{
            session=null;phase=null;fullSince=0;say(mc,"检测到意外的重生或世界切换，已取消自动返回；任务保持停止。");
        }
    }
    public static void onDeath(Minecraft mc,int playerId){
        if(session!=null && mc.player!=null && mc.player.getId()==playerId){
            session=null;phase=null;fullSince=0;
        }
    }
    public static String status(){
        if(session==null)return "当前没有紧急回家任务。";
        return switch(phase){
            case STOPPING -> "正在停止自动工作。";
            case GOING_HOME -> "已请求回家，正在等服务器传送。";
            case HEALING -> "在家恢复中，满血后返回工作位置。";
            case RETURNING -> "已请求返回工作位置。";
            case FAILED -> "自动回家未完成，请手动检查。";
        };
    }
    /** Checks the command tree for the exact route used by emergency teleport. */
    public static boolean canTeleportTo(Minecraft mc,String targetDimension){
        if(mc==null || mc.player==null || mc.level==null || mc.getConnection()==null
            || targetDimension==null || targetDimension.isBlank())return false;
        Point probe=new Point(targetDimension,mc.player.getX(),mc.player.getY(),mc.player.getZ());
        return preparedTeleport(mc,probe)!=null;
    }
    public static void cancel(Minecraft mc){
        if(session!=null){session=null;phase=null;fullSince=0;say(mc,"已取消自动回家或自动返回；工作任务保持停止。");}
    }
    public static boolean allowFood(Minecraft mc){
        return session!=null && phase!=Phase.STOPPING && mc.player==session.player
            && ClientActions.connection(mc)==session.connection && mc.level!=null
            && (ClientActions.dimension(mc).equals(session.work.dimension) || ClientActions.dimension(mc).equals(session.home.dimension)) && mc.player.isAlive()
            && mc.gui.screen()==null && !mc.player.isSpectator();
    }
    public static void tick(Minecraft mc){
        ticks++;
        if(session!=null){
            if(mc.player!=session.player || ClientActions.connection(mc)!=session.connection || mc.level==null
                || !mc.player.isAlive() || (!ClientActions.dimension(mc).equals(session.work.dimension)
                    && !ClientActions.dimension(mc).equals(session.home.dimension))){
                session=null;phase=null;fullSince=0;return;
            }
            int stopped=BaritoneTasks.stopForRecovery(mc);
            if(stopped<0){fail(mc,"无法确认自动工作已停止，请手动处理。");return;}
            if(stopped==0){
                if(ticks>deadline && phase==Phase.STOPPING)fail(mc,"自动工作未能安全停止，请手动处理。");
                return;
            }
            if(mc.gui.screen()!=null)return;
            switch(phase){
                case STOPPING -> {
                    ProtectionController.prepareForTeleport(mc);
                    if(sendTeleport(mc,session.home)){
                        phase=Phase.GOING_HOME;deadline=ticks+600;say(mc,"工作已停止，正在请求传送回家。");
                    }else fail(mc,"服务器没有接受传送指令；跨维度请确认 OP 权限，或安装服务端传送插件。");
                }
                case GOING_HOME -> {
                    if(positionSerial>requestedAfter && arrived(mc,session.home)){phase=Phase.HEALING;fullSince=0;say(mc,"已回家，任务已停止；正在等待恢复满血。");}
                    else if(ticks>deadline)fail(mc,"等待回家传送超时，任务仍然停止。");
                }
                case HEALING -> {
                    if(!arrived(mc,session.home)){fail(mc,"你已离开记录的家，已取消自动返回。");return;}
                    if(mc.player.getHealth()>=mc.player.getMaxHealth()){
                        if(fullSince==0)fullSince=ticks;
                        if(ticks-fullSince>=40){
                            ProtectionController.prepareForTeleport(mc);
                            if(sendTeleport(mc,session.work)){
                                phase=Phase.RETURNING;deadline=ticks+600;say(mc,"已恢复满血，正在请求返回工作位置。");
                            }else fail(mc,"返回指令不可用，请手动返回。");
                        }
                    }else fullSince=0;
                }
                case RETURNING -> {
                    if(positionSerial>requestedAfter && arrived(mc,session.work)){
                        session=null;phase=null;fullSince=0;say(mc,"已返回原位置，任务保持停止；请检查后再重新开工。");
                    }else if(ticks>deadline)fail(mc,"等待返回传送超时，任务仍然停止。");
                }
                case FAILED -> { }
            }
            return;
        }
        if(mc.player==null || mc.level==null || mc.gameMode==null || mc.getConnection()==null || !mc.player.isAlive()
            || mc.player.isSpectator() || mc.gui.screen()!=null || RegionPick.active())return;
        var prefs=MiningAssistantClient.emergencyPreferences;
        if(prefs==null || !prefs.readable())return;
        EmergencyPreferences.Home home=prefs.home(ClientActions.serverScope(mc));
        if(home==null || !home.enabled() || mc.player.getHealth()>home.threshold())return;
        Point work=new Point(ClientActions.dimension(mc),mc.player.getX(),mc.player.getY(),mc.player.getZ());
        Point destination=new Point(home.dimension(),home.x(),home.y(),home.z());
        session=new Session(ClientActions.connection(mc),mc.player,destination,work);
        phase=Phase.STOPPING;deadline=ticks+200;fullSince=0;
        say(mc,"血量过低，正在停止工作并准备回家。");
    }
    private static boolean arrived(Minecraft mc,Point p){
        if(!ClientActions.dimension(mc).equals(p.dimension))return false;
        double dx=mc.player.getX()-p.x,dy=mc.player.getY()-p.y,dz=mc.player.getZ()-p.z;
        return dx*dx+dy*dy+dz*dz<=2.25;
    }
    private static boolean sendTeleport(Minecraft mc,Point p){
        String prepared=preparedTeleport(mc,p);
        if(prepared==null)return false;
        requestedAfter=positionSerial;
        mc.getConnection().sendCommand(prepared);return true;
    }
    private static String preparedTeleport(Minecraft mc,Point p){
        String sameDimension=String.format(Locale.ROOT,"tp %.5f %.5f %.5f",p.x,p.y,p.z);
        String vanillaCrossDimension=String.format(Locale.ROOT,
            "execute in %s run tp @s %.5f %.5f %.5f",p.dimension,p.x,p.y,p.z);
        String pluginCrossDimension=String.format(Locale.ROOT,
            "wgtpworld %s %.5f %.5f %.5f",p.dimension,p.x,p.y,p.z);
        String[] candidates=ClientActions.dimension(mc).equals(p.dimension)
            ?new String[]{sameDimension}
            :new String[]{vanillaCrossDimension,pluginCrossDimension};
        String prepared=null;
        for(String candidate:candidates){
            String parsed=ClientActions.prepare(mc,candidate);
            if(parsed!=null && ClientActions.parserError(mc,parsed).isEmpty()){
                prepared=parsed;break;
            }
        }
        return prepared;
    }
    private static void fail(Minecraft mc,String reason){
        if(phase!=Phase.FAILED){phase=Phase.FAILED;fullSince=0;say(mc,reason);}
    }
}



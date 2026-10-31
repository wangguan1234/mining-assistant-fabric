package cn.wangguan.miningassistant;

import java.util.Optional;
import java.lang.reflect.Proxy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Baritone is optional; do not link its classes at startup. */
final class BaritoneTasks {
    private static final String[] PROCESSES={"getMineProcess","getBuilderProcess","getFollowProcess","getCustomGoalProcess",
        "getFarmProcess","getExploreProcess","getGetToBlockProcess","getElytraProcess"};
    private static Object pauseManager,pauseProcess,pauseCommand;
    private static boolean pauseRequested,pauseReady;
    private BaritoneTasks() {}
    static boolean active(){
        if(!FabricLoader.getInstance().isModLoaded("baritone"))return false;
        try{
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> botType=Class.forName("baritone.api.IBaritone");
            var active=Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive");
            for(String getter:PROCESSES)if((boolean)active.invoke(botType.getMethod(getter).invoke(bot)))return true;
            Object manager=botType.getMethod("getPathingControlManager").invoke(bot);
            Optional<?> current=(Optional<?>)Class.forName("baritone.api.pathing.calc.IPathingControlManager").getMethod("mostRecentInControl").invoke(manager);
            return current.isPresent() && current.get()!=pauseProcess && (boolean)active.invoke(current.get());
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){return false;}
    }
    /** A temporary Baritone process yields control without cancelling or resuming the user's task. */
    static boolean pauseForCombat(){
        try{
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Object manager=Class.forName("baritone.api.IBaritone").getMethod("getPathingControlManager").invoke(bot);
            if(manager!=pauseManager){
                Class<?> processType=Class.forName("baritone.api.process.IBaritoneProcess");
                Class<?> commandType=Class.forName("baritone.api.process.PathingCommandType");
                Object request=commandType.getField("REQUEST_PAUSE").get(null);
                Object command=Class.forName("baritone.api.process.PathingCommand")
                    .getConstructor(Class.forName("baritone.api.pathing.goals.Goal"),commandType).newInstance(null,request);
                Object process=Proxy.newProxyInstance(processType.getClassLoader(),new Class<?>[]{processType},(self,method,args)->switch(method.getName()){
                    case "isActive" -> pauseRequested;
                    case "isTemporary" -> true;
                    case "priority" -> 1000.0;
                    case "onTick" -> {pauseReady=Boolean.TRUE.equals(args[1]);yield pauseCommand;}
                    case "onLostControl" -> {pauseRequested=false;pauseReady=false;yield null;}
                    case "displayName", "displayName0", "toString" -> "挖矿辅助：自动保护";
                    case "hashCode" -> System.identityHashCode(self);
                    case "equals" -> self==args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
                pauseCommand=command;
                Class.forName("baritone.api.pathing.calc.IPathingControlManager").getMethod("registerProcess",processType).invoke(manager,process);
                pauseManager=manager;pauseProcess=process;
            }
            pauseRequested=true;
            return true;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){pauseRequested=false;pauseReady=false;return false;}
    }
    static boolean combatPauseRequested(){return pauseRequested;}
    static boolean combatPauseReady(){return pauseRequested && pauseReady;}
    static void releaseCombatPause(){pauseRequested=false;pauseReady=false;}
    static boolean breaking(){
        try{
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Object input=Class.forName("baritone.api.IBaritone").getMethod("getInputOverrideHandler").invoke(bot);
            Class<?> inputType=Class.forName("baritone.api.utils.input.Input");
            Object left=inputType.getField("CLICK_LEFT").get(null);
            return (boolean)Class.forName("baritone.api.utils.IInputOverrideHandler").getMethod("isInputForcedDown",inputType).invoke(input,left);
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){return false;}
    }
    static boolean stopBreaking(boolean hasProgress){
        try{
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> botType=Class.forName("baritone.api.IBaritone");
            Object input=botType.getMethod("getInputOverrideHandler").invoke(bot);
            Class<?> inputType=Class.forName("baritone.api.utils.input.Input");
            Class.forName("baritone.api.utils.IInputOverrideHandler").getMethod("setInputForceState",inputType,boolean.class)
                .invoke(input,inputType.getField("CLICK_LEFT").get(null),false);
            if(hasProgress){
                Object context=botType.getMethod("getPlayerContext").invoke(bot);
                Object controller=Class.forName("baritone.api.utils.IPlayerContext").getMethod("playerController").invoke(context);
                Class<?> type=Class.forName("baritone.api.utils.IPlayerController");
                type.getMethod("setHittingBlock",boolean.class).invoke(controller,true);
                type.getMethod("resetBlockRemoving").invoke(controller);
            }
            return true;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){return false;}
    }
    /** 1 = confirmed stopped, 0 = waiting, -1 = unable to verify. */
    static int stopForRecovery(Minecraft mc){
        // Emergency teleport also works when Baritone is not installed or was not running.
        // In that case there is simply no automation to stop.
        if(!FabricLoader.getInstance().isModLoaded("baritone"))return 1;
        if(mc.gameMode==null)return -1;
        try{
            Object provider=Class.forName("baritone.api.BaritoneAPI").getMethod("getProvider").invoke(null);
            Object bot=Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> botType=Class.forName("baritone.api.IBaritone");
            Object behavior=botType.getMethod("getPathingBehavior").invoke(bot);
            Class<?> behaviorType=Class.forName("baritone.api.behavior.IPathingBehavior");
            var isActive=Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive");
            boolean taskWasActive=false;
            for(String getter:PROCESSES)if((boolean)isActive.invoke(botType.getMethod(getter).invoke(bot)))taskWasActive=true;
            boolean wasBreaking=breaking() || (taskWasActive && mc.gameMode.getDestroyStage()>=0);
            behaviorType.getMethod("cancelEverything").invoke(behavior);
            if(wasBreaking && !stopBreaking(mc.gameMode.getDestroyStage()>=0))return -1;
            for(String getter:PROCESSES)if((boolean)isActive.invoke(botType.getMethod(getter).invoke(bot)))return 0;
            if((boolean)behaviorType.getMethod("hasPath").invoke(behavior))return 0;
            Optional<?> pending=(Optional<?>)behaviorType.getMethod("getInProgress").invoke(behavior);
            return pending.isPresent() || breaking()?0:1;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){return -1;}
    }
}


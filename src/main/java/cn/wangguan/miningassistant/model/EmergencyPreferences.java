package cn.wangguan.miningassistant.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/** Home positions are scoped by player and server, not shared between worlds. */
public final class EmergencyPreferences {
    public record Home(String dimension,double x,double y,double z,float yaw,float pitch,int threshold,boolean enabled) {}
    private record Data(int version,Map<String,Home> homes) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Map<String,Home> homes=Map.of();
    private boolean readable=true;
    public EmergencyPreferences(Path file){
        this.file=file.toAbsolutePath().normalize();
        if(Files.notExists(this.file))return;
        try{
            if(Files.size(this.file)>131072)throw new IOException("oversized");
            Data d=GSON.fromJson(Files.readString(this.file,StandardCharsets.UTF_8),Data.class);
            if(d==null || d.version!=1 || d.homes==null || d.homes.size()>128)throw new IOException("invalid");
            for(var entry:d.homes.entrySet()){validScope(entry.getKey());validHome(entry.getValue());}
            homes=Map.copyOf(d.homes);
        }catch(Exception e){readable=false;homes=Map.of();}
    }
    private static void validScope(String scope)throws IOException{
        if(scope==null || scope.isBlank() || scope.length()>1000 || scope.chars().anyMatch(Character::isISOControl))throw new IOException("invalid scope");
    }
    private static void validHome(Home h)throws IOException{
        if(h==null || h.dimension==null || !h.dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
            || !Double.isFinite(h.x) || !Double.isFinite(h.y) || !Double.isFinite(h.z)
            || !Float.isFinite(h.yaw) || !Float.isFinite(h.pitch)
            || Math.abs(h.x)>30000000 || Math.abs(h.z)>30000000 || h.y < -2048 || h.y > 2048
            || h.threshold<2 || h.threshold>18)throw new IOException("invalid home");
    }
    public boolean readable(){return readable;}
    public Home home(String scope){return homes.get(scope);}
    public void save(String scope,Home home)throws IOException{
        if(!readable)throw new IOException("回家设置无法读取，已保留原文件");
        validScope(scope);validHome(home);Files.createDirectories(file.getParent());
        Path lockPath=file.resolveSibling(file.getFileName()+".lock");
        try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            FileLock acquired;
            try{acquired=channel.tryLock();}catch(OverlappingFileLockException e){throw new IOException("另一个游戏窗口正在保存");}
            if(acquired==null)throw new IOException("另一个游戏窗口正在保存");
            try(FileLock ignored=acquired){
                EmergencyPreferences latest=new EmergencyPreferences(file);
                if(!latest.readable)throw new IOException("回家设置已损坏，已保留原文件");
                Map<String,Home> next=new HashMap<>(latest.homes);next.put(scope,home);
                if(next.size()>128)throw new IOException("too many worlds");
                Path temp=Files.createTempFile(file.getParent(),"command-helper-home-",".tmp");
                try{
                    Files.writeString(temp,GSON.toJson(new Data(1,next)),StandardCharsets.UTF_8);
                    try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
                    homes=Map.copyOf(next);
                }finally{Files.deleteIfExists(temp);}
            }
        }
    }
}


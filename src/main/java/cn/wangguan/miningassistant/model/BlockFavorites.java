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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Block favorites live separately from the user's command favorites. */
public final class BlockFavorites {
    private record Data(int version,List<String> favorites) {}
    public static final List<String> DEFAULTS=List.of("minecraft:diamond_ore","minecraft:deepslate_diamond_ore","minecraft:ancient_debris");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private List<String> favorites=DEFAULTS;
    private boolean readable=true;
    public BlockFavorites(Path file){
        this.file=file.toAbsolutePath().normalize();
        if(Files.notExists(this.file))return;
        try{
            if(Files.size(this.file)>65536)throw new IOException("oversized");
            Data data=GSON.fromJson(Files.readString(this.file,StandardCharsets.UTF_8),Data.class);
            if(data==null || data.version!=1 || data.favorites==null || data.favorites.size()>256)throw new IOException("invalid");
            var unique=new LinkedHashSet<String>();
            for(String id:data.favorites){if(!valid(id))throw new IOException("invalid id");unique.add(id);}
            favorites=List.copyOf(unique);
        }catch(Exception e){readable=false;favorites=List.of();}
    }
    private static boolean valid(String id){return id!=null && id.length()<=255 && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");}
    public boolean readable(){return readable;}
    public boolean contains(String id){return favorites.contains(id);}
    public List<String> all(){return favorites;}
    public void toggle(String id)throws IOException{
        if(!valid(id))throw new IllegalArgumentException("方块名称无效");
        if(!readable)throw new IOException("收藏文件无法读取");
        boolean wanted=!contains(id);
        Files.createDirectories(file.getParent());
        Path lockFile=file.resolveSibling(file.getFileName()+".lock");
        try(FileChannel channel=FileChannel.open(lockFile,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            FileLock acquired;
            try{acquired=channel.tryLock();}catch(OverlappingFileLockException e){throw new IOException("另一个窗口正在保存",e);}
            if(acquired==null)throw new IOException("另一个窗口正在保存");
            try(FileLock ignored=acquired){
                BlockFavorites latest=new BlockFavorites(file);
                if(!latest.readable)throw new IOException("收藏文件已损坏，已保留原文件");
                var next=new ArrayList<>(latest.favorites);
                if(wanted && !next.contains(id)){if(next.size()>=256)throw new IOException("方块收藏已满");next.add(id);}
                if(!wanted)next.remove(id);
                Path temp=Files.createTempFile(file.getParent(),"command-helper-blocks-",".tmp");
                try{
                    Files.writeString(temp,GSON.toJson(new Data(1,List.copyOf(next))),StandardCharsets.UTF_8);
                    try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
                    favorites=List.copyOf(next);
                }finally{Files.deleteIfExists(temp);}
            }
        }
    }
}


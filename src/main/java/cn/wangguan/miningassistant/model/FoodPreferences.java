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
import java.util.List;

/** Unlisted foods are never consumed when a custom list is in use. */
public final class FoodPreferences {
    private record Data(int version,Boolean custom,List<String> order) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data=new Data(1,false,List.of());
    private boolean readable=true;

    public FoodPreferences(Path file){
        this.file=file.toAbsolutePath().normalize();
        if(Files.notExists(this.file))return;
        try{
            if(Files.size(this.file)>32768)throw new IOException("oversized");
            Data loaded=GSON.fromJson(Files.readString(this.file,StandardCharsets.UTF_8),Data.class);
            validate(loaded);data=new Data(1,loaded.custom,List.copyOf(loaded.order));
        }catch(Exception e){readable=false;data=new Data(1,true,List.of());}
    }
    private static void validate(Data d)throws IOException{
        if(d==null || d.version!=1 || d.custom==null || d.order==null || d.order.size()>64)throw new IOException("invalid");
        for(String id:d.order)if(id==null || id.length()>128 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IOException("invalid food id");
        if(d.order.stream().distinct().count()!=d.order.size())throw new IOException("duplicate");
    }
    public boolean readable(){return readable;}
    public boolean custom(){return data.custom;}
    public List<String> order(){return data.order;}
    public void saveCustom(List<String> order)throws IOException{
        if(!readable)throw new IOException("食物设置无法读取，已保留原文件");
        Data next=new Data(1,true,List.copyOf(order));validate(next);
        Files.createDirectories(file.getParent());
        Path lockPath=file.resolveSibling(file.getFileName()+".lock");
        try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            FileLock acquired;
            try{acquired=channel.tryLock();}catch(OverlappingFileLockException e){throw new IOException("另一个游戏窗口正在保存");}
            if(acquired==null)throw new IOException("另一个游戏窗口正在保存");
            try(FileLock ignored=acquired){
                FoodPreferences latest=new FoodPreferences(file);
                if(!latest.readable)throw new IOException("食物设置已损坏，已保留原文件");
                Path temp=Files.createTempFile(file.getParent(),"command-helper-food-",".tmp");
                try{
                    Files.writeString(temp,GSON.toJson(next),StandardCharsets.UTF_8);
                    try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
                    data=next;
                }finally{Files.deleteIfExists(temp);}
            }
        }
    }
}


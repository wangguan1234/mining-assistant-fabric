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
import java.util.function.UnaryOperator;

/** Separate from the command favorites, so upgrading does not rewrite them. */
public final class ProtectionPreferences {
    private record Data(int version,Boolean autoEat,Boolean retaliate) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data=new Data(1,true,true);
    private boolean readable=true;
    public ProtectionPreferences(Path file){
        this.file=file.toAbsolutePath().normalize();
        if(Files.notExists(this.file))return;
        try{
            if(Files.size(this.file)>32768)throw new IOException("oversized");
            Data loaded=GSON.fromJson(Files.readString(this.file,StandardCharsets.UTF_8),Data.class);
            if(loaded==null || loaded.version!=1 || loaded.autoEat==null || loaded.retaliate==null)throw new IOException("invalid");
            data=loaded;
        }catch(Exception e){readable=false;data=new Data(1,false,false);}
    }
    public boolean autoEat(){return data.autoEat;}
    public boolean retaliate(){return data.retaliate;}
    public boolean readable(){return readable;}
    public void setAutoEat(boolean value)throws IOException{update(current->new Data(1,value,current.retaliate));}
    public void setRetaliate(boolean value)throws IOException{update(current->new Data(1,current.autoEat,value));}
    private void update(UnaryOperator<Data> change)throws IOException{
        if(!readable)throw new IOException("设置文件无法读取，已保留原文件");
        Files.createDirectories(file.getParent());
        Path lockPath=file.resolveSibling(file.getFileName()+".lock");
        try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            FileLock acquired;
            try{acquired=channel.tryLock();}catch(OverlappingFileLockException e){throw new IOException("另一个游戏窗口正在保存");}
            if(acquired==null)throw new IOException("另一个游戏窗口正在保存");
            try(FileLock ignored=acquired){
                ProtectionPreferences latest=new ProtectionPreferences(file);
                if(!latest.readable)throw new IOException("设置文件已损坏，已保留原文件");
                Data next=change.apply(latest.data);
                Path temp=Files.createTempFile(file.getParent(),"command-helper-protection-",".tmp");
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


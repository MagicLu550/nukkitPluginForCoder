package net.noyark.www.nukkit.decoder;

import cn.nukkit.plugin.PluginDescription;
import java.io.File;
import java.util.*;

public class LoadMapper{

    Map<File,PluginDescription> mapper;

    public LoadMapper(Map<File,PluginDescription> mapper){
        this.mapper = mapper;
    }

    public Map<String,File> loadMapper(){
        Map<String,File> nameFile = new HashMap<>();
        for(Map.Entry<File,PluginDescription> entry:mapper.entrySet()){
            nameFile.put(entry.getValue().getName(),entry.getKey());
        }
        return nameFile;
    }
}



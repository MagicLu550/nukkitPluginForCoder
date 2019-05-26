package net.noyark.www.nukkit.decoder;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.PluginException;
import net.noyark.www.utils.api.Pool;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首先加载plugin.yml，接着加载插件
 * 加载插件，根据softdepend加载插件的前置，然后记录
 * 记录完接着继续加载插件，发现插件已经记录就不再加载
 */

public class DecodePluginManager {

    private Map<File,PluginDescription> descriptionMap = new HashMap<>();

    private RunMain main = RunMain.getMain();

    private File PLUGIN_FILE = new File(main.getDataFolder()+"/plugins/");

    private File KEY_FILE = new File(main.getDataFolder()+"/key/");

    private List<String> loaded = new ArrayList<>();

    private Map<String,File> mapper = new HashMap<>();

    private List<Plugin> allPlugins = new ArrayList<>();
    /**
     * 加载插件 策略 递归依赖
     * 一个插件->看依赖->依赖的依赖
     * @return
     */
    public void loadPlugins() {
        loadDescriptions();
        loadNameFileMapper();
        File[] plugins = PLUGIN_FILE.listFiles();
        if(plugins!=null){
            for(File plugin:plugins){
                loadPlugin(plugin.toString());
            }
        }
    }

    /**
     * 加载出Descriptions
     */
    public void loadDescriptions(){
        File[] plugins = PLUGIN_FILE.listFiles();
        if(plugins!=null){
            for(File plugin:plugins){
                getDescription(plugin.toString());
            }
        }
    }

    /**
     * 加载插件名和文件的映射
     */
    public void loadNameFileMapper(){
        LoadMapper loadMapper = new LoadMapper(descriptionMap);
        mapper.putAll(loadMapper.loadMapper());
    }


    /**
     * 加载加密的插件
     * eplugin.yml
     * @param fileName
     * @return
     */

    public void loadPlugin(String fileName){
        try{
            PluginDescription description = descriptionMap.get(fileName);
            String thisName = description.getName();
            if(!loaded.contains(thisName)){
                String main_class = description.getMain();
                URL url = new File(fileName).toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
                Config config = new Config(Config.YAML);
                //稍后重构，换成指定配置文件
                config.load(classLoader.getResourceAsStream("plugin.yml"));
                String keyFile = KEY_FILE+config.getString("key");
                Class<?> mainClass = Pool.getClassCoder().getClassInJar(fileName,main_class,keyFile,this.getClass().getClassLoader());
                List<String> dependNames = description.getSoftDepend();
                dependNames.addAll(description.getDepend());
                dependNames.addAll(description.getLoadBefore());
                for(String name:dependNames){
                    loadPlugin(mapper.get(name).toString());//递归依赖
                }
                PluginBase base = (PluginBase) mainClass.newInstance();
                base.init(main.getPluginLoader(),main.getServer(),description,new File(main.getServer().getFilePath()+"/"+description.getName()),new File(fileName));
                base.onLoad();
                main.getLogger().info(base.getName()+"is starting!");
                loaded.add(thisName);
                allPlugins.add(base);
            }
            //这里找依赖
        }catch (InstantiationException e1){
            throw new PluginException("could not load plugin",e1);
        }catch (IllegalAccessException e2){
            throw new PluginException("could not load plugin",e2);
        }catch (ClassCastException e3){
            throw new PluginException("the plugin must extends the PluginBase",e3);
        }catch (Exception e){
            throw new PluginException("unknown exception");
        }
    }

    public PluginDescription getDescription(String fileName){
        PluginDescription pluginDescription = RunMain.getMain().getPluginLoader().getPluginDescription(fileName);
        descriptionMap.put(new File(fileName),pluginDescription);
        return pluginDescription;
    }

}

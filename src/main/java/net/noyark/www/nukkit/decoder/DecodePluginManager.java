package net.noyark.www.nukkit.decoder;

import cn.nukkit.Server;
import cn.nukkit.command.CommandMap;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.SimpleCommandMap;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.PluginException;
import net.noyark.www.utils.api.Pool;
import net.noyark.www.utils.encode.Util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * 首先加载plugin.yml，接着加载插件
 * 加载插件，根据softdepend加载插件的前置，然后记录
 * 记录完接着继续加载插件，发现插件已经记录就不再加载
 */

public class DecodePluginManager  extends PluginManager{


    public DecodePluginManager(Server server, CommandMap map,PluginManager manager){
        super(server,(SimpleCommandMap) map);
        PLUGIN_FILE = new File(main.getDataFolder()+"/plugins/");
        KEY_FILE = new File(main.getDataFolder()+"/key/");
    }


    private Map<File,PluginDescription> descriptionMap = new HashMap<>();

    private RunMain main = RunMain.getMain();

    private File PLUGIN_FILE;

    private File KEY_FILE;

    private List<String> loaded = new ArrayList<>();

    private Map<String,File> mapper = new HashMap<>();

    private List<PluginBase> allPlugins = new ArrayList<>();



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
                if(plugin.toString().endsWith(".jar")) {
                    loadDecodePlugin(plugin.toString());
                }
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
                if(plugin.toString().endsWith(".jar")) {
                    getDescription(plugin.toString());
                }
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

    public void loadDecodePlugin(String fileName){
        try{
            PluginDescription description = descriptionMap.get(new File(fileName));
            if(!description.getCompatibleAPIs().contains(main.getServer().getApiVersion())){
                main.getLogger().error("无法加载插件:api版本无法兼容");
                return;
            }
            String thisName = description.getName();
            if(!loaded.contains(thisName)){
                String main_class = description.getMain();
                JarFile file = new JarFile(fileName);
                InputStream in = file.getInputStream(file.getEntry("plugin.yml"));
                Config config = new Config(Config.YAML);
                //稍后重构，换成指定配置文件
                config.load(in);
                String keyFile = KEY_FILE+"/"+config.getString("key");
                Class<?> mainClass = Pool.getAESClassCoder().getClassInJar(fileName,main_class,keyFile,this.getClass().getClassLoader());
                List<String> dependNames = description.getSoftDepend();
                dependNames.addAll(description.getDepend());
                dependNames.addAll(description.getLoadBefore());
                for(String name:dependNames){
                    loadDecodePlugin(mapper.get(name).toString());//递归依赖
                }
                PluginBase base = (PluginBase) mainClass.newInstance();
                base.init(main.getPluginLoader(),main.getServer(),description,new File(main.getServer().getFilePath()+"/"+description.getName()),new File(fileName));
                base.onLoad();
                main.getLogger().info(base.getName()+"is starting! version "+description.getVersion());
                loaded.add(thisName);
                List<PluginCommand> commands = parseYamlCommands(base);
                allPlugins.add(base);
                if (!commands.isEmpty()) {
                    main.getServer().getCommandMap().registerAll(base.getDescription().getName(),commands);
                }
            }
            //这里找依赖
        }catch (InstantiationException e1){
            throw new PluginException("could not load plugin",e1);
        }catch (IllegalAccessException e2){
            throw new PluginException("could not load plugin",e2);
        }catch (ClassCastException e3){
            throw new PluginException("the plugin must extends the PluginBase",e3);
        }catch (Exception e){
            throw new PluginException("unknown exception",e);
        }
    }

    public PluginDescription getDescription(String fileName){
        PluginDescription pluginDescription = RunMain.getMain().getPluginLoader().getPluginDescription(fileName);
        descriptionMap.put(new File(fileName),pluginDescription);
        List<Permission> permissions = pluginDescription.getPermissions();
        //添加permession
        for(Permission permission:permissions){
            main.getServer().getPluginManager().addPermission(permission);
        }
        return pluginDescription;
    }


    public List<PluginBase> getAllPlugins() {
        return allPlugins;
    }
}

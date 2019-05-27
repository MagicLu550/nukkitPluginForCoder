package net.noyark.www.nukkit.decoder;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLoadOrder;

import java.util.List;

public class RunMain extends PluginBase implements Listener {

    private static RunMain main;

    private static DecodePluginManager decodePluginManager;

    @Override
    public void onLoad() {
        decodePluginManager = new DecodePluginManager(this.getServer(),this.getServer().getCommandMap());
        decodePluginManager.loadPlugins();//加载全部插件
    }

    @Override
    public void onEnable() {
        main = this;
        List<PluginBase> plugins = decodePluginManager.getAllPlugins();
        for(PluginBase plugin:plugins){
            if(plugin.getDescription().getOrder().equals(PluginLoadOrder.STARTUP)){
                plugin.setEnabled();
            }
        }
    }

    @EventHandler
    public void loadPluginAfterLoadedWorld(LevelLoadEvent e){
        List<PluginBase> plugins = decodePluginManager.getAllPlugins();
        for(PluginBase plugin:plugins){
            if(!plugin.isEnabled()&&plugin.getDescription().getOrder().equals(PluginLoadOrder.POSTWORLD)){
                plugin.onEnable();
            }
        }
    }

    @Override
    public void onDisable() {
        for(PluginBase pluginBase:decodePluginManager.getAllPlugins()){
            pluginBase.onDisable();
            pluginBase.setEnabled(false);
        }
    }

    public static RunMain getMain() {
        return main;
    }
}

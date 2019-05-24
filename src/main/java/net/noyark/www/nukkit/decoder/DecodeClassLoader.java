package net.noyark.www.nukkit.decoder;

import cn.nukkit.plugin.Plugin;


import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;


public class DecodeClassLoader extends URLClassLoader {

    public DecodeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Plugin loadPlugin(String fileName){
        try{
            URL url = new File(fileName).toURI().toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url},RunMain.class.getClassLoader());
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}

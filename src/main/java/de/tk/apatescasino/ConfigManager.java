package de.tk.apatescasino;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ConfigManager<T> {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    // Config stuff
    private File configFile;
    private T configObject;

    public ConfigManager(String configName, Plugin plugin) {

        configFile = new File(plugin.getDataFolder(), configName);

        // Copy config if there is none
        if (!configFile.exists() && plugin.getResource(configFile.getName()) != null)
            plugin.saveResource(configFile.getName(), false);
    }

    public void loadConfig(Type type) throws IOException {
        // Only load config if there is some kind of file
        if (configFile.exists()) {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
            configObject = gson.fromJson(inputStreamReader, type);
            inputStreamReader.close();
        }
    }

    public T getObject() {
        return configObject;
    }

    public void setObject(T value) {
        this.configObject = value;
    }

    /**
     * @return returns true on success
     */
    public boolean saveConfig() {
        final String json = gson.toJson(configObject);
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        try {
            if (Files.exists(configFile.toPath())) Files.delete(configFile.toPath());
            Files.write(configFile.toPath(), json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

package mattin.vs1;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(new GameManager(this), this);
        //CommandAPI.onLoad(new CommandAPIConfig());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

package de.photon.anticheataddition.modules;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.util.config.ConfigUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, onlyExplicitlyIncluded = true)
@ToString
public abstract class Module
{
    @Getter protected final String configString;
    @Getter @EqualsAndHashCode.Include private final String moduleId;
    @Getter private final String bypassPermission = (InternalPermission.BYPASS.getRealPermission() + '.') + this.getModuleId();
    @Getter(lazy = true) private final ModuleLoader moduleLoader = Preconditions.checkNotNull(createModuleLoader(), "Tried to create null ModuleLoader.");
    @Getter private boolean enabled;

    protected Module(String configString)
    {
        Preconditions.checkNotNull(configString, "Tried to create Module with null configString.");
        Preconditions.checkArgument(AntiCheatAddition.getInstance().getConfig().contains(configString), "Config path " + configString + " does not exist in the config. Please regenerate your config.");
        this.configString = configString;
        this.moduleId = "anticheataddition_" + configString.toLowerCase(Locale.ENGLISH);
    }

    public boolean loadBoolean(String substring, boolean def)
    {
        return AntiCheatAddition.getInstance().getConfig().getBoolean(configString + substring, def);
    }

    public int loadInt(String substring, int def)
    {
        return AntiCheatAddition.getInstance().getConfig().getInt(configString + substring, def);
    }

    public long loadLong(String substring, long def)
    {
        return AntiCheatAddition.getInstance().getConfig().getLong(configString + substring, def);
    }

    public double loadDouble(String substring, double def)
    {
        return AntiCheatAddition.getInstance().getConfig().getDouble(configString + substring, def);
    }

    public String loadString(String substring, String def)
    {
        return AntiCheatAddition.getInstance().getConfig().getString(configString + substring, def);
    }

    public List<String> loadStringList(String substring)
    {
        return ConfigUtils.loadImmutableStringOrStringList(configString + substring);
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled) {
            if (enabled) enableModule();
            else disableModule();
        }
    }

    public final void enableModule()
    {
        if (this.getModuleLoader().load()) {
            this.enabled = true;
            this.enable();
        }
    }

    public final void disableModule()
    {
        if (this.enabled) {
            this.enabled = false;
            this.getModuleLoader().unload();
            this.disable();
        }
    }

    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    protected void enable() {}

    protected void disable() {}
}
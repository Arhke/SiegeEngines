package com.github.alathra.siegeengines.command;

import com.github.alathra.siegeengines.SiegeEngines;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler {
    private final SiegeEngines instance;

    public CommandHandler(SiegeEngines instance) {
        this.instance = instance;
    }

    public void onLoad() {
    }

    public void onEnable() {
        // Register commands here
        instance.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            SiegeEnginesCommand.register(event.registrar())
        );
    }

    public void onDisable() {
    }
}

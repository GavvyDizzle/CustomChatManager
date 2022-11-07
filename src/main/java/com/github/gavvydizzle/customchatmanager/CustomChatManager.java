package com.github.gavvydizzle.customchatmanager;

import com.github.gavvydizzle.customchatmanager.commands.ReloadCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CustomChatManager extends JavaPlugin {

    private static CustomChatManager instance;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;

        chatManager = new ChatManager();
        getServer().getPluginManager().registerEvents(chatManager, this);

        Objects.requireNonNull(getCommand("chatmanageradmin")).setExecutor(new ReloadCommand());
    }

    @Override
    public void onDisable() {

    }

    public static CustomChatManager getInstance() {
        return instance;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }
}

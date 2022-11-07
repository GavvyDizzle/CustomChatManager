package com.github.gavvydizzle.customchatmanager.commands;

import com.github.gavvydizzle.customchatmanager.CustomChatManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements TabExecutor {

    ArrayList<String> arguments;

    public ReloadCommand() {
        arguments = new ArrayList<>();
        arguments.add("reload");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            CustomChatManager.getInstance().reloadConfig();
            CustomChatManager.getInstance().getChatManager().reload();
            sender.sendMessage(ChatColor.GREEN + "[CustomChatManager] Successfully reloaded");
        }
        catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "[CustomChatManager] Failed to reload the config");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> list = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], arguments, list);
        }

        return list;
    }
}

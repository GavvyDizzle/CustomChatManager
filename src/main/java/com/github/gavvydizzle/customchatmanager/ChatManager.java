package com.github.gavvydizzle.customchatmanager;

import com.github.mittenmc.gangsplugin.api.GangsAPI;
import com.github.mittenmc.serverutils.Colors;
import me.clip.placeholderapi.PlaceholderAPI;
import me.maximus1027.mittenrankups.MittenAPI.RankupsAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager implements Listener {

    private final Pattern pattern = Pattern.compile("(?<!\\\\)(&#[a-fA-F0-9]{6})");
    private final LuckPerms luckPerms;
    private final GangsAPI gangsAPI;
    private final RankupsAPI rankupsAPI;
    private Track wardTrack, prestigeTrack;
    private String defaultMessageFormat, prestigeMessageFormat;
    private String nonInGangString, inGangString;
    private boolean removeExtraWhitespace;

    public ChatManager() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
        else {
            luckPerms = null;
        }

        rankupsAPI = new RankupsAPI();
        gangsAPI = new GangsAPI();

        reload();
    }

    public void reload() {
        FileConfiguration config = CustomChatManager.getInstance().getConfig();
        config.options().copyDefaults(true);
        config.addDefault("removeExtraWhitespace", true);
        config.addDefault("trackNames.wards", "wards");
        config.addDefault("trackNames.prestiges", "prestiges");
        config.addDefault("format.default", "&8[<ward>&8]<gang> <rank> <name> &8»&7 <message>");
        config.addDefault("format.prestige", "&8[<prestige>&8-<ward>&8]<gang> <rank> <name> &8»&7 <message>");
        config.addDefault("gang.notInGang", "");
        config.addDefault("gang.inGang", " &8[&e%gangsplugin_gangName%&8]");
        CustomChatManager.getInstance().saveConfig();

        removeExtraWhitespace = config.getBoolean("removeExtraWhitespace");

        wardTrack = luckPerms.getTrackManager().getTrack(config.getString("trackNames.wards", "wards"));
        if (wardTrack == null) {
            CustomChatManager.getInstance().getLogger().warning("The track '" + config.getString("trackNames.wards") + "' could not be found. Wards will not work!");
        }

        prestigeTrack = luckPerms.getTrackManager().getTrack(config.getString("trackNames.prestiges", "prestiges"));
        if (prestigeTrack == null) {
            CustomChatManager.getInstance().getLogger().warning("The track '" + config.getString("trackNames.prestiges") + "' could not be found. Prestiges will not work!");
        }

        defaultMessageFormat = config.getString("format.default");
        prestigeMessageFormat = config.getString("format.prestige");
        nonInGangString = config.getString("gang.notInGang");
        inGangString = config.getString("gang.inGang");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onChat(AsyncPlayerChatEvent e) {
        String format;
        if (rankupsAPI.getPrestigePosition(e.getPlayer()) > 0) {
            format = prestigeMessageFormat;

            // <prestige> = Highest prestige
            if (prestigeTrack != null) {
                List<String> prestigeGroups = prestigeTrack.getGroups();
                int prestige = rankupsAPI.getPrestigePosition(e.getPlayer());

                if (prestige < 0 || prestige >= prestigeGroups.size()) {
                    format = format.replace("<prestige>", "");
                }
                else {
                    Group prestigeGroup = luckPerms.getGroupManager().getGroup(prestigeGroups.get(prestige));
                    if (prestigeGroup != null) {
                        if (prestigeGroup.getCachedData().getMetaData().getPrefix() != null) {
                            format = format.replace("<prestige>", prestigeGroup.getCachedData().getMetaData().getPrefix());
                        } else {
                            format = format.replace("<prestige>", prestigeGroup.getName());
                        }
                    }
                }
            }
        }
        else {
            format = defaultMessageFormat;
        }

        // <rank> = Primary LuckPerms group
        User user = luckPerms.getUserManager().getUser(e.getPlayer().getUniqueId());
        if (user != null) {
            Group primaryGroup = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (primaryGroup != null) {
                if (primaryGroup.getDisplayName() != null) {
                    format = format.replace("<rank>", primaryGroup.getDisplayName());
                } else {
                    format = format.replace("<rank>", primaryGroup.getName());
                }
            }
        }

        // <ward> = Highest ward
        if (wardTrack != null) {
            List<String> wardGroups = wardTrack.getGroups();
            int ward = rankupsAPI.getWardPosition(e.getPlayer()) - 1;

            if (ward < 0 || ward >= wardGroups.size()) {
                format = format.replace("<ward>", "");
            }
            else {
                Group wardGroup = luckPerms.getGroupManager().getGroup(wardGroups.get(ward));
                if (wardGroup != null) {
                    if (wardGroup.getCachedData().getMetaData().getPrefix() != null) {
                        format = format.replace("<ward>", wardGroup.getCachedData().getMetaData().getPrefix());
                    } else {
                        format = format.replace("<ward>", wardGroup.getName());
                    }
                }
            }
        }

        if (gangsAPI.isInGang(e.getPlayer().getUniqueId())) {
            format = format.replace("<gang>", inGangString);
        }
        else {
            format = format.replace("<gang>", nonInGangString);
        }

        format = format.replace("<name>", "%1$s");
        format = format.replace("<message>", "%2$s");
        format = PlaceholderAPI.setPlaceholders(e.getPlayer(), format);
        format = reformatHex(format);
        format = Colors.conv(format);
        if (removeExtraWhitespace) format = format.trim().replaceAll(" +", " ");
        e.setFormat(format);
    }

    // Reformats hex codes given in the &#xxxxxx format to <SOLID:xxxxxx>
    private String reformatHex(String message) {
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String code = matcher.group(1);
            if (code == null) code = matcher.group(2);

            message = message.replace(code, "<SOLID:" + code.substring(2) + ">");
        }

        return message;
    }

}

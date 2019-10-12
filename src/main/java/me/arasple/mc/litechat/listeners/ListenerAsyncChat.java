package me.arasple.mc.litechat.listeners;

import io.izzel.taboolib.module.inject.TListener;
import io.izzel.taboolib.module.locale.TLocale;
import io.izzel.taboolib.module.tellraw.TellrawJson;
import me.arasple.mc.litechat.LiteChat;
import me.arasple.mc.litechat.LiteChatFiles;
import me.arasple.mc.litechat.api.LiteChatAPI;
import me.arasple.mc.litechat.bstats.Metrics;
import me.arasple.mc.litechat.channels.StaffChat;
import me.arasple.mc.litechat.data.Cooldowns;
import me.arasple.mc.litechat.data.DataHandler;
import me.arasple.mc.litechat.filter.ChatFilter;
import me.arasple.mc.litechat.filter.process.FilteredObject;
import me.arasple.mc.litechat.formats.ChatFormats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * @author Arasple
 * @date 2019/8/16 11:04
 */
@TListener
public class ListenerAsyncChat implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        long start = System.currentTimeMillis();

        Player p = e.getPlayer();
        String message = e.getMessage();
        FilteredObject filteredObject = LiteChatAPI.filterString(p, message, ChatFilter.getEnable()[0]);
        e.setCancelled(true);

        // 世界是否启用
        if (LiteChatFiles.getSettings().getStringList("GENERAL.DISABLED-WORLDS").contains(p.getWorld().getName())) {
            e.setCancelled(false);
            return;
        }
        // 长度限制、敏感词数限制
        if (!processLimit(p, message) || !processFilter(p, filteredObject)) {
            return;
        }
        // 管理频道聊天
        if (StaffChat.isInStaffChannel(p)) {
            StaffChat.execute(p, message);
            return;
        }

        TellrawJson format = ChatFormats.getNormal(p, filteredObject.getFiltered());
        Bukkit.getOnlinePlayers().forEach(format::send);
        format.send(Bukkit.getConsoleSender());
        Metrics.increase(0);

        if (LiteChat.isDebug()) {
            LiteChat.getTLogger().info("[DEBUG] Process ChatEvent in " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private boolean processFilter(Player p, FilteredObject filteredObject) {
        if (p.hasPermission("litechat.bypass.filter")) {
            return true;
        }
        if (filteredObject.getSensitiveWords() >= ChatFilter.getBlockSending()) {
            TLocale.sendTo(p, "GENERAL.NO-SWEAR");
            return false;
        }
        return true;
    }

    private boolean processLimit(Player p, String message) {
        if (!p.hasPermission("litechat.bypass.*")) {
            long limit = LiteChatFiles.getSettings().getLong("CHAT-CONTROL.LENGTH-LIMIT", 100);
            if (message.length() > limit) {
                TLocale.sendTo(p, "GENERAL.TOO-LONG", message.length(), limit);
                return false;
            }
        }
        if (!p.hasPermission("litechat.bypass.itemcd")) {
            long itemShowCooldown = DataHandler.getCooldownLeft(p.getUniqueId(), Cooldowns.CooldownType.ITEM_SHOW);
            if (LiteChatFiles.getFuncitons().getStringList("ITEM-SHOW.KEYS").stream().anyMatch(message::contains)) {
                if (itemShowCooldown > 0) {
                    TLocale.sendTo(p, "COOLDOWNS.ITEM-SHOW", String.valueOf(itemShowCooldown / 1000D));
                    return false;
                } else {
                    DataHandler.updateCooldown(p.getUniqueId(), Cooldowns.CooldownType.ITEM_SHOW, (long) (LiteChatFiles.getFuncitons().getDouble("ITEM-SHOW.COOLDOWNS") * 1000));
                }
            }
        }
        if (!p.hasPermission("litechat.bypass.chatcd")) {
            long chatCooldown = DataHandler.getCooldownLeft(p.getUniqueId(), Cooldowns.CooldownType.CHAT);
            if (chatCooldown > 0) {
                TLocale.sendTo(p, "COOLDOWNS.CHAT", String.valueOf(chatCooldown / 1000D));
                return false;
            } else {
                DataHandler.updateCooldown(p.getUniqueId(), Cooldowns.CooldownType.CHAT, (long) (LiteChatFiles.getSettings().getDouble("CHAT-CONTROL.COOLDOWN") * 1000));
            }
        }
        return true;
    }

}

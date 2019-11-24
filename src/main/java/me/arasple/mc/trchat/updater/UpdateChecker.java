package me.arasple.mc.trchat.updater;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.taboolib.internal.apache.lang3.math.NumberUtils;
import io.izzel.taboolib.module.inject.TListener;
import io.izzel.taboolib.module.locale.TLocale;
import me.arasple.mc.trchat.TrChat;
import me.arasple.mc.trchat.TrChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author Arasple
 * @date 2019/8/15 22:35
 */
@TListener(register = "init")
public class UpdateChecker implements Listener {

    private static String url = "https://api.github.com/repos/Arasple/TrChat/releases/latest";
    private double version;
    private LatestInfo latest;

    public void init() {
        if (new File(TrChat.getPlugin().getDataFolder(), "do_not_update").exists()) {
            return;
        }
        version = NumberUtils.toDouble(TrChat.getPlugin().getDescription().getVersion().split("-")[0], -1);
        latest = new LatestInfo(false, -1, new String[]{});
        if (version == -1) {
            TLocale.sendToConsole("ERROR.VERSION");
            Bukkit.shutdown();
        }
        startTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (latest.hasLatest && !latest.noticed[1] && p.hasPermission("trchat.admin")) {
            latest.notifyUpdates(version, p);
            latest.noticed[1] = true;
        }
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 若已抓取到新版本信息或插件关闭了更新检测, 则取消
                if (latest.hasLatest) {
                    cancel();
                    return;
                }

                String read;
                try (InputStream inputStream = new URL(url).openStream(); BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
                    read = TrChatPlugin.readFully(bufferedInputStream, StandardCharsets.UTF_8);
                    JsonObject json = (JsonObject) new JsonParser().parse(read);
                    double latestVersion = json.get("tag_name").getAsDouble();

                    // 如果抓取到新版本
                    if (latestVersion > version) {
                        latest.hasLatest = true;
                        latest.newVersion = latestVersion;
                        latest.noticed[0] = true;
                        latest.updates = json.get("body").getAsString().replace("\r", "").split("\n");
                        latest.notifyUpdates(version, Bukkit.getConsoleSender());
                    } else if (!latest.noticed[0]) {
                        latest.noticed[0] = true;
                        TLocale.sendToConsole(version > latestVersion ? "PLUGIN.UPDATE-NOTIFY.DEV" : "PLUGIN.UPDATE-NOTIFY.LATEST");
                    }
                } catch (Exception ignored) {
                }
            }
        }.runTaskTimerAsynchronously(TrChat.getPlugin(), 20 * 5, 20 * 60 * 30);
    }

    public static void check() {
        double currentVersion = NumberUtils.toDouble(TrChat.getPlugin().getDescription().getVersion(), -1);
        double latestVersion = catchLatestVersion();

        if (latestVersion - currentVersion >= 0.03) {
            int last = (int) (5 * ((latestVersion - currentVersion) / 0.01));
            Bukkit.getConsoleSender().sendMessage("§8--------------------------------------------------");
            Bukkit.getConsoleSender().sendMessage("§r");
            Bukkit.getConsoleSender().sendMessage("§8# §4您所运行的 §cTrChat §4版本过旧, 可能潜在很多漏洞");
            Bukkit.getConsoleSender().sendMessage("§8# §4请及时更新到最新版本以便获得更好的插件体验!");
            Bukkit.getConsoleSender().sendMessage("§8# §r");
            Bukkit.getConsoleSender().sendMessage("§8# §4Mcbbs: §chttps://www.mcbbs.net/thread-903335-1-1.html");
            Bukkit.getConsoleSender().sendMessage("§8# §r");
            Bukkit.getConsoleSender().sendMessage("§8# §r");
            Bukkit.getConsoleSender().sendMessage("§8# §4服务器将在 §c§l" + last + " secs §4后继续启动...");
            Bukkit.getConsoleSender().sendMessage("§r");
            Bukkit.getConsoleSender().sendMessage("§8--------------------------------------------------");
            try {
                Thread.sleep(last * 1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static double catchLatestVersion() {
        try (InputStream inputStream = new URL(url).openStream(); BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            String read = TrChatPlugin.readFully(bufferedInputStream, StandardCharsets.UTF_8);
            JsonObject json = (JsonObject) new JsonParser().parse(read);
            return json.get("tag_name").getAsDouble();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static class LatestInfo {

        private boolean hasLatest;
        private double newVersion;
        private String[] updates;
        private boolean[] noticed;

        public LatestInfo(boolean hasLatest, double newVersion, String[] updates) {
            this.hasLatest = hasLatest;
            this.newVersion = newVersion;
            this.updates = updates;
            this.noticed = new boolean[]{false, false};
        }

        public void notifyUpdates(double version, CommandSender sender) {
            List<String> messages = Lists.newArrayList();
            messages.addAll(TLocale.asStringList("PLUGIN.UPDATE-NOTIFY.HEADER", String.valueOf(version), String.valueOf(newVersion)));
            messages.addAll(Arrays.asList(updates));
            messages.addAll(TLocale.asStringList("PLUGIN.UPDATE-NOTIFY.FOOTER"));
            messages.forEach(sender::sendMessage);
        }
    }

}

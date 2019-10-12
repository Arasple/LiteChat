package me.arasple.mc.litechat.utils;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * @author Arasple
 * @date 2019/8/15 20:52
 */
public class MessageColors {

    private static final List<Character> COLOR_CODES = Arrays.asList(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'
    );

    private static final String COLOR_PERMISSION_NODE = "litechat.color";

    public static List<String> replaceWithPermission(Player player, List<String> message) {
        for (int i = 0; i < message.size(); i++) {
            message.set(i, replaceWithPermission(player, message.get(i)));
        }
        return message;
    }

    public static String replaceWithPermission(Player player, String message) {
        for (Character code : COLOR_CODES) {
            if (player.hasPermission(COLOR_PERMISSION_NODE + "." + code)) {
                message = message.replace("&" + code, "§" + code);
            }
        }
        return message;
    }

}
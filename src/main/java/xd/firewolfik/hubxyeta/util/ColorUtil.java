package xd.firewolfik.hubxyeta.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class ColorUtil {
    private static ColorUtil instance;

    private ColorUtil() {
    }

    public static ColorUtil getInstance() {
        if (instance == null) {
            instance = new ColorUtil();
        }
        return instance;
    }

    public String translateColor(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        Pattern hexPattern = Pattern.compile("&(#\\w{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of(hexColor).toString());
        }
        matcher.appendTail(buffer);
        message = buffer.toString();
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
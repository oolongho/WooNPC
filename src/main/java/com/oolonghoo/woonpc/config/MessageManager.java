package com.oolonghoo.woonpc.config;

import com.oolonghoo.woonpc.WooNPC;
import com.oolonghoo.woonpc.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息管理器
 * 负责加载和管理语言文件
 * 
 * @author oolongho
 */
public class MessageManager {

    private final WooNPC plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix;

    public MessageManager(WooNPC plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载语言文件
     */
    public void load() {
        String language = plugin.getConfigLoader().getLanguage();
        File langFile = new File(plugin.getDataFolder(), "lang" + File.separator + language + ".yml");
        
        // 如果语言文件不存在，从资源中复制
        if (!langFile.exists()) {
            plugin.saveResource("lang" + File.separator + language + ".yml", false);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 加载默认语言文件作为备用
        InputStream defaultStream = plugin.getResource("lang" + File.separator + language + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);
        }

        // 清空旧消息
        messages.clear();

        // 加载所有消息
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                messages.put(key, langConfig.getString(key));
            }
        }

        // 加载前缀
        this.prefix = colorize(messages.getOrDefault("prefix", "&a[WooNPC] &r"));
    }

    /**
     * 获取消息
     * 
     * @param key 消息键
     * @return 消息内容
     */
    public String get(String key) {
        return colorize(messages.getOrDefault(key, key));
    }

    /**
     * 获取消息并替换占位符
     * 
     * @param key 消息键
     * @param placeholders 占位符键值对
     * @return 替换后的消息
     */
    public String get(String key, String... placeholders) {
        String message = get(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }

    /**
     * 获取带前缀的消息
     * 
     * @param key 消息键
     * @return 带前缀的消息
     */
    public String getWithPrefix(String key) {
        return prefix + get(key);
    }

    /**
     * 获取带前缀的消息并替换占位符
     * 
     * @param key 消息键
     * @param placeholders 占位符键值对
     * @return 替换后的带前缀消息
     */
    public String getWithPrefix(String key, String... placeholders) {
        return prefix + get(key, placeholders);
    }

    /**
     * 获取前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 颜色化字符串
     * 支持 §、& 和 MiniMessage 格式
     * 
     * @param text 原始文本
     * @return 颜色化后的文本
     */
    public static String colorize(String text) {
        return ColorUtil.translate(text);
    }
}

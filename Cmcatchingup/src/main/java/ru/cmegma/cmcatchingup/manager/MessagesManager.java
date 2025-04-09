package ru.cmegma.cmcatchingup.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.cmegma.cmcatchingup.Cmcatchingup;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class MessagesManager {

    private final Cmcatchingup plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final LegacyComponentSerializer legacySerializer;
    private String prefix = "";

    public MessagesManager(Cmcatchingup plugin) {
        this.plugin = plugin;
        this.legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexCharacter('#')
                .hexColors()
                .build();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
            prefix = messagesConfig.getString("prefix", "&7[CmCatchingUp] ");
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load messages.yml", e);
        }
    }

    public String getRawMessage(String key, String defaultValue) {
        return messagesConfig.getString(key, defaultValue);
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String messageFormat = messagesConfig.getString(key);
        if (messageFormat == null) {
            plugin.getLogger().warning("Missing message key in messages.yml: " + key);
            return legacySerializer.deserialize("&cMissing message: " + key);
        }

        String replacedMessage = messageFormat;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                replacedMessage = replacedMessage.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        String fullMessage = prefix + replacedMessage;
        return legacySerializer.deserialize(fullMessage);
    }

    public Component getSimpleMessage(String key) {
        return getMessage(key, null);
    }

    public Component getRawComponentMessage(String key, Map<String, String> placeholders) {
        String messageFormat = messagesConfig.getString(key);
        if (messageFormat == null) {
            plugin.getLogger().warning("Missing message key in messages.yml: " + key);
            return legacySerializer.deserialize("&cMissing message: " + key);
        }

        String replacedMessage = messageFormat;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                replacedMessage = replacedMessage.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return legacySerializer.deserialize(replacedMessage);
    }
}
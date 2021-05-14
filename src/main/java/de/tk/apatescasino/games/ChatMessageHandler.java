package de.tk.apatescasino.games;

import de.tk.apatescasino.ApatesCasino;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.WeakHashMap;
import java.util.function.Consumer;

public class ChatMessageHandler {
    private final WeakHashMap<Player, Consumer<String>> expectedMessages = new WeakHashMap<>();

    public boolean HandleChat(Player player, String message) {
        Consumer<String> consumer = expectedMessages.remove(player);
        if (consumer != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    consumer.accept(message);
                }
            }.runTask(ApatesCasino.getInstance());
            return true;
        }
        return false;
    }

    public void AddExpectedMessage(Player player, Consumer<String> consumer) {
        expectedMessages.put(player, consumer);
    }
}

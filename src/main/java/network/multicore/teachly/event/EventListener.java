package network.multicore.teachly.event;

import com.google.common.base.Preconditions;
import network.multicore.teachly.js.Script;
import network.multicore.teachly.js.exceptions.JSException;
import network.multicore.teachly.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EventListener<E extends Event> implements Listener {
    private final Logger logger = Logger.getLogger();
    private final List<ListenerCallback> callbacks = new ArrayList<>();
    private final EventPriority priority;

    public EventListener(@NotNull Plugin plugin, @NotNull Class<E> eventClass, @NotNull EventPriority priority) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(eventClass, "eventClass");
        Preconditions.checkNotNull(priority, "priority");

        this.priority = priority;

        Bukkit.getPluginManager().registerEvent(eventClass, this, priority, (listener, event) -> call(event), plugin);
    }

    public void registerCallback(@NotNull ListenerCallback callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }

    public void unregisterCallback(@NotNull Script script) {
        synchronized (callbacks) {
            callbacks.removeIf(callback -> callback.script.equals(script));
        }
    }

    public void unregisterAllCallbacks() {
        synchronized (callbacks) {
            callbacks.clear();
        }
    }

    public boolean isEmpty() {
        return callbacks.isEmpty();
    }

    public int size() {
        return callbacks.size();
    }

    public EventPriority getPriority() {
        return priority;
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    private void call(@NotNull Event event) {
        synchronized (callbacks) {
            callbacks.forEach(callback -> {
                try {
                    callback.script.call(callback.callback, event);
                } catch (JSException e) {
                    logger.warn("Error calling callback {} for event {} in script {}: {}", callback.callback, event.getClass().getSimpleName(), callback.script.getId(), e.getMessage());
                }
            });
        }
    }

    public record ListenerCallback(@NotNull Script script, @NotNull String callback) {
    }
}
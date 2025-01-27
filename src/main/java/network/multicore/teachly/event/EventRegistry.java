/*
 * BSD 3-Clause License
 * Copyright (c) 2023 - 2024, Lorenzo Magni & Kevin Delugan.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package network.multicore.teachly.event;

import com.google.common.base.Preconditions;
import network.multicore.teachly.Teachly;
import network.multicore.teachly.js.Script;
import network.multicore.teachly.utils.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.util.*;

public class EventRegistry implements Listener {
    private final Teachly plugin;
    private final Logger logger = Logger.getLogger();
    private final Map<String, Class<? extends Event>> events = new HashMap<>();
    private final Map<EventGroup, EventListener> listeners = new HashMap<>();

    public EventRegistry(@NotNull Teachly plugin) {
        this.plugin = plugin;
    }

    public boolean fetchEvents() {
        try {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("org.bukkit.event", "io.papermc.paper.event", "network.multicore.teachly.event")
                    .addScanners(Scanners.SubTypes));

            reflections.getSubTypesOf(Event.class).forEach(c -> {
                if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) return;
                events.put(c.getSimpleName(), c);
            });
        } catch (Throwable t) {
            return false;
        }

        return true;
    }

    public int size() {
        return events.size();
    }

    public Optional<Class<? extends Event>> getEventClass(String name) {
        if (name == null) return Optional.empty();
        return Optional.of(events.get(name));
    }

    @NotNull
    public EventPriority getEventPriority(String name) {
        if (name == null) return EventPriority.NORMAL;

        try {
            return EventPriority.valueOf(name.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ignored) {
            return EventPriority.NORMAL;
        }
    }

    public void close() {
        synchronized (listeners) {
            listeners.forEach((event, listener) -> {
                listener.unregisterAllCallbacks();
                listener.unregister();
            });
            listeners.clear();
        }

        events.clear();
    }

    public void registerListener(@NotNull Script script, @NotNull String callback, @NotNull Class<? extends Event> event, @NotNull EventPriority priority) {
        Preconditions.checkNotNull(script);
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(priority);

        synchronized (listeners) {
            EventGroup group = new EventGroup(event, priority);
            EventListener listener = listeners.getOrDefault(group, new EventListener(plugin, event, priority));
            listener.registerCallback(new EventListener.ListenerCallback(script, callback));
            listeners.put(group, listener);

            logger.info("<green>Script <yellow>{}</yellow> registered listener <yellow>{}</yellow> for event <yellow>{}</yellow> with priority <yellow>{}</yellow>", script.getId(), callback, event.getSimpleName(), priority);
        }
    }

    public void unregisterListeners(@NotNull Script script) {
        Preconditions.checkNotNull(script);

        synchronized (listeners) {
            List<EventGroup> toBeRemoved = new ArrayList<>();

            listeners.forEach((group, listener) -> {
                listener.unregisterCallback(script);
                if (listener.isEmpty()) {
                    listener.unregister();
                    toBeRemoved.add(group);
                }
            });

            toBeRemoved.forEach(listeners::remove);
        }

        logger.info("<dark_green>Script <yellow>{}</yellow> unregistered all listeners", script.getId());
    }

    private static final class EventGroup {
        @NotNull
        private final Class<? extends Event> event;
        @NotNull
        private final EventPriority priority;

        private EventGroup(@NotNull Class<? extends Event> event, @NotNull EventPriority priority) {
            this.event = event;
            this.priority = priority;
        }

        @NotNull
        public Class<? extends Event> event() {
            return event;
        }

        @NotNull
        public EventPriority priority() {
            return priority;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (EventGroup) obj;
            return Objects.equals(this.event, that.event) &&
                    Objects.equals(this.priority, that.priority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(event, priority);
        }

        @Override
        public String toString() {
            return "EventGroup[" +
                    "event=" + event + ", " +
                    "priority=" + priority + ']';
        }


    }
}

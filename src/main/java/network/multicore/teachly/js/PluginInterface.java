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

package network.multicore.teachly.js;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import network.multicore.mbcore.Text;
import network.multicore.teachly.Teachly;
import network.multicore.teachly.event.EventRegistry;
import network.multicore.teachly.js.data.Exercise;
import network.multicore.teachly.utils.Logger;
import network.multicore.teachly.js.data.Result;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PluginInterface {
    private static final Gson GSON = new Gson();
    private final Teachly plugin;
    private final Logger logger = Logger.getLogger();
    private final EventRegistry eventRegistry;
    private final Script script;

    public PluginInterface(@NotNull Teachly plugin, @NotNull Script script) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(script, "Script cannot be null");

        this.plugin = plugin;
        this.eventRegistry = plugin.eventRegistry();
        this.script = script;
    }

    /**
     * Log a message with the INFO level.
     *
     * @param msg The message to log.
     */
    public void info(Object msg) {
        if (msg == null) return;
        logger.info(msg.toString());
    }

    /**
     * Log a message with the WARN level.
     *
     * @param msg The message to log.
     */
    public void warn(Object msg) {
        if (msg == null) return;
        logger.warn(msg.toString());
    }

    /**
     * Log a message with the ERROR level.
     *
     * @param msg The message to log.
     */
    public void error(Object msg) {
        if (msg == null) return;
        logger.error(msg.toString());
    }

    /**
     * Execute an exercise.
     *
     * @param player   The player that will execute the exercise.
     * @param exercise The exercise to execute.
     * @return A {@link Result} indicating the success of the operation.
     */
    public Result<Void> exercise(Object player, String exercise) {
        Player p;
        switch (player) {
            case Player player1 -> p = player1;
            case UUID uuid -> p = Bukkit.getPlayer(uuid);
            case String s -> p = Bukkit.getPlayer(s);
            case null, default -> {
                return Result.failure("player is not a player, UUID or String");
            }
        }

        if (p == null) return Result.failure("player not found");

        Exercise e;

        try {
            e = GSON.fromJson(exercise, Exercise.class);
        } catch (Throwable t) {
            logger.warn("An error occurred while parsing exercise: {}", t.getMessage());
            return Result.failure("error parsing exercise");
        }

        e.execute(plugin, p);
        return Result.success();
    }

    /**
     * Send a message to a player.
     *
     * @param msg The message to send.
     * @param dst The {@link Player}, {@link UUID} or {@link String} to send the message to.
     * @return A {@link Result} indicating the success of the operation.
     */
    public Result<Void> sendMessage(String msg, Object dst) {
        if (msg == null) {
            logger.warn("Script {} tried to send a null message to {}", script.getId(), dst);
            return Result.failure("message is null");
        }

        switch (dst) {
            case null -> {
                logger.warn("Script {} tried to send a message to a null receiver", script.getId());
                return Result.failure("destination is null");
            }
            case Player player -> {
                Text.send(msg, player);
                return Result.success();
            }
            case UUID uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) return Result.failure("player not found");

                Text.send(msg, player);
                return Result.success();
            }
            case String dstStr -> {
                if (dstStr.equalsIgnoreCase(Bukkit.getConsoleSender().getName())) {
                    logger.info(msg);
                    return Result.success();
                }

                Player player = Bukkit.getPlayer(dstStr);
                if (player == null) return Result.failure("player not found");

                Text.send(msg, player);
                return Result.success();
            }
            default -> {
                return Result.failure("destination is not a player or UUID");
            }
        }
    }

    /**
     * Broadcast a message to all players.
     *
     * @param msg The message to broadcast.
     * @return A {@link Result} indicating the success of the operation.
     */
    public Result<Void> broadcast(String msg) {
        if (msg == null) {
            logger.warn("Script {} tried to broadcast a null message", script.getId());
            return Result.failure("message is null");
        }

        Text.broadcast(msg);
        return Result.success();
    }

    /**
     * Register a new event listener.
     *
     * @param event    The name of the event class to listen to.
     * @param callback The name of the callback function that will be called when the event is triggered.
     * @param priority The priority of the event listener. Valid values are found in {@link org.bukkit.event.EventPriority}
     * @return A {@link Result} indicating the success of the operation.
     */
    public Result<Void> subscribe(String event, String callback, String priority) {
        if (event == null) {
            logger.warn("<yellow>An error occurred while subscribing script {} to an event. Event cannot be null.", script.getId());
            return Result.failure("event is null");
        }

        if (callback == null) {
            logger.warn("<yellow>An error occurred while subscribing script {} to event {}. Callback cannot be null.", script.getId(), event);
            return Result.failure("callback is null");
        }

        Optional<Class<? extends Event>> eventClass = eventRegistry.getEventClass(event);
        if (eventClass.isEmpty()) {
            logger.warn("<yellow>An error occurred while subscribing script {} to event {}. This event does not exist.", script.getId(), event);
            return Result.failure("event does not exist");
        }

        eventRegistry.registerListener(
                script,
                callback,
                eventClass.get(),
                eventRegistry.getEventPriority(priority)
        );

        return Result.success();
    }

    /**
     * Register a new event listener with NORMAL priority.
     *
     * @param event    The name of the event class to listen to.
     * @param callback The name of the callback function that will be called when the event is triggered.
     * @return A {@link Result} indicating the success of the operation.
     */
    public Result<Void> subscribe(String event, String callback) {
        return subscribe(event, callback, null);
    }
}

/*
 * BSD 3-Clause License
 * Copyright (c) 2024, Lorenzo Magni & Kevin Delugan.
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

package network.multicore.teachly.utils;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.logging.Level;

public class Logger {
    private static final ANSIComponentSerializer ANSI = ANSIComponentSerializer.builder().build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
    private final java.util.logging.Logger logger;

    private static Logger instance;

    private Logger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public static Logger createLogger(java.util.logging.Logger logger) {
        if (instance == null) instance = new Logger(logger);
        return instance;
    }

    public static Logger getLogger() {
        if (instance == null) throw new IllegalStateException("Logger has not been created yet");
        return instance;
    }

    public void log(@NotNull Level level, String message, Object... args) {
        Preconditions.checkNotNull(level, "level");
        if (message == null) return;

        logger.log(level, ANSI.serialize(MINI_MESSAGE.deserialize(format(message, args))));
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARNING, "<yellow>" + message, args);
    }

    public void error(String message, Object... args) {
        log(Level.SEVERE, "<red>" + message, args);
    }

    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) return message;
        if (!message.contains("{}")) return message;

        try {
            return message.replace("{}", "%S").formatted(Arrays.stream(args).map(arg -> arg == null ? "null" : arg.toString()).toArray());
        } catch (Throwable t) {
            throw new IllegalArgumentException("Missing arguments to format the string", t);
        }
    }
}

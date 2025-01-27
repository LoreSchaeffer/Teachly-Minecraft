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
import network.multicore.teachly.Teachly;
import network.multicore.teachly.js.exceptions.JSException;
import network.multicore.teachly.js.exceptions.SyntaxException;
import org.graalvm.polyglot.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class JavaScript implements Closeable {
    private final Engine engine;
    private final Context ctx;

    public JavaScript(@NotNull PluginInterface pluginInterface) {
        Preconditions.checkNotNull(pluginInterface, "pluginInterface");

        engine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        ctx = Context.newBuilder("js")
                .engine(engine)
                .logHandler(OutputStream.nullOutputStream())
                .allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(c -> c.startsWith("org.bukkit.") || c.startsWith("io.papermc.") || c.startsWith("network.multicore."))
                .build();

        ctx.getBindings("js").putMember("$", pluginInterface);
    }

    public void evaluate(@NotNull File script) throws JSException {
        Preconditions.checkNotNull(script, "script");
        if (!script.exists() || !script.isFile()) throw new JSException("Script not found");

        try {
            Source source = Source.newBuilder("js", script).build();

            Value value;
            try {
                value = ctx.parse(source);
            } catch (PolyglotException e) {
                if (e.isSyntaxError()) throw new SyntaxException(e.getSourceLocation());
                else throw new SyntaxException("SyntaxException in file " + script.getName());
            }

            value.execute();
        } catch (Throwable t) {
            if (t instanceof JSException) throw (JSException) t;
            else throw new JSException(t);
        }
    }

    public <T> T call(@NotNull String function, @NotNull Class<T> returnType, Object... args) throws JSException {
        Preconditions.checkNotNull(function, "function");
        Preconditions.checkNotNull(returnType, "returnType");
        Preconditions.checkArgument(!function.trim().isEmpty(), "Function cannot be empty");

        try {
            Value value = ctx.getBindings("js").getMember(function).execute(args);
            return value.as(returnType);
        } catch (Throwable t) {
            throw new JSException(t);
        }
    }

    public void call(@NotNull String function, Object... args) throws JSException {
        call(function, Void.class, args);
    }

    @Override
    public void close() throws IOException {
        try {
            if (ctx != null) ctx.close();
            if (engine != null) engine.close();
        } catch (Throwable t) {
            throw new IOException(t);
        }
    }
}

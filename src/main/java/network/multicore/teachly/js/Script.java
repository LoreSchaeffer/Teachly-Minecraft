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
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class Script implements Closeable {
    private static final String ALLOWED_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_";
    private final String pkg;
    private final String id;
    private final File file;
    private final Teachly plugin;
    private final PluginInterface pluginInterface;
    private final JavaScript js;

    public Script(@NotNull File file, @NotNull File scriptsDir, @NotNull Teachly plugin) {
        Preconditions.checkNotNull(file, "file");
        Preconditions.checkNotNull(scriptsDir, "scriptsDir");
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkArgument(file.exists() && file.isFile(), "File must exist and be a file");
        Preconditions.checkArgument(file.getName().endsWith(".js"), "File must be a .js file");

        this.file = file;
        this.pkg = getPackage(file, scriptsDir);
        this.id = getId(file);
        this.plugin = plugin;
        this.pluginInterface = new PluginInterface(plugin, this);
        this.js = new JavaScript(pluginInterface);
    }

    public String getPackage() {
        return pkg;
    }

    public String getId() {
        return pkg.isEmpty() ? id : pkg + "." + id;
    }

    public File getFile() {
        return file;
    }

    public void evaluate() throws JSException {
        js.evaluate(file);
    }

    public <T> T call(@NotNull String function, @NotNull Class<T> returnType, Object... args) throws JSException {
        return js.call(function, returnType, args);
    }

    public void call(@NotNull String function, Object... args) throws JSException {
        call(function, Void.class, args);
    }

    @Override
    public void close() throws IOException {
        plugin.eventRegistry().unregisterListeners(this);
        if (js != null) js.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Script script = (Script) o;

        if (!pkg.equals(script.pkg)) return false;
        return id.equals(script.id);
    }

    @Override
    public int hashCode() {
        int result = pkg.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @NotNull
    private static String getPackage(@NotNull File file, @NotNull File scriptsDir) throws IllegalArgumentException {
        String parentPath = scriptsDir.getPath().replace("\\", "/");
        String filePath = file.getParentFile().getPath().replace("\\", "/");

        if (!filePath.startsWith(parentPath)) throw new IllegalArgumentException("File must be inside the scripts root directory");

        String relativePath = filePath.substring(parentPath.length());
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);

        return relativePath.replace("/", ".");
    }

    @NotNull
    private static String getId(@NotNull File file) throws IllegalArgumentException {
        String id = file.getName()
                .toLowerCase(Locale.US)
                .replace(".js", "")
                .replace(" ", "_")
                .replaceAll("[^" + ALLOWED_ID_CHARS + "]", "");

        if (id.isBlank()) throw new IllegalArgumentException("File name must contain at least one alphanumeric character");
        return id;
    }
}

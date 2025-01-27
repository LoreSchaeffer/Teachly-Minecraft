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

package network.multicore.teachly;

import com.google.common.base.Preconditions;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import network.multicore.teachly.event.EventRegistry;
import network.multicore.teachly.js.Script;
import network.multicore.teachly.js.exceptions.JSException;
import network.multicore.teachly.utils.Logger;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Teachly extends JavaPlugin {
    private final Logger logger = Logger.createLogger(getLogger());
    private final File scriptsDir = new File(getDataFolder(), "scripts");
    private final File configFile = new File(getDataFolder(), "config.yml");
    private YamlDocument config;
    private final List<Script> scripts = new ArrayList<>();
    private EventRegistry<? extends Event> eventRegistry;
    private static Teachly instance;

    public Teachly() {
        instance = this;
    }

    @Override
    public void onLoad() {
        try {
            initStorage();
        } catch (IOException e) {
            logger.error("Could not initialize storage. {}", e.getMessage());
            onDisable();
            return;
        }

        eventRegistry = new EventRegistry<>(this);
        if (!eventRegistry.fetchEvents()) {
            onDisable();
            return;
        }

        logger.info("Loaded {} events", eventRegistry.size());

        try {
            loadConfig();
        } catch (IOException e) {
            logger.error("Could not load config. {}", e.getMessage());
            onDisable();
        }
    }

    @Override
    public void onEnable() {
        loadScripts(scriptsDir, scripts);
        logger.info("<dark_green>Loaded <green>{} <dark_green>scripts", scripts.size());

        // TODO Add ability to make some scripts run at startup saving something in the config file
        evaluateScripts();

        logger.info("<green>{} enabled!", getName());
    }

    @Override
    public void onDisable() {
        scripts.forEach(s -> {
            try {
                s.close();
            } catch (IOException e) {
                logger.warn("An error occurred while closing script {}: {}", s.getId(), e.getMessage());
            }
        });
        scripts.clear();

        if (eventRegistry != null) eventRegistry.close();

        System.gc();
        logger.info("<red>{} disabled!", getName());
    }

    public static Teachly getInstance() {
        if (instance == null) throw new IllegalStateException("Teachly is not initialized");
        return instance;
    }

    public YamlDocument config() {
        return config;
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    public EventRegistry<? extends Event> eventRegistry() {
        return eventRegistry;
    }

    public List<Script> getScripts() {
        return scripts;
    }

    public void evaluateScripts() {
        scripts.forEach(this::evaluateScript);
    }

    public void evaluateScript(Script script) {
        try {
            script.evaluate();
        } catch (JSException e) {
            logger.warn(e.getMessage());
        }
    }

    public void closeScripts() {
        scripts.forEach(this::closeScript);
    }

    public void closeScript(Script script) {
        try {
            script.close();
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private void initStorage() throws IOException {
        if (!getDataFolder().exists() || !getDataFolder().isDirectory()) {
            if (!getDataFolder().mkdir()) {
                throw new IOException("Could not create data folder");
            }
        }

        if (!scriptsDir.exists() || !scriptsDir.isDirectory()) {
            if (!scriptsDir.mkdir()) {
                throw new IOException("Could not create scripts folder");
            }
        }
    }

    private void loadConfig() throws IOException {
        try (InputStream is = getResource("config.yml")) {
            config = YamlDocument.create(
                    configFile,
                    Objects.requireNonNull(is),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder()
                            .setAutoUpdate(true)
                            .setCreateFileIfAbsent(true)
                            .build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                            .build()
            );

            if (config.update()) config.save();
        }
    }

    private void loadScripts(File dir, List<Script> scripts) {
        try {
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isDirectory()) {
                    loadScripts(file, scripts);
                } else if (file.getName().toLowerCase(Locale.US).endsWith(".js")) {
                    try {
                        Script script = new Script(file, scriptsDir, this);
                        if (getScript(script.getId()) != null) throw new Exception("Script with id " + script.getId() + " already exists");

                        scripts.add(script);
                    } catch (Exception e) {
                        logger.warn("<red>Cannot load file. {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

    @Nullable
    private Script getScript(@NotNull String id) {
        Preconditions.checkNotNull(id, "id");

        for (Script script : scripts) {
            if (script.getId().equals(id)) return script;
        }

        return null;
    }
}

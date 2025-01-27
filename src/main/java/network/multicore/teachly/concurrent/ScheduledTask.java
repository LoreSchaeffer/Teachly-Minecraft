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

package network.multicore.teachly.concurrent;

import network.multicore.mclib.Watchdog;
import network.multicore.teachly.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

public class ScheduledTask {
    private final Logger logger = Logger.getLogger();
    private final UUID id;
    private final boolean kill;
    private final TaskPriority priority;
    private final ScheduledFuture<?> future;

    ScheduledTask(@NotNull UUID id, boolean kill, TaskPriority priority, @NotNull ScheduledFuture<?> future) {
        this.id = id;
        this.kill = kill;
        this.priority = priority == null ? TaskPriority.NORMAL : priority;
        this.future = future;
    }

    public UUID getId() {
        return id;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    //TODO Test termination of tasks
    public void cancel() {
        try {
            if (!future.isCancelled() && !future.isDone()) {
                if (kill) {
                    Watchdog watchdog = new Watchdog(() -> {
                        logger.warn("<yellow>Task {} did not terminate within 30 seconds", id);
                    }, 30000);
                    watchdog.setEnabled(true);

                    future.cancel(true);

                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.warn("<yellow>An error occurred while canceling task {}: {}", id, e.getMessage());
                    } finally {
                        watchdog.setEnabled(false);
                    }
                } else {
                    future.cancel(false);
                }
            }
        } catch (Throwable t) {
            logger.warn("<yellow>An error occurred while canceling task {}: {}", id, t.getMessage());
        }
    }
}

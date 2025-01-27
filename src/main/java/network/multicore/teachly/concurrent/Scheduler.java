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

import com.google.common.base.Preconditions;
import network.multicore.teachly.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private final Logger logger = Logger.getLogger();
    private final ScheduledExecutorService executorService;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();

    public Scheduler(int poolSize) {
        Preconditions.checkArgument(poolSize > 0, "Pool size must be greater than 0");

        this.executorService = Executors.newScheduledThreadPool(poolSize);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit, boolean kill, TaskPriority priority) {
        Preconditions.checkNotNull(runnable, "runnable");
        Preconditions.checkNotNull(unit, "unit");

        UUID id = generateTaskUUID();

        ScheduledTask task = new ScheduledTask(id, kill, priority, executorService.scheduleAtFixedRate(() -> {
            runnable.run();
            tasks.remove(id);
        }, delay, period, unit));

        tasks.put(id, task);
        return task;
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit, boolean kill) {
        return scheduleTaskAtFixedRate(runnable, delay, period, unit, kill, null);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit, TaskPriority priority) {
        return scheduleTaskAtFixedRate(runnable, delay, period, unit, true, priority);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit) {
        return scheduleTaskAtFixedRate(runnable, delay, period, unit, true, null);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long period, @NotNull TimeUnit unit, boolean kill, TaskPriority priority) {
        return scheduleTaskAtFixedRate(runnable, 0, period, unit, kill, priority);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long period, @NotNull TimeUnit unit, boolean kill) {
        return scheduleTaskAtFixedRate(runnable, 0, period, unit, kill, null);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long period, @NotNull TimeUnit unit, TaskPriority priority) {
        return scheduleTaskAtFixedRate(runnable, 0, period, unit, true, priority);
    }

    public ScheduledTask scheduleTaskAtFixedRate(@NotNull Runnable runnable, long period, @NotNull TimeUnit unit) {
        return scheduleTaskAtFixedRate(runnable, 0, period, unit, true, null);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit, boolean kill, TaskPriority priority) {
        Preconditions.checkNotNull(runnable, "runnable");
        Preconditions.checkNotNull(unit, "unit");

        UUID id = generateTaskUUID();

        ScheduledTask task = new ScheduledTask(id, kill, priority, executorService.schedule(() -> {
            runnable.run();
            tasks.remove(id);
        }, delay, unit));

        tasks.put(id, task);
        return task;
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit, boolean kill) {
        return scheduleTask(runnable, delay, unit, kill, null);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit, TaskPriority priority) {
        return scheduleTask(runnable, delay, unit, true, priority);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, boolean kill, TaskPriority priority) {
        return scheduleTask(runnable, 0, TimeUnit.MILLISECONDS, kill, priority);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, boolean kill) {
        return scheduleTask(runnable, 0, TimeUnit.MILLISECONDS, kill, null);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable, TaskPriority priority) {
        return scheduleTask(runnable, 0, TimeUnit.MILLISECONDS, true, priority);
    }

    public ScheduledTask scheduleTask(@NotNull Runnable runnable) {
        return scheduleTask(runnable, 0, TimeUnit.MILLISECONDS, true, null);
    }

    public void shutdown() {
        List<ScheduledTask> tasks = new ArrayList<>(this.tasks.values());
        tasks.sort((t1, t2) -> Integer.compare(t2.getPriority().getValue(), t1.getPriority().getValue()));

        for (ScheduledTask task : tasks) {
            task.cancel();
        }

        tasks.clear();
        this.tasks.clear();

        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) throw new InterruptedException("Scheduler shutdown timeout");
        } catch (InterruptedException e) {
            logger.error("<red>An error occurred while shutting down the scheduler: {}", e.getMessage());
        }

        executorService.shutdown();
    }

    public void shutdownNow() {
        executorService.shutdownNow();
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    private UUID generateTaskUUID() {
        UUID uuid;

        do {
            uuid = UUID.randomUUID();
        } while (tasks.containsKey(uuid));

        return uuid;
    }
}

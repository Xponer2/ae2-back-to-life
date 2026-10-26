/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.hooks.ticking;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.crafting.CraftingCalculation;
import appeng.me.Grid;
import appeng.me.GridNode;
import appeng.util.ILevelRunnable;
import appeng.util.Platform;

public class TickHandler {

    /**
     * Time limit for process queues with respect to the 50ms of a minecraft tick.
     */
    private static final int TIME_LIMIT_PROCESS_QUEUE_MILLISECONDS = 25;

    private static final TickHandler INSTANCE = new TickHandler();
    private final Queue<ILevelRunnable> serverQueue = new ArrayDeque<>();
    private final Multimap<World, CraftingCalculation> craftingJobs = LinkedListMultimap.create();
    private final Map<World, Queue<ILevelRunnable>> callQueue = new HashMap<>();
    private final ServerBlockEntityRepo blockEntities = new ServerBlockEntityRepo();
    private final ServerGridRepo grids = new ServerGridRepo();

    /**
     * A stop watch to limit processing the additional queues to honor
     * {@link TickHandler#TIME_LIMIT_PROCESS_QUEUE_MILLISECONDS}.
     * <p>
     * This cumulative for all queues of one server tick.
     */
    private final Stopwatch stopWatch = Stopwatch.createUnstarted();
    private int processQueueElementsProcessed = 0;
    private int processQueueElementsRemaining = 0;

    private long tickCounter;

    public static TickHandler instance() {
        return INSTANCE;
    }

    private TickHandler() {
    }

    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            onServerTickStart();
        } else {
            onServerTickEnd();
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side.isServer() && event.world instanceof WorldServer level) {
            if (event.phase == TickEvent.Phase.START) {
                onServerLevelTickStart(level);
            } else {
                onServerLevelTickEnd(level);
            }
        }
    }

    public void addCallable(World level, Runnable c) {
        addCallable(level, ignored -> c.run());
    }

    /**
     * Add a server or level callback which gets called the next time the queue is ticked.
     * <p>
     * Callbacks on the client are not support.
     * <p>
     * Using null as level will queue it into the global {@link ServerTickEvent}, otherwise it will be ticked with the
     * corresponding {@link LevelTickEvent}.
     *
     * @param level null or the specific {@link World}
     * @param c     the callback
     */
    public void addCallable(World level, ILevelRunnable c) {
        Preconditions.checkArgument(level == null || !level.isRemote, "Can only register serverside callbacks");

        if (level == null) {
            this.serverQueue.add(c);
        } else {
            Queue<ILevelRunnable> queue = this.callQueue.get(level);

            if (queue == null) {
                queue = new ArrayDeque<>();
                this.callQueue.put(level, queue);
            }

            queue.add(c);
        }
    }

    /**
     * Add a {@link AEBaseBlockEntity} to be initializes with the next update.
     *
     * @see appeng.api.networking.GridHelper#onFirstTick
     */
    public <T extends TileEntity> void addInit(T blockEntity, Consumer<? super T> initFunction) {
        Objects.requireNonNull(blockEntity);

        // for no there is no reason to care about this on the client...
        if (!blockEntity.getWorld().isRemote) {
            this.blockEntities.addBlockEntity(blockEntity, initFunction);
        }
    }

    /**
     * Add a new grid for ticking on the next update.
     * <p>
     * Must only be called on the server.
     *
     * @param grid the {@link Grid} to add, must be not null
     */
    public void addNetwork(Grid grid) {
        Platform.assertServerThread();

        this.grids.addNetwork(grid);
    }

    /**
     * Mark a {@link Grid} to be removed with the next update.
     * <p>
     * Must only be called on the server.
     *
     * @param grid the {@link Grid} to remove, must be not null
     */
    public void removeNetwork(Grid grid) {
        Platform.assertServerThread();

        this.grids.removeNetwork(grid);
    }

    public Set<Grid> getGridList() {
        Platform.assertServerThread();
        return this.grids.getNetworks();
    }

    public void shutdown() {
        Platform.assertServerThread();
        this.blockEntities.clear();
        this.grids.clear();
    }

    /**
     * Handles a chunk being unloaded (on the server)
     * <p>
     * Removes any pending initialization callbacks for block entities in that chunk.
     */
    @SubscribeEvent
    public void onUnloadChunk(final ChunkEvent.Unload ev) {
        var level = ev.getWorld();
        var chunk = ev.getChunk();

        if (!level.isRemote) {
            this.blockEntities.removeChunk(level, ChunkPos.asLong(chunk.x, chunk.z));
        }
    }

    /**
     * Handle a level unload and tear down related data structures.
     */
    // Try to go last for level unloads since we use it to clean-up state
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUnloadLevel(final WorldEvent.Unload ev) {
        var level = ev.getWorld();

        if (level.isRemote) {
            return; // for no there is no reason to care about this on the client...
        }

        var toDestroy = new ArrayList<GridNode>();

        this.grids.updateNetworks();
        for (Grid g : this.grids.getNetworks()) {
            for (var n : g.getNodes()) {
                if (n.getLevel() == level) {
                    toDestroy.add((GridNode) n);
                }
            }
        }

        for (var n : toDestroy) {
            n.destroy();
        }

        this.blockEntities.removeLevel(level);
        this.callQueue.remove(level);
    }

    private void onServerLevelTickStart(WorldServer level) {
        var queue = this.callQueue.remove(level);
        processQueueElementsRemaining += this.processQueue(queue, level);
        var newQueue = this.callQueue.put(level, queue);
        // Some new tasks may have been added while we were processing the queue
        if (newQueue != null) {
            queue.addAll(newQueue);
        }

        // tick networks
        this.grids.updateNetworks();
        for (var g : this.grids.getNetworks()) {
            try {
                g.onLevelStartTick(level);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Ticking grid on start of level tick");
                g.fillCrashReportCategory(crashReport.makeCategory("Grid being ticked"));
                level.addWorldInfoToCrashReport(crashReport);
                throw new ReportedException(crashReport);
            }
        }
    }

    private void onServerLevelTickEnd(WorldServer level) {
        this.simulateCraftingJobs(level);
        this.readyBlockEntities(level);

        // tick networks
        for (var g : this.grids.getNetworks()) {
            try {
                g.onLevelEndTick(level);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Ticking grid on end of level tick");
                g.fillCrashReportCategory(crashReport.makeCategory("Grid being ticked"));
                level.addWorldInfoToCrashReport(crashReport);
                throw new ReportedException(crashReport);
            }
        }
    }

    private void onServerTickStart() {
        // Reset the stop watch on the start of each server tick.
        this.processQueueElementsProcessed = 0;
        this.processQueueElementsRemaining = 0;
        this.stopWatch.reset();

        // tick networks
        for (var g : this.grids.getNetworks()) {
            try {
                g.onServerStartTick();
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Ticking grid on start of server tick");
                g.fillCrashReportCategory(crashReport.makeCategory("Grid being ticked"));
                throw new ReportedException(crashReport);
            }
        }
    }

    private void onServerTickEnd() {
        // tick networks
        for (var g : this.grids.getNetworks()) {
            try {
                g.onServerEndTick();
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Ticking grid on end of server tick");
                g.fillCrashReportCategory(crashReport.makeCategory("Grid being ticked"));
                throw new ReportedException(crashReport);
            }
        }

        // cross level queue.
        processQueueElementsRemaining += this.processQueue(this.serverQueue, null);

        if (this.stopWatch.elapsed(TimeUnit.MILLISECONDS) > TIME_LIMIT_PROCESS_QUEUE_MILLISECONDS) {
            AELog.warn("Exceeded time limit of %d ms after processing %d queued tick callbacks (%d remain)",
                    TIME_LIMIT_PROCESS_QUEUE_MILLISECONDS, processQueueElementsProcessed,
                    processQueueElementsRemaining);
        }

        tickCounter++;
    }

    public void registerCraftingSimulation(World level, CraftingCalculation craftingCalculation) {
        Preconditions.checkArgument(!level.isRemote, "Trying to register a crafting job for a client-level");

        synchronized (this.craftingJobs) {
            this.craftingJobs.put(level, craftingCalculation);
        }
    }

    /**
     * Simulates the current crafting requests before they user can submit them to be processed.
     */
    private void simulateCraftingJobs(World level) {
        synchronized (this.craftingJobs) {
            final Collection<CraftingCalculation> jobSet = this.craftingJobs.get(level);

            if (!jobSet.isEmpty()) {
                final int jobSize = jobSet.size();
                final int microSecondsPerTick = AEConfig.instance().getCraftingCalculationTimePerTick() * 1000;
                final int simTime = Math.max(1, microSecondsPerTick / jobSize);

                final Iterator<CraftingCalculation> i = jobSet.iterator();

                while (i.hasNext()) {
                    final CraftingCalculation cj = i.next();
                    if (!cj.simulateFor(simTime)) {
                        i.remove();
                    }
                }
            }
        }
    }

    /**
     * Ready the block entities in this level. server-side only.
     */
    private void readyBlockEntities(WorldServer level) {
        var levelQueue = blockEntities.getBlockEntities(level);
        if (levelQueue == null) {
            return;
        }

        // Make a copy because this set may be modified when new chunks are loaded by an onReady call below
        long[] workSet = levelQueue.keySet().toLongArray();

        for (long packedChunkPos : workSet) {
            // Readies all of our block entities in this chunk as soon as it can tick BEs
            // The following test is equivalent to WorldServer#isPositionTickingWithEntitiesLoaded
            if (Platform.areBlockEntitiesTicking(level, packedChunkPos)) {
                // Take the currently waiting block entities for this chunk and ready them all. Should more block
                // entities be added to this chunk while we're working on it, a new list will be added automatically and
                // we'll work on this chunk again next tick.
                var chunkQueue = levelQueue.remove(packedChunkPos);
                if (chunkQueue == null) {
                    AELog.warn("Chunk %s was unloaded while we were readying block entities",
                            new ChunkPos((int) packedChunkPos, (int) (packedChunkPos >>> 32)));
                    continue; // This should never happen, chunk unloaded under our noses
                }

                for (var info : chunkQueue) {
                    // Only ready block entities which weren't destroyed in the meantime.
                    if (!info.blockEntity().isInvalid()) {
                        try {
                            // This could load more chunks, but the earliest time to be initialized is the next tick.
                            info.callInit();
                        } catch (Throwable t) {
                            CrashReport crashReport = CrashReport.makeCrashReport(t, "Readying AE2 block entity");

                            var category = crashReport.makeCategory("Block entity being readied");
                            category.addDetail("World", () -> String.valueOf(level.provider.getDimension()));
                            info.blockEntity().addInfoToCrashReport(category);

                            throw new ReportedException(crashReport);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process the {@link ILevelRunnable} queue in this {@link World}
     * <p>
     * This has a hard limit of about 50 ms before deferring further processing into the next tick.
     *
     * @param queue the queue to process
     * @param level the level in which the queue is processed or null for the server queue
     * @return the amount of remaining callbacks
     */
    private int processQueue(Queue<ILevelRunnable> queue, World level) {
        if (queue == null) {
            return 0;
        }

        // start the clock
        stopWatch.start();

        while (!queue.isEmpty()) {
            try {
                // call the first queue element.
                queue.poll().call(level);
                this.processQueueElementsProcessed++;

                if (stopWatch.elapsed(TimeUnit.MILLISECONDS) > TIME_LIMIT_PROCESS_QUEUE_MILLISECONDS) {
                    break;
                }
            } catch (Exception e) {
                AELog.warn(e);
            }
        }

        // stop watch for the next call
        stopWatch.stop();

        return queue.size();
    }

    public long getCurrentTick() {
        return tickCounter;
    }

    public List<ITextComponent> getBlockEntityReport() {
        return blockEntities.getReport();
    }
}

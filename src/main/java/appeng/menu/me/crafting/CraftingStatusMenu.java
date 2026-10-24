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

package appeng.menu.me.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

import appeng.api.config.CpuSelectionMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ITerminalHost;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.menu.ISubMenu;
import appeng.menu.MenuType;
import appeng.menu.guisync.GuiSync;
import appeng.menu.guisync.PacketWritable;
import appeng.menu.implementations.MenuTypeBuilder;

/**
 * @see appeng.client.gui.me.crafting.CraftingStatusScreen
 */
public class CraftingStatusMenu extends CraftingCPUMenu implements ISubMenu {

    private static final CraftingCpuList EMPTY_CPU_LIST = new CraftingCpuList(Collections.emptyList());

    private static final Comparator<CraftingCpuListEntry> CPU_COMPARATOR = Comparator
            .comparing((CraftingCpuListEntry e) -> e.name() == null)
            .thenComparing(e -> e.name() != null ? e.name().getUnformattedText() : "")
            .thenComparingInt(CraftingCpuListEntry::serial);

    private static final String ACTION_SELECT_CPU = "selectCpu";

    public static final MenuType<CraftingStatusMenu> TYPE = MenuTypeBuilder
            .create(CraftingStatusMenu::new, ITerminalHost.class)
            .build("craftingstatus");

    private final WeakHashMap<ICraftingCPU, Integer> cpuSerialMap = new WeakHashMap<>();

    private int nextCpuSerial = 1;

    private ImmutableSet<ICraftingCPU> lastCpuSet = ImmutableSet.of();

    private int lastUpdate = 0;

    @GuiSync(8)
    public CraftingCpuList cpuList = EMPTY_CPU_LIST;

    private final ITerminalHost host;

    // This is server-side
    @Nullable
    private ICraftingCPU selectedCpu = null;

    @GuiSync(9)
    private int selectedCpuSerial = -1;

    public CraftingStatusMenu(int id, InventoryPlayer ip, ITerminalHost host) {
        super(TYPE, id, ip, host);
        this.host = host;
        registerClientAction(ACTION_SELECT_CPU, Integer.class, this::selectCpu);
    }

    @Override
    public ITerminalHost getHost() {
        return host;
    }

    @Override
    protected void setCPU(ICraftingCPU c) {
        super.setCPU(c);
        this.selectedCpuSerial = getOrAssignCpuSerial(c);
    }

    @Override
    public void broadcastChanges() {
        IGrid network = this.getGrid();
        if (isServerSide() && network != null) {
            if (!lastCpuSet.equals(network.getCraftingService().getCpus())
                    // Always try to update once every second to show job progress
                    || ++lastUpdate >= 20) {
                lastCpuSet = network.getCraftingService().getCpus();
                cpuList = createCpuList();
            }
        } else {
            lastUpdate = 20;
            if (!lastCpuSet.isEmpty()) {
                cpuList = EMPTY_CPU_LIST;
                lastCpuSet = ImmutableSet.of();
            }
        }

        // Clear selection if CPU is no longer in list
        if (selectedCpuSerial != -1) {
            if (cpuList.cpus().stream().noneMatch(c -> c.serial() == selectedCpuSerial)) {
                selectCpu(-1);
            }
        }

        // Select a suitable CPU if none is selected
        if (selectedCpuSerial == -1) {
            // Try busy CPUs first
            for (var cpu : cpuList.cpus()) {
                if (cpu.currentJob() != null) {
                    selectCpu(cpu.serial());
                    break;
                }
            }
            // If we couldn't find a busy one, just select the first
            if (selectedCpuSerial == -1 && !cpuList.cpus().isEmpty()) {
                selectCpu(cpuList.cpus().get(0).serial());
            }
        }

        super.broadcastChanges();
    }

    private CraftingCpuList createCpuList() {
        var entries = new ArrayList<CraftingCpuListEntry>(lastCpuSet.size());
        for (var cpu : lastCpuSet) {
            var serial = getOrAssignCpuSerial(cpu);
            var status = cpu.getJobStatus();
            var progress = 0f;
            if (status != null && status.totalItems() > 0) {
                progress = (float) (status.progress() / (double) status.totalItems());
            }
            entries.add(new CraftingCpuListEntry(
                    serial,
                    cpu.getAvailableStorage(),
                    cpu.getCoProcessors(),
                    cpu.getName(),
                    cpu.getSelectionMode(),
                    status != null ? status.crafting() : null,
                    progress,
                    status != null ? status.elapsedTimeNanos() : 0));
        }
        entries.sort(CPU_COMPARATOR);
        return new CraftingCpuList(entries);
    }

    private int getOrAssignCpuSerial(ICraftingCPU cpu) {
        return cpuSerialMap.computeIfAbsent(cpu, ignored -> nextCpuSerial++);
    }

    @Override
    public boolean allowConfiguration() {
        return false;
    }

    public void selectCpu(int serial) {
        if (isClientSide()) {
            selectedCpuSerial = serial;
            sendClientAction(ACTION_SELECT_CPU, serial);
        } else {
            ICraftingCPU newSelectedCpu = null;
            if (serial != -1) {
                for (var cpu : lastCpuSet) {
                    if (cpuSerialMap.getOrDefault(cpu, -1) == serial) {
                        newSelectedCpu = cpu;
                        break;
                    }
                }
            }

            if (newSelectedCpu != selectedCpu) {
                setCPU(newSelectedCpu);
            }
        }
    }

    public int getSelectedCpuSerial() {
        return selectedCpuSerial;
    }

    public final static class CraftingCpuList implements PacketWritable {
        private final List<CraftingCpuListEntry> cpus;

        public CraftingCpuList(List<CraftingCpuListEntry> cpus) {
            this.cpus = cpus;
        }

        public List<CraftingCpuListEntry> cpus() {
            return cpus;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CraftingCpuList)) return false;
            CraftingCpuList that = (CraftingCpuList) o;
            return java.util.Objects.equals(cpus, that.cpus);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(cpus);
        }

        @Override
        public String toString() {
            return "CraftingCpuList[" + "cpus=" + cpus + "]";
        }

        public CraftingCpuList(RegistryFriendlyByteBuf data) {
            this(readFromPacket(data));
        }

        private static List<CraftingCpuListEntry> readFromPacket(RegistryFriendlyByteBuf data) {
            var count = data.readInt();
            var result = new ArrayList<CraftingCpuListEntry>(count);
            for (int i = 0; i < count; i++) {
                result.add(CraftingCpuListEntry.readFromPacket(data));
            }
            return result;
        }

        @Override
        public void writeToPacket(RegistryFriendlyByteBuf data) {
            data.writeInt(cpus.size());
            for (var entry : cpus) {
                entry.writeToPacket(data);
            }
        }
    }

    public final static class CraftingCpuListEntry {
        private final int serial;
        private final long storage;
        private final int coProcessors;
        private final ITextComponent name;
        private final CpuSelectionMode mode;
        private final GenericStack currentJob;
        private final float progress;
        private final long elapsedTimeNanos;

        public CraftingCpuListEntry(int serial, long storage, int coProcessors, ITextComponent name, CpuSelectionMode mode, GenericStack currentJob, float progress, long elapsedTimeNanos) {
            this.serial = serial;
            this.storage = storage;
            this.coProcessors = coProcessors;
            this.name = name;
            this.mode = mode;
            this.currentJob = currentJob;
            this.progress = progress;
            this.elapsedTimeNanos = elapsedTimeNanos;
        }

        public int serial() {
            return serial;
        }

        public long storage() {
            return storage;
        }

        public int coProcessors() {
            return coProcessors;
        }

        public ITextComponent name() {
            return name;
        }

        public CpuSelectionMode mode() {
            return mode;
        }

        public GenericStack currentJob() {
            return currentJob;
        }

        public float progress() {
            return progress;
        }

        public long elapsedTimeNanos() {
            return elapsedTimeNanos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CraftingCpuListEntry)) return false;
            CraftingCpuListEntry that = (CraftingCpuListEntry) o;
            return serial == that.serial
                    && storage == that.storage
                    && coProcessors == that.coProcessors
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(mode, that.mode)
                    && java.util.Objects.equals(currentJob, that.currentJob)
                    && Float.compare(progress, that.progress) == 0
                    && elapsedTimeNanos == that.elapsedTimeNanos;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(serial, storage, coProcessors, name, mode, currentJob, progress, elapsedTimeNanos);
        }

        @Override
        public String toString() {
            return "CraftingCpuListEntry[" + "serial=" + serial + ", " + "storage=" + storage + ", " + "coProcessors=" + coProcessors + ", " + "name=" + name + ", " + "mode=" + mode + ", " + "currentJob=" + currentJob + ", " + "progress=" + progress + ", " + "elapsedTimeNanos=" + elapsedTimeNanos + "]";
        }

        public static CraftingCpuListEntry readFromPacket(RegistryFriendlyByteBuf data) {
            return new CraftingCpuListEntry(
                    data.readInt(),
                    data.readLong(),
                    data.readInt(),
                    data.readBoolean() ? ByteBufCodecs.COMPONENT.decode(data) : null,
                    data.readEnum(CpuSelectionMode.class),
                    GenericStack.readBuffer(data),
                    data.readFloat(),
                    data.readVarLong());
        }

        public void writeToPacket(RegistryFriendlyByteBuf data) {
            data.writeInt(serial);
            data.writeLong(storage);
            data.writeInt(coProcessors);
            data.writeBoolean(name != null);
            if (name != null) {
                ByteBufCodecs.COMPONENT.encode(data, name);
            }
            data.writeEnum(mode);
            GenericStack.writeBuffer(currentJob, data);
            data.writeFloat(progress);
            data.writeVarLong(elapsedTimeNanos);
        }
    }
}

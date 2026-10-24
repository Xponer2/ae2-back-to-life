package appeng.server.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import appeng.core.AppEng;
import appeng.core.worlddata.AESavedData;

/**
 * Implementation detail of {@link ChunkLoadingService} on Fabric, as {@code ForgeChunkManager} is not available there.
 */
class ChunkLoadState extends AESavedData {

    public static final String NAME = AppEng.MOD_ID + "_chunk_load_state";

    public static ChunkLoadState get(WorldServer level) {
        return level.getDataStorage().computeIfAbsent(
                new WorldSavedData.Factory<>(
                        () -> new ChunkLoadState(level),
                        (tag, provider) -> new ChunkLoadState(level, tag),
                        null),
                NAME);
    }

    private final WorldServer level;
    private final Long2ObjectMap<Set<BlockPos>> forceLoadedChunks = new Long2ObjectOpenHashMap<>();

    private ChunkLoadState(WorldServer level) {
        this.level = level;
    }

    private ChunkLoadState(WorldServer level, NBTTagCompound tag) {
        this(level);
        var forcedChunks = tag.getTagList("forcedChunks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < forcedChunks.tagCount(); ++i) {
            var forcedChunk = forcedChunks.getCompoundTag(i);
            var chunkPos = new ChunkPos(forcedChunk.getInt("cx"), forcedChunk.getInt("cz"));

            var blockSet = new HashSet<BlockPos>();
            for (long blockPos : forcedChunk.getLongArray("blocks")) {
                blockSet.add(BlockPos.fromLong(blockPos));
            }

            forceLoadedChunks.put(chunkPos.toLong(), blockSet);
        }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound tag) {
        var forcedChunks = new NBTTagList();
        for (var entry : forceLoadedChunks.long2ObjectEntrySet()) {
            var chunkPos = new ChunkPos(entry.getLongKey());

            var forcedChunk = new NBTTagCompound();
            forcedChunk.setInteger("cx", chunkPos.x);
            forcedChunk.setInteger("cz", chunkPos.z);

            var list = new NBTTagLongArray(entry.getValue().stream().map(BlockPos::asLong).collect(Collectors.toList()));
            forcedChunk.setTag("blocks", list);

            forcedChunks.appendTag(forcedChunk);
        }
        tag.setTag("forcedChunks", forcedChunks);
        return tag;
    }

    /**
     * @param chunkPos  Chunk to load.
     * @param sourcePos Source of the chunk load request.
     */
    public void forceChunk(ChunkPos chunkPos, BlockPos sourcePos) {
        long chunk = chunkPos.toLong();
        forceLoadedChunks.computeIfAbsent(chunk, pos -> new HashSet<>()).add(sourcePos.toImmutable());

        level.setChunkForced(chunkPos.x, chunkPos.z, true);
        setDirty();
    }

    public void releaseChunk(ChunkPos chunkPos, BlockPos sourcePos) {
        var map = forceLoadedChunks.get(chunkPos.toLong());
        if (map == null) {
            return;
        }

        map.remove(sourcePos);
        if (map.isEmpty()) {
            forceLoadedChunks.remove(chunkPos.toLong());
            level.setChunkForced(chunkPos.x, chunkPos.z, false);
        }
        setDirty();
    }

    public void releaseAll(BlockPos sourcePos) {
        var relevantChunks = forceLoadedChunks.long2ObjectEntrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(sourcePos))
                .mapToLong(Long2ObjectMap.Entry::getLongKey)
                .toArray();

        for (var chunk : relevantChunks) {
            releaseChunk(new ChunkPos(chunk), sourcePos);
        }
    }

    public Map<BlockPos, LongSet> getAllBlocks() {
        var blocks = new HashMap<BlockPos, LongSet>();
        for (var entry : forceLoadedChunks.long2ObjectEntrySet()) {
            for (var blockPos : entry.getValue()) {
                blocks.computeIfAbsent(blockPos, pos -> new LongOpenHashSet()).add(entry.getLongKey());
            }
        }
        return blocks;
    }

    public boolean isForceLoaded(int cx, int cz) {
        return forceLoadedChunks.containsKey(ChunkPos.asLong(cx, cz));
    }
}

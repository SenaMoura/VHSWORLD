package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GrassroomsLayout {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ChunkPos, List<BlockPos>> GRASSROOMS = new ConcurrentHashMap<>();

    public static void addGrassroom(ChunkPos chunkPos, BlockPos blockPos) {
        GRASSROOMS.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(blockPos);
    }

    public static List<BlockPos> getGrassroomsInChunk(ChunkPos chunkPos) {
        return GRASSROOMS.getOrDefault(chunkPos, new ArrayList<>());
    }
}


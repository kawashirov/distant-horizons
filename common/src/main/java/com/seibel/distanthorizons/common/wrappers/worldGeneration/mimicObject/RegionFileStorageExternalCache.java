package com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject;

import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RegionFileStorageExternalCache implements AutoCloseable {
    public final RegionFileStorage storage;
    public static final int MAX_CACHE_SIZE = 16;

    @Override
    public void close() throws IOException {
        RegionFileCache cache;
        while ((cache = regionFileCache.poll()) != null) {
            cache.file.close();
        }
    }

    static class RegionFileCache {
        public final long pos;
        public final RegionFile file;

        public RegionFileCache(long pos, RegionFile file) {
            this.pos = pos;
            this.file = file;
        }
    }

    public ConcurrentLinkedQueue<RegionFileCache> regionFileCache = new ConcurrentLinkedQueue<>();

    public RegionFileStorageExternalCache(RegionFileStorage storage) {
        this.storage = storage;
    }

    @Nullable
    public RegionFile getRegionFile(ChunkPos pos) throws IOException
	{
		long posLong = ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ());
		RegionFile rFile;

		// Check vanilla cache
		while (true)
		{
			try
			{
				rFile = this.storage.regionCache.getOrDefault(posLong, null);
				break;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				BatchGenerationEnvironment.LOAD_LOGGER.warn("Concurrency issue detected when getting region file for chunk at " + pos + ". Retrying...");
			}
		}
		
		if (rFile != null)
		{
			return rFile;
		}

		// Then check our custom cache
		for (RegionFileCache cache : this.regionFileCache)
		{
			if (cache.pos == posLong)
			{
				return cache.file;
			}
		}
		
		// Otherwise, check if file exist, and if so, add it to the cache
		Path p = storage.folder;
		if (!Files.exists(p))
		{
			return null;
		}
		
		Path rFilePath = p.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
		rFile = new RegionFile(rFilePath, p, false);
		regionFileCache.add(new RegionFileCache(ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ()), rFile));
		while (regionFileCache.size() > MAX_CACHE_SIZE)
		{
			regionFileCache.poll().file.close();
		}
		
		return rFile;
	}
	
	
    @Nullable
    public CompoundTag read(ChunkPos pos) throws IOException
	{
		RegionFile file = getRegionFile(pos);
		if (file == null)
		{
			return null;
		}
		
		
		try (DataInputStream stream = file.getChunkDataInputStream(pos))
		{
			if (stream == null)
			{
				return null;
			}
			
			return NbtIo.read(stream);
		}
		catch (Throwable e)
		{
			return null;
		}
	}

}

package de.cas_ual_ty.beaconchunkloaders;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class ChunkLoaderList implements IChunkLoaderList
{
    private Long2IntMap refCount = new Long2IntOpenHashMap();
    private LongSet loaders = new LongOpenHashSet();
    private boolean loading = false;
    @Nullable
    private final ServerWorld world;
    
    public ChunkLoaderList(@Nullable ServerWorld world)
    {
        this.refCount.defaultReturnValue(Integer.MIN_VALUE);
        this.world = world;
    }
    
    @Override
    public void add(BlockPos pos)
    {
        long block = pos.toLong();
        if(!this.loaders.contains(block))
        {
            long chunk = this.toChunk(pos);
            int ref = this.refCount.get(chunk);
            if(ref == Integer.MIN_VALUE)
            {
                if(!this.loading)
                {
                    this.force(pos);
                }
                ref = 1;
            }
            else
            {
                ref += 1;
            }
            this.refCount.put(chunk, ref);
            this.loaders.add(block);
        }
    }
    
    @Override
    public void remove(BlockPos pos)
    {
        if(this.loaders.remove(pos.toLong()))
        {
            long chunk = this.toChunk(pos);
            int ref = this.refCount.get(chunk);
            
            if(ref == Integer.MIN_VALUE || --ref <= 0)
            {
                if(!this.loading)
                {
                    this.unforce(pos);
                }
                this.refCount.remove(chunk);
            }
            else
            {
                this.refCount.put(chunk, ref);
            }
        }
    }
    
    @Override
    public boolean contains(BlockPos pos)
    {
        return this.loaders.contains(pos.toLong());
    }
    
    private final long toChunk(BlockPos pos)
    {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }
    
    private void force(BlockPos pos)
    {
        this.forceload(pos, "add");
    }
    
    private void unforce(BlockPos pos)
    {
        this.forceload(pos, "remove");
    }
    
    private void forceload(BlockPos pos, String action)
    {
        if(this.world == null || this.world.getServer() == null)
        {
            return;
        }
        CommandSource source = this.world.getServer().getCommandSource().withWorld(this.world); //TODO: Use custom source that doesn't spam chat?
        @SuppressWarnings("unused")
        int ret = this.world.getServer().getCommandManager().handleCommand(source, "forceload " + action + " " + pos.getX() + " " + pos.getZ());
        
        //Lame feedback.
        //BasicParticleType particle = ret == 0 ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.HAPPY_VILLAGER;
        //world.spawnParticle(particle, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5F, 5, 0.5D, 0.5D, 0.5D, 0.0D);
    }
    
    public static class Storage implements IStorage<IChunkLoaderList>
    {
        @Override
        public INBT writeNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance, Direction side)
        {
            if(!(instance instanceof ChunkLoaderList))
            {
                return null;
            }
            ChunkLoaderList list = (ChunkLoaderList)instance;
            long[] data = new long[list.loaders.size()];
            int idx = 0;
            for(long l : list.loaders)
            {
                data[idx++] = l;
            }
            return new LongArrayNBT(data);
        }
        
        @Override
        public void readNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance, Direction side, INBT nbt)
        {
            if(!(instance instanceof ChunkLoaderList) || !(nbt instanceof LongArrayNBT))
            {
                return;
            }
            ChunkLoaderList list = (ChunkLoaderList)instance;
            list.loading = true;
            list.refCount.clear();
            list.loaders.clear();
            try
            {
                for(long l : ((LongArrayNBT)nbt).getAsLongArray())
                {
                    list.add(BlockPos.fromLong(l));
                }
                if(list.world != null)
                {
                    // Run the force commands next tick to make sure they wern't removed.
                    list.world.getServer().enqueue(new TickDelayedTask(1, () ->
                    {
                        for(long l : list.refCount.keySet())
                        {
                            ChunkPos chunk = new ChunkPos(l);
                            list.force(new BlockPos(chunk.x << 4, 0, chunk.z << 4));
                        }
                    }));
                }
            }
            finally
            {
                list.loading = false;
            }
        }
    }
}

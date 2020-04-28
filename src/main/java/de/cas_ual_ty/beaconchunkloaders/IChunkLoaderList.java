package de.cas_ual_ty.beaconchunkloaders;

import net.minecraft.util.math.BlockPos;

public interface IChunkLoaderList
{
    void add(BlockPos pos);
    
    void remove(BlockPos pos);
    
    boolean contains(BlockPos pos);
}

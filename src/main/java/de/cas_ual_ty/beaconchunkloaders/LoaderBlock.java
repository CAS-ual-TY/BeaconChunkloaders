package de.cas_ual_ty.beaconchunkloaders;

import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LoaderBlock extends BeaconBlock
{
    public LoaderBlock(Properties properties)
    {
        super(properties);
    }
    
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        if(world.isRemote)
        {
            return;
        }
        world.getCapability(BeaconChunkloaders.CAPABILITY, null).ifPresent(cap -> cap.add(pos));
    }
    
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(world.isRemote)
        {
            return;
        }
        world.getCapability(BeaconChunkloaders.CAPABILITY, null).ifPresent(cap -> cap.remove(pos));
    }
}

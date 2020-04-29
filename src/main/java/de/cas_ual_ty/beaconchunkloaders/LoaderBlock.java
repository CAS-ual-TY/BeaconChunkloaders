package de.cas_ual_ty.beaconchunkloaders;

import java.util.List;

import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
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
        if(!world.isRemote)
        {
            world.getCapability(BeaconChunkloaders.CAPABILITY, null).ifPresent(cap -> cap.add(pos));
        }
        
        super.onBlockAdded(state, world, pos, oldState, isMoving);
    }
    
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(!world.isRemote)
        {
            world.getCapability(BeaconChunkloaders.CAPABILITY, null).ifPresent(cap -> cap.remove(pos));
        }
        
        super.onReplaced(state, world, pos, newState, isMoving);
    }
    
    @Override
    public void addInformation(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(new TranslationTextComponent("beaconchunkloaders.info"));
        super.addInformation(stack, worldIn, tooltip, flagIn);
    }
}

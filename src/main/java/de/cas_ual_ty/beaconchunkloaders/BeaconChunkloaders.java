package de.cas_ual_ty.beaconchunkloaders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(BeaconChunkloaders.MODID)
public class BeaconChunkloaders
{
    public static final String MODID = "beaconchunkloaders";
    public static final String BEACON_ID = "beacon";
    
    public static final Logger LOGGER = LogManager.getLogger();
    
    @CapabilityInject(IChunkLoaderList.class)
    public static Capability<IChunkLoaderList> CAPABILITY = null;
    
    private static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, "minecraft");
    private static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, "minecraft");
    
    public static final RegistryObject<Block> NEW_BEACON_BLOCK = BeaconChunkloaders.BLOCKS.register(BeaconChunkloaders.BEACON_ID, () -> new LoaderBlock(Block.Properties.create(Material.GLASS, MaterialColor.DIAMOND).hardnessAndResistance(3.0F).lightValue(15).notSolid()));
    public static final RegistryObject<Item> NEW_BEACON_ITEM = BeaconChunkloaders.ITEMS.register(BeaconChunkloaders.BEACON_ID, () -> new BlockItem(BeaconChunkloaders.NEW_BEACON_BLOCK.get(), (new Item.Properties()).group(ItemGroup.MISC).rarity(Rarity.RARE)));
    
    public BeaconChunkloaders()
    {
        BeaconChunkloaders.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BeaconChunkloaders.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void setup(final FMLCommonSetupEvent event)
    {
        CapabilityManager.INSTANCE.register(IChunkLoaderList.class, new ChunkLoaderList.Storage(), () -> new ChunkLoaderList(null));
    }
    
    @SubscribeEvent
    public void attachWorldCaps(AttachCapabilitiesEvent<World> event)
    {
        if(!(event.getObject() instanceof ServerWorld))
        {
            return;
        }
        final LazyOptional<IChunkLoaderList> inst = LazyOptional.of(() -> new ChunkLoaderList((ServerWorld)event.getObject()));
        final ICapabilitySerializable<INBT> provider = new ICapabilitySerializable<INBT>()
        {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
            {
                return BeaconChunkloaders.CAPABILITY.orEmpty(cap, inst);
            }
            
            @Override
            public INBT serializeNBT()
            {
                return BeaconChunkloaders.CAPABILITY.writeNBT(inst.orElse(null), null);
            }
            
            @Override
            public void deserializeNBT(INBT nbt)
            {
                BeaconChunkloaders.CAPABILITY.readNBT(inst.orElse(null), null, nbt);
            }
        };
        event.addCapability(new ResourceLocation(BeaconChunkloaders.MODID, BeaconChunkloaders.BEACON_ID), provider);
        event.addListener(() -> inst.invalidate());
    }
}

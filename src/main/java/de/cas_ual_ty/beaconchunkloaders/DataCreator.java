package de.cas_ual_ty.beaconchunkloaders;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.LootTableProvider;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootParameterSet;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.ValidationTracker;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@EventBusSubscriber(modid = BeaconChunkloaders.MODID, bus = Bus.MOD)
public class DataCreator
{
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();
        
        if(event.includeServer())
        {
            gen.addProvider(new Recipes(gen));
            gen.addProvider(new Loots(gen));
        }
        if(event.includeClient())
        {
            //TODO: Generate models when Forge/Someone makes model data generators
            gen.addProvider(new Language(gen, BeaconChunkloaders.MODID));
        }
    }
    
    private static void save(DirectoryCache cache, Object object, Path target) throws IOException
    {
        String data = DataCreator.GSON.toJson(object);
        String hash = IDataProvider.HASH_FUNCTION.hashUnencodedChars(data).toString();
        if(!Objects.equals(cache.getPreviousHash(target), hash) || !Files.exists(target))
        {
            Files.createDirectories(target.getParent());
            
            try(BufferedWriter bufferedwriter = Files.newBufferedWriter(target))
            {
                bufferedwriter.write(data);
            }
        }
        
        cache.recordHash(target, hash);
    }
    
    private static class Recipes extends RecipeProvider
    {
        public Recipes(DataGenerator gen)
        {
            super(gen);
        }
        
        @Override
        protected void registerRecipes(Consumer<IFinishedRecipe> consumer)
        {
            ShapedRecipeBuilder.shapedRecipe(BeaconChunkloaders.LOADER_BLOCK.get(), 10)
                .key('O', Items.ENDER_PEARL).key('E', Blocks.ENCHANTING_TABLE)
                .patternLine("OOO").patternLine("OEO").patternLine("OOO")
                .addCriterion("has_ender_pearl", this.hasItem(Items.ENDER_PEARL))
                .build(consumer);
        }
    }
    
    private static class Loots extends LootTableProvider
    {
        public Loots(DataGenerator gen)
        {
            super(gen);
        }
        
        @Override
        public String getName()
        {
            return "LootTables";
        }
        
        @Override
        protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables()
        {
            return ImmutableList.of(Pair.of(Blocks::new, LootParameterSets.BLOCK));
        }
        
        @Override
        protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker tracker)
        {
        } //We don't do validation
        
        private class Blocks extends BlockLootTables
        {
            private Set<Block> knownBlocks = new HashSet<>();
            
            @Override
            protected void addTables()
            {
                this.registerDropSelfLootTable(BeaconChunkloaders.LOADER_BLOCK.get());
            }
            
            @Override
            public void registerDropSelfLootTable(Block block)
            {
                this.knownBlocks.add(block);
                super.registerDropSelfLootTable(block);
            }
            
            @Override
            protected Iterable<Block> getKnownBlocks()
            {
                return this.knownBlocks;
            }
        }
    }
    
    private static class Language implements IDataProvider
    {
        private final DataGenerator gen;
        private final String modid;
        private final Map<String, String> data = new TreeMap<>();
        
        public Language(DataGenerator gen, String modid)
        {
            this.gen = gen;
            this.modid = modid;
        }
        
        private void addTranslations()
        {
            this.add(BeaconChunkloaders.LOADER_BLOCK.get(), "Single Chunk Loader");
        }
        
        @Override
        public String getName()
        {
            return "Languages";
        }
        
        @Override
        public void act(DirectoryCache cache) throws IOException
        {
            this.addTranslations();
            
            if(!this.data.isEmpty())
            {
                DataCreator.save(cache, this.data, this.gen.getOutputFolder().resolve("assets/" + this.modid + "/lang/en_us.json"));
            }
        }
        
        private void add(Block block, String name)
        {
            this.add(block.getTranslationKey(), name);
        }
        
        private void add(String key, String value)
        {
            if(this.data.put(key, value) != null)
            {
                throw new IllegalStateException("Duplicate translation key " + key);
            }
        }
    }
}

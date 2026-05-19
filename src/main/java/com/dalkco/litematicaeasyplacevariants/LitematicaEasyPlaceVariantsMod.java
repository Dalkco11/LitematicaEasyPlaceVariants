package com.dalkco.litematicaeasyplacevariants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class LitematicaEasyPlaceVariantsMod implements ClientModInitializer {
    public static final String MOD_ID = "litematica-easyplace-variants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ConfigBoolean ENABLE_VARIANTS = new ConfigBoolean(
        "easyPlaceEnableVariants", 
        true, 
        "Включить/выключить автоматическую замену вариантов блоков (дерево, камень) во время Easy Place."
    );
    public static final ConfigBoolean WOODEN_VARIANTS = new ConfigBoolean(
        "easyPlaceWoodenVariants", 
        true, 
        "Включить/выключить замену деревянных вариантов блоков (заборы, ступени, плиты, двери, люки, брёвна и т.д.)."
    );
    public static final ConfigBoolean STONE_VARIANTS = new ConfigBoolean(
        "easyPlaceStoneVariants", 
        true, 
        "Включить/выключить замену каменных вариантов блоков (стены, ступени, плиты и песчаник)."
    );
    public static final ConfigBoolean VERIFY_VARIANTS_STATE = new ConfigBoolean(
        "easyPlaceVerifyVariantsState", 
        true, 
        "Показывать эквивалентные варианты блоков как правильно установленные, если их состояние (поворот, направление) совпадает со схемой."
    );
    public static final ConfigBoolean EASY_PLACE_FIX = new ConfigBoolean(
        "easyPlaceFix", 
        true, 
        "Корректировать поворот (направление) устанавливаемых блоков при Easy Place, чтобы они вставали ровно по схеме (даже на ванильных серверах)."
    );

    public static final List<ConfigBoolean> OPTIONS = List.of(
        ENABLE_VARIANTS, 
        WOODEN_VARIANTS, 
        STONE_VARIANTS, 
        VERIFY_VARIANTS_STATE,
        EASY_PLACE_FIX
    );
    public static boolean isCustomTabActive = false;
    public static float originalYaw = 0.0f;
    public static float originalPitch = 0.0f;
    public static boolean isRotating = false;

    private static File configFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Litematica Easy Place Variants initialized!");
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "litematica-easyplace-variants.json");
        loadConfig();
    }

    public static void loadConfig() {
        if (configFile == null || !configFile.exists()) {
            saveConfig();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                if (json.has("enableVariants")) {
                    ENABLE_VARIANTS.setBooleanValue(json.get("enableVariants").getAsBoolean());
                }
                if (json.has("woodenVariants")) {
                    WOODEN_VARIANTS.setBooleanValue(json.get("woodenVariants").getAsBoolean());
                }
                if (json.has("stoneVariants")) {
                    STONE_VARIANTS.setBooleanValue(json.get("stoneVariants").getAsBoolean());
                }
                if (json.has("verifyVariantsState")) {
                    VERIFY_VARIANTS_STATE.setBooleanValue(json.get("verifyVariantsState").getAsBoolean());
                }
                if (json.has("easyPlaceFix")) {
                    EASY_PLACE_FIX.setBooleanValue(json.get("easyPlaceFix").getAsBoolean());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Litematica Easy Place Variants configuration", e);
        }
    }

    public static void saveConfig() {
        if (configFile == null) return;
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject json = new JsonObject();
                json.addProperty("enableVariants", ENABLE_VARIANTS.getBooleanValue());
                json.addProperty("woodenVariants", WOODEN_VARIANTS.getBooleanValue());
                json.addProperty("stoneVariants", STONE_VARIANTS.getBooleanValue());
                json.addProperty("verifyVariantsState", VERIFY_VARIANTS_STATE.getBooleanValue());
                json.addProperty("easyPlaceFix", EASY_PLACE_FIX.getBooleanValue());
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save Litematica Easy Place Variants configuration", e);
        }
    }

    public static boolean isEquivalent(ItemStack req, ItemStack candidate) {
        if (!ENABLE_VARIANTS.getBooleanValue()) {
            return false;
        }
        if (req == null || candidate == null || req.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        if (req.getItem() == candidate.getItem()) {
            return true;
        }
        Block reqBlock = Block.byItem(req.getItem());
        Block candBlock = Block.byItem(candidate.getItem());
        if (reqBlock == Blocks.AIR || candBlock == Blocks.AIR) {
            return false;
        }
        if (reqBlock.defaultBlockState().is(BlockTags.BEDS) && candBlock.defaultBlockState().is(BlockTags.BEDS)) {
            return true;
        }

        if (WOODEN_VARIANTS.getBooleanValue()) {
            List<TagKey<Block>> woodenTags = List.of(
                BlockTags.WOODEN_STAIRS,
                BlockTags.WOODEN_SLABS,
                BlockTags.WOODEN_FENCES,
                BlockTags.WOODEN_TRAPDOORS,
                BlockTags.WOODEN_DOORS,
                BlockTags.WOODEN_PRESSURE_PLATES,
                BlockTags.WOODEN_BUTTONS,
                BlockTags.ALL_SIGNS,
                BlockTags.ALL_HANGING_SIGNS,
                BlockTags.FENCE_GATES,
                BlockTags.PLANKS,
                BlockTags.LOGS
            );

            for (TagKey<Block> tag : woodenTags) {
                if (reqBlock.defaultBlockState().is(tag) && candBlock.defaultBlockState().is(tag)) {
                    return true;
                }
            }
        }

        if (STONE_VARIANTS.getBooleanValue()) {
            if (reqBlock.defaultBlockState().is(BlockTags.WALLS) && candBlock.defaultBlockState().is(BlockTags.WALLS)) {
                return true;
            }

            if (reqBlock.defaultBlockState().is(BlockTags.STAIRS) && !reqBlock.defaultBlockState().is(BlockTags.WOODEN_STAIRS)) {
                return candBlock.defaultBlockState().is(BlockTags.STAIRS) && !candBlock.defaultBlockState().is(BlockTags.WOODEN_STAIRS);
            }

            if (reqBlock.defaultBlockState().is(BlockTags.SLABS) && !reqBlock.defaultBlockState().is(BlockTags.WOODEN_SLABS)) {
                return candBlock.defaultBlockState().is(BlockTags.SLABS) && !candBlock.defaultBlockState().is(BlockTags.WOODEN_SLABS);
            }
        }

        return false;
    }

    public static boolean areStatesEquivalent(BlockState schematicState, BlockState worldState) {
        if (schematicState == worldState) {
            return true;
        }

        ItemStack req = new ItemStack(schematicState.getBlock().asItem());
        ItemStack candidate = new ItemStack(worldState.getBlock().asItem());

        if (!isEquivalent(req, candidate)) {
            return false;
        }

        for (net.minecraft.world.level.block.state.properties.Property<?> prop : schematicState.getProperties()) {
            if (worldState.hasProperty(prop)) {
                if (!schematicState.getValue(prop).equals(worldState.getValue(prop))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static float[] getRequiredLookDirection(BlockState state) {
        if (state == null) {
            return null;
        }

        Block block = state.getBlock();
        boolean isTrapdoor = block.getClass().getSimpleName().toLowerCase().contains("trapdoor");
        
        boolean isOrientedByPlayer = block instanceof net.minecraft.world.level.block.StairBlock
            || block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
            || block instanceof net.minecraft.world.level.block.ObserverBlock
            || block instanceof net.minecraft.world.level.block.DispenserBlock
            || block instanceof net.minecraft.world.level.block.DropperBlock
            || block instanceof net.minecraft.world.level.block.ChestBlock
            || block instanceof net.minecraft.world.level.block.EnderChestBlock
            || block instanceof net.minecraft.world.level.block.FurnaceBlock
            || block instanceof net.minecraft.world.level.block.BlastFurnaceBlock
            || block instanceof net.minecraft.world.level.block.SmokerBlock
            || block instanceof net.minecraft.world.level.block.FenceGateBlock
            || block instanceof net.minecraft.world.level.block.DoorBlock
            || block instanceof net.minecraft.world.level.block.RepeaterBlock
            || block instanceof net.minecraft.world.level.block.ComparatorBlock
            || isTrapdoor;

        if (!isOrientedByPlayer) {
            return null;
        }

        Direction facing = null;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }

        if (facing == null) {
            return null;
        }

        boolean opposite = false;
        if (block instanceof net.minecraft.world.level.block.ObserverBlock 
            || block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
            || block instanceof net.minecraft.world.level.block.DispenserBlock
            || block instanceof net.minecraft.world.level.block.DropperBlock) {
            opposite = true;
        } else if (block instanceof net.minecraft.world.level.block.ChestBlock
            || block instanceof net.minecraft.world.level.block.EnderChestBlock
            || block instanceof net.minecraft.world.level.block.FurnaceBlock
            || block instanceof net.minecraft.world.level.block.BlastFurnaceBlock
            || block instanceof net.minecraft.world.level.block.SmokerBlock
            || isTrapdoor) {
            opposite = true;
        }

        Direction targetDirection = opposite ? facing.getOpposite() : facing;

        float yaw = 0.0f;
        float pitch = 0.0f;

        switch (targetDirection) {
            case SOUTH: yaw = 0.0f; break;
            case WEST: yaw = 90.0f; break;
            case NORTH: yaw = 180.0f; break;
            case EAST: yaw = 270.0f; break;
            case UP: pitch = -90.0f; break;
            case DOWN: pitch = 90.0f; break;
        }

        return new float[]{yaw, pitch};
    }
}

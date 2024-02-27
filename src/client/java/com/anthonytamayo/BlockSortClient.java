package com.anthonytamayo;

import com.anthonytamayo.event.KeyInputHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.anthonytamayo.Cluster.groupColors;

@Environment(EnvType.CLIENT)
public class BlockSortClient implements ClientModInitializer {
    public static net.minecraft.client.MinecraftClient client;
    boolean scanned = false;
    private static double minRoughness = Double.MAX_VALUE;
    private static double maxRoughness = Double.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        registerKeyInputHandler();
        registerClientPlayConnectionEvents();
        refreshColors("colors.json");
    }

    private void registerKeyInputHandler() {
        KeyInputHandler.register(this);
    }

    private void registerClientPlayConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, _client) -> {
            client = _client;
            if (scanned) return;
            loadAndProcessBlockColors();
            refreshColors("colors.json");
        });
    }

    private void loadAndProcessBlockColors() {
        try {
            BlockDetailStorage[] loadedBlockColorArray = new Gson().fromJson(FileUtils.readJson("./blocksort/colors.json"), BlockDetailStorage[].class);
            if (loadedBlockColorArray != null) {
                updateBlockSpriteDetails(loadedBlockColorArray);
                scanned = true;
            }
        } catch (Exception e) {
            // Handle exception (log or specific handling)
        }
    }

    private void updateBlockSpriteDetails(BlockDetailStorage[] loadedBlockColorArray) {
        Registries.BLOCK.stream()
                .forEach(block -> Arrays.stream(loadedBlockColorArray)
                        .filter(storage -> storage.block.equals(block.asItem().getTranslationKey()))
                        .findFirst()
                        .ifPresent(storage -> updateBlockSpriteDetailsForBlock(block, storage)));
    }

    private void updateBlockSpriteDetailsForBlock(Block block, BlockDetailStorage storage) {
        if (block instanceof IItemBlockColorSaver) {
            IItemBlockColorSaver itemBlockColorSaver = (IItemBlockColorSaver) block.asItem();
            storage.spriteDetails.forEach(itemBlockColorSaver::blocksort_main$addSpriteDetails);
        }
    }

    public static void refreshColors(String fileName) {
        //long startTime = System.nanoTime();
        if (client == null) {
            // Log an error or throw an exception here
            return;
        }
        Sprite defaultSprite = getDefaultSprite();
        var atlas = defaultSprite.getAtlasId();
        int textureGlId = client.getTextureManager().getTexture(atlas).getGlId();
        RenderSystem.bindTexture(textureGlId);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int pixelSize = 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * pixelSize);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        ObjectMapper objectMapper = new ObjectMapper();
        File outputFile = new File("./blocksort/" + fileName);
        try {
            objectMapper.writeValue(outputFile,
                    Registries.BLOCK.stream().parallel()
                            .map(block -> processBlock(block, buffer, width))
                            .collect(Collectors.toList())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        //long endTime = System.nanoTime();
        //long duration = endTime - startTime;
        //System.out.println("refreshColors took " + (duration / 1e6) + " milliseconds.");
    }

    public static BlockDetailStorage processBlock(Block block, ByteBuffer originalPixelsBuffer, int width) {
        if (Objects.equals(block.asItem().getTranslationKey(), Items.AIR.getTranslationKey())) {
            return null;
        }
        ByteBuffer pixelsBuffer = originalPixelsBuffer.duplicate();
        BlockDetailStorage storage = new BlockDetailStorage();
        Set<Sprite> sprites = new HashSet<>(getSprites(block));
        sprites.forEach(sprite -> processSprite(storage, block, sprite, pixelsBuffer, width));
        storage.block = block.asItem().getTranslationKey();
        synchronized (storage.spriteDetails) {
            storage.spriteDetails.forEach(details -> ((IItemBlockColorSaver) block.asItem()).blocksort_main$addSpriteDetails(details));
        }
        return storage;
    }

    public static Sprite getDefaultSprite() {
        return client.getBakedModelManager()
                .getBlockModels()
                .getModel(Blocks.STONE.getDefaultState())
                .getQuads(Blocks.STONE.getDefaultState(), Direction.SOUTH, net.minecraft.util.math.random.Random.create())
                .get(0)
                .getSprite();
    }

    private static void processSprite(BlockDetailStorage storage, Block block, Sprite sprite, ByteBuffer pixelsBuffer, int width) {
        ArrayList<Vector3i> rgbList = extractRGBList(sprite, pixelsBuffer, width);
        double roughness = calculateRoughness(sprite, pixelsBuffer, width);
        SpriteDetails spriteDetails = createSpriteDetails(sprite);
        spriteDetails.roughness = roughness;
        Set<ColorGroup> colorGroups = groupColors(rgbList);
        colorGroups.forEach(cg -> spriteDetails.colorinfo.add(cg.meanColor));
        storage.block = block.asItem().getTranslationKey();
        storage.spriteDetails.add(spriteDetails);

    }

    private static ArrayList<Vector3i> extractRGBList(Sprite sprite, ByteBuffer pixelsBuffer, int width) {
        int spriteX = sprite.getX();
        int spriteY = sprite.getY();
        int spriteW = sprite.getContents().getWidth();
        int spriteH = sprite.getContents().getHeight();
        int firstPixel = (spriteY * width + spriteX) * 4;
        ArrayList<Vector3i> rgbList = new ArrayList<>();
        pixelsBuffer.rewind();
        for (int row = 0; row < spriteH; row++) {
            int firstInRow = firstPixel + row * width * 4;

            // Adjust the buffer's position to the start of the current row
            pixelsBuffer.position(firstInRow);

            for (int col = 0; col < spriteW; col++) {
                // Read one pixel's worth of data
                int blue = pixelsBuffer.get() & 0xFF;
                int green = pixelsBuffer.get() & 0xFF;
                int red = pixelsBuffer.get() & 0xFF;
                int alpha = pixelsBuffer.get() & 0xFF;

                int pixelColor = ColorHelper.Argb.getArgb(alpha, red, green, blue);
                rgbList.add(new Vector3i(ColorHelper.Argb.getRed(pixelColor), ColorHelper.Argb.getGreen(pixelColor), ColorHelper.Argb.getBlue(pixelColor)));
            }
        }
        pixelsBuffer.rewind();
        return rgbList;
    }

    private static SpriteDetails createSpriteDetails(Sprite sprite) {
        SpriteDetails spriteDetails = new SpriteDetails();
        String[] namesplit = sprite.getContents().getId().toString().split("/");
        spriteDetails.name = namesplit[namesplit.length - 1];
        return spriteDetails;
    }

    private static ArrayList<Sprite> getSprites(Block block) {
        ArrayList<Sprite> sprites = new ArrayList<>();
        var modelManager = client.getBakedModelManager();
        var blockModels = modelManager.getBlockModels();
        block.getStateManager().getStates().forEach(state -> {
            try {
                var model = blockModels.getModel(state);
                for (Direction direction : Direction.values()) {
                    sprites.add(getSprite(model, state, direction));
                }
            } catch (Exception e) {
                // Handle exception
            }
        });
        return sprites;
    }

    private static Sprite getSprite(BakedModel model, BlockState state, Direction direction) {
        return model.getQuads(state, direction, Random.create()).get(0).getSprite();
    }

    private static double calculateRoughness(Sprite sprite, ByteBuffer pixels, int width) {
        int spriteX = sprite.getX();
        int spriteY = sprite.getY();
        int spriteW = sprite.getContents().getWidth();
        int spriteH = sprite.getContents().getHeight();
        int firstPixel = (spriteY * width + spriteX) * 4;
        ArrayList<Vector3i> rgbList = new ArrayList<>();
        for (int row = 0; row < spriteH; row++) {
            int firstInRow = firstPixel + row * width * 4; // This remains unchanged
            pixels.position(firstInRow);
            for (int col = 0; col < spriteW; col++) {
                int red = pixels.get() & 0xFF;
                int green = pixels.get() & 0xFF;
                int blue = pixels.get() & 0xFF;
                pixels.get(); // Skip alpha byte but advance the position

                rgbList.add(new Vector3i(red, green, blue));
            }
        }
        double mean = rgbList.stream().mapToInt(rgb -> (rgb.x + rgb.y + rgb.z) / 3).average().orElse(0.0);
        double variance = rgbList.stream().mapToDouble(rgb -> {
            int intensity = (rgb.x + rgb.y + rgb.z) / 3;
            return Math.pow(intensity - mean, 2);
        }).average().orElse(0.0);
        double roughness = Math.sqrt(variance);
        return normalizeRoughness(roughness);
    }

    private static double normalizeRoughness(double roughness) {
        if (roughness < minRoughness) {
            minRoughness = roughness;
        }
        if (roughness > maxRoughness) {
            maxRoughness = roughness;
        }
        double normalizedRoughness = 0.0;
        if (maxRoughness != minRoughness) {
            normalizedRoughness = (roughness - minRoughness) / (maxRoughness - minRoughness) * 100;
        }
        return normalizedRoughness;
    }
}
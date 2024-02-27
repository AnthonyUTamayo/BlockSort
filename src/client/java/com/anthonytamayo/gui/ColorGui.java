package com.anthonytamayo.gui;

import com.anthonytamayo.BlockSortClient;
import com.anthonytamayo.IItemBlockColorSaver;
import hsluv.HUSLColorConverter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class ColorGui extends LightweightGuiDescription {
    final int gridWidth = 21;
    final int gridHeight = 13;
    int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
    int GRID_HEIGHT = guiScale != 0 ? (gridHeight * 4) / guiScale : gridHeight;
    int GRID_WIDTH = guiScale != 0 ? (gridWidth * 4) / guiScale : gridWidth;
    final WButton refreshButton = createButton();
    final WGridPanel root = new WGridPanel();
    final WScrollBar scrollBar = createScrollBar();
    final ArrayList<ItemStack> stacks = new ArrayList<>();
    final ArrayList<Slot> slots = new ArrayList<>();
    static MinecraftClient client;
    BlockSortClient blocksortClient;
    private String filterName = "";
    final WSlider MinSaturationSlider = new WSlider(0, 500, Axis.VERTICAL);
    final WSlider MaxSaturationSlider = new WSlider(0, 500, Axis.VERTICAL);
    final WSlider MinLuminanceSlider = new WSlider(0, 500, Axis.VERTICAL);
    final WSlider MaxLuminanceSlider = new WSlider(0, 500, Axis.VERTICAL);
    final WSlider MinRoughnessSlider = new WSlider(0, 500, Axis.HORIZONTAL);
    final WSlider MaxRoughnessSlider = new WSlider(0, 500, Axis.HORIZONTAL);
    private float MIN_SATURATION_THRESHOLD = 0.0f;
    private float MAX_SATURATION_THRESHOLD = 100.0f;
    private float MIN_LUMINANCE_THRESHOLD = 0.0f;
    private float MAX_LUMINANCE_THRESHOLD = 100.0f;
    private float MIN_ROUGHNESS_THRESHOLD = 0.0f;
    private float MAX_ROUGHNESS_THRESHOLD = 100.0f;
    private long lastRunTime = 0;
    private static final long DELAY_MS = 25; // 100 ms delay between updates

    @Override
    public TriState isDarkMode() {
        return TriState.TRUE;
    }

    @Override
    public void addPainters() {
    }

    @Environment(EnvType.CLIENT)
    private WScrollBar createScrollBar() {
        return new WScrollBar(Axis.VERTICAL) {
            @Override
            public InputResult onMouseScroll(int x, int y, double horz, double vert) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRunTime > DELAY_MS) {
                    placeSlots(); // Call the method directly if enough time has passed
                    lastRunTime = currentTime;
                }
                return super.onMouseScroll(x, y, horz, vert);
            }

            @Override
            public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRunTime > DELAY_MS) {
                    placeSlots(); // Call the method directly if enough time has passed
                    lastRunTime = currentTime;
                }
                return super.onMouseDrag(x, y, button, deltaX, deltaY);
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private WButton createButton() {
        return new WButton() {
            @Override
            public void addTooltip(TooltipBuilder tooltip) {
                if (tooltip != null) {
                    tooltip.add(Text.translatable("ui.blocksort.refresh_info"));
                }
                super.addTooltip(tooltip);
            }
        };
    }

    @Environment(value = EnvType.CLIENT)
    public ColorGui(MinecraftClient client, BlockSortClient blocksortClient) {
        if (client == null || blocksortClient == null) {
            throw new IllegalArgumentException("client and BlockSortClient cannot be null");
        }

        MinSaturationSlider.setValue((int) (MIN_SATURATION_THRESHOLD * 100));
        MaxSaturationSlider.setValue((int) (MAX_SATURATION_THRESHOLD * 100));
        MinLuminanceSlider.setValue((int) (MIN_LUMINANCE_THRESHOLD * 100));
        MaxLuminanceSlider.setValue((int) (MAX_LUMINANCE_THRESHOLD * 100));
        MinRoughnessSlider.setValue((int) (MIN_ROUGHNESS_THRESHOLD * 100));
        MaxRoughnessSlider.setValue((int) (MAX_ROUGHNESS_THRESHOLD * 100));

        ColorGui.client = client;
        this.blocksortClient = blocksortClient;
        setupRootPanel();
        colorSort();
        root.validate(this);
    }

    private void updateScrollAmount() {
        int scrollAmount = stacks.size() / GRID_WIDTH;
        if (stacks.size() % GRID_WIDTH > 0) {
            scrollAmount += 4;
        }

        switch (guiScale) {
            case 4:
            case 0:
                scrollBar.setMaxValue(scrollAmount);
                break;
            case 3:
                scrollBar.setMaxValue(scrollAmount - 4);
                break;
            case 2:
                scrollBar.setMaxValue(scrollAmount - 13);
                break;
            case 1:
                scrollBar.setMaxValue(scrollAmount - 39);
                break;
        }
    }

    public void sliderAdjust(char d, int value) {
        long currentTime = System.currentTimeMillis();
        float newValue = value / 5.0f;

        switch (d) {
            case 'a':
                MIN_SATURATION_THRESHOLD = newValue;
                break;
            case 's':
                MAX_SATURATION_THRESHOLD = newValue;
                break;
            case 'd':
                MIN_LUMINANCE_THRESHOLD = newValue;
                break;
            case 'f':
                MAX_LUMINANCE_THRESHOLD = newValue;
                break;
            case 'g':
                MIN_ROUGHNESS_THRESHOLD = newValue;
                break;
            case 'h':
                MAX_ROUGHNESS_THRESHOLD = newValue;
                break;
        }

        if (currentTime - lastRunTime > DELAY_MS) {
            colorSort();
            lastRunTime = currentTime;
        }
    }

    public void sliderDragFinish(char d, int value) {
        float newValue = value / 5.0f; // Convert the range back to 0.0 - 100.0 from 500.0

        // Set the new value based on the slider adjusted
        switch (d) {
            case 'a':
                MIN_SATURATION_THRESHOLD = newValue;
                break;
            case 's':
                MAX_SATURATION_THRESHOLD = newValue;
                break;
            case 'd':
                MIN_LUMINANCE_THRESHOLD = newValue;
                break;
            case 'f':
                MAX_LUMINANCE_THRESHOLD = newValue;
                break;
            case 'g':
                MIN_ROUGHNESS_THRESHOLD = newValue;
                break;
            case 'h':
                MAX_ROUGHNESS_THRESHOLD = newValue;
                break;
        }
        colorSort();
    }

    private void setupRootPanel() {
        setRootPanel(root);
        root.setInsets(new Insets(3, 6, 3, 3));
        root.add(scrollBar, GRID_WIDTH, 0, 1, GRID_HEIGHT);

        // Create and configure the text field for filter name input
        WTextField filterNameField = new WTextField(Text.of(filterName));
        filterNameField.setChangedListener(this::onFilterNameChanged);
        root.add(filterNameField, GRID_WIDTH + 1, 4, 4, 1);

        // Add Saturation and Luminance labels
        root.add(new WLabel(Text.of("    S")), GRID_WIDTH + 1, 6);
        root.add(new WLabel(Text.of("    L")), GRID_WIDTH + 3, 6);
        root.add(new WLabel(Text.of("   Roughness")), GRID_WIDTH + 1, 0);


        root.add(MinSaturationSlider, GRID_WIDTH + 1, 6, 1, GRID_HEIGHT - 8);
        MinSaturationSlider.setValueChangeListener((int value) -> sliderAdjust('a', value));
        MinSaturationSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('a', value));

        root.add(MaxSaturationSlider, GRID_WIDTH + 2, 6, 1, GRID_HEIGHT - 8);
        MaxSaturationSlider.setValueChangeListener((int value) -> sliderAdjust('s', value));
        MaxSaturationSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('s', value));

        root.add(MinLuminanceSlider, GRID_WIDTH + 3, 6, 1, GRID_HEIGHT - 8);
        MinLuminanceSlider.setValueChangeListener((int value) -> sliderAdjust('d', value));
        MinLuminanceSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('d', value));

        root.add(MaxLuminanceSlider, GRID_WIDTH + 4, 6, 1, GRID_HEIGHT - 8);
        MaxLuminanceSlider.setValueChangeListener((int value) -> sliderAdjust('f', value));
        MaxLuminanceSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('f', value));

        root.add(MinRoughnessSlider, GRID_WIDTH + 1, 1, 4, 1);
        MinRoughnessSlider.setValueChangeListener((int value) -> sliderAdjust('g', value));
        MinRoughnessSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('g', value));

        root.add(MaxRoughnessSlider, GRID_WIDTH + 1, 2, 4, 1);
        MaxRoughnessSlider.setValueChangeListener((int value) -> sliderAdjust('h', value));
        MaxRoughnessSlider.setDraggingFinishedListener((int value) -> sliderDragFinish('h', value));

        setupButton(refreshButton, this::refreshAndSortColors);
    }

    private void refreshAndSortColors() {
        BlockSortClient.refreshColors("colors.json");
        colorSort();
    }

    private void setupButton(WButton button, Runnable onClick) {
        root.add(refreshButton, GRID_WIDTH + 1, GRID_HEIGHT - 1, 4, 1);
        button.setOnClick(onClick);
    }

    public void colorSort() {
        Map<String, Vector3i> colorMap = initializeColorMap();
        stacks.clear();

        Registries.BLOCK.stream()
                .filter(this::filterByBlockName)
                .forEach(block -> processItemBlock(block, (IItemBlockColorSaver) block.asItem(), colorMap));

        stacks.sort(Comparator.comparingDouble(stack -> ((IItemBlockColorSaver) stack.getItem()).blocksort_main$getScore()));

        placeSlots();
    }

    private Map<String, Vector3i> initializeColorMap() {
        Map<String, Vector3i> colorMap = new HashMap<>();
        colorMap.put("compare", new Vector3i(0, 0, 0));
        return colorMap;
    }

    public void placeSlots() {
        removeSlotsFromRoot();
        int startIndex = calculateStartIndex();
        placeSlotsInGrid(startIndex);
        root.validate(this);
    }

    private void removeSlotsFromRoot() {
        slots.forEach(root::remove);
    }

    private int calculateStartIndex() {
        updateScrollAmount();
        int index = GRID_WIDTH * scrollBar.getValue();
        return Math.max(index, 0);
    }

    private void placeSlotsInGrid(int startIndex) {
        for (int row = 0; row < GRID_HEIGHT && startIndex < stacks.size(); row++) {
            for (int col = 0; col < GRID_WIDTH && startIndex < stacks.size(); col++) {
                Slot colorGuiSlot = createOrGetSlot(startIndex);
                root.add(colorGuiSlot, col, row);
                startIndex++;
            }
        }
    }

    private Slot createOrGetSlot(int index) {
        Slot colorGuiSlot = new Slot(stacks.get(index));
        if (slots.size() <= index) {
            slots.add(colorGuiSlot);
        } else {
            slots.set(index, colorGuiSlot);
        }
        return colorGuiSlot;
    }

    private boolean filterByBlockName(Block block) {
        String translationKey = block.getTranslationKey().toLowerCase();
        String[] parts = translationKey.split("\\.");
        if (parts.length > 0) {
            String blockName = parts[parts.length - 1];
            return blockName.contains(filterName.toLowerCase());
        }
        return false;
    }

    private void onFilterNameChanged(String newFilterName) {
        filterName = newFilterName;
        colorSort();
    }

    private void processItemBlock(Block block, IItemBlockColorSaver itemBlock, Map<String, Vector3i> colorMap) {
        AtomicReference<Double> minColorDifference = new AtomicReference<>(Double.MAX_VALUE);
        colorMap.values().forEach(colorToCompare ->
                processColors(itemBlock, colorToCompare, minColorDifference)
        );
        updateBlockScoreAndStacks(block, itemBlock, minColorDifference.get());
    }

    private void processColors(IItemBlockColorSaver itemBlock, Vector3i colorToCompare, AtomicReference<Double> minColorDifference) {
        double[] hsluvColorToCompare = convertToHSLuv(colorToCompare);
        double minRoughness = Math.min(MIN_ROUGHNESS_THRESHOLD, MAX_ROUGHNESS_THRESHOLD);
        double maxRoughness = Math.max(MIN_ROUGHNESS_THRESHOLD, MAX_ROUGHNESS_THRESHOLD);

        IntStream.range(0, itemBlock.blocksort_main$getLength())
                .mapToObj(itemBlock::blocksort_main$getSpriteDetails) // Convert each index to SpriteDetails
                .filter(sprite -> Double.isFinite(sprite.roughness) && sprite.roughness >= minRoughness && sprite.roughness <= maxRoughness) // Filter by roughness
                .flatMap(sprite -> sprite.colorinfo.stream()) // Flatten the stream of color information
                .filter(this::isColorWithinThreshold) // Filter colors within threshold
                .forEach(spriteColor -> updateMinColorDifference(hsluvColorToCompare, spriteColor, minColorDifference)); // Process each color
    }

    private double[] convertToHSLuv(Vector3i color) {
        return HUSLColorConverter.rgbToHsluv(new double[]{
                color.x / 255.0, color.y / 255.0, color.z / 255.0
        });
    }

    private boolean isColorWithinThreshold(Vector3i spriteColor) {
        double[] hsluvSpriteColor = convertToHSLuv(spriteColor);
        double minS = Math.min(MIN_SATURATION_THRESHOLD, MAX_SATURATION_THRESHOLD + 2);
        double maxS = Math.max(MIN_SATURATION_THRESHOLD, MAX_SATURATION_THRESHOLD + 2);
        double minL = Math.min(MIN_LUMINANCE_THRESHOLD, MAX_LUMINANCE_THRESHOLD + 2);
        double maxL = Math.max(MIN_LUMINANCE_THRESHOLD, MAX_LUMINANCE_THRESHOLD + 2);
        boolean isSaturationWithinRange = hsluvSpriteColor[1] >= minS && hsluvSpriteColor[1] <= maxS;
        boolean isLuminanceWithinRange = hsluvSpriteColor[2] >= minL && hsluvSpriteColor[2] <= maxL;
        return isSaturationWithinRange && isLuminanceWithinRange;
    }

    private void updateMinColorDifference(double[] hsluvColorToCompare, Vector3i spriteColor, AtomicReference<Double> minColorDifference) {
        double[] hsluvSpriteColor = convertToHSLuv(spriteColor);
        double colorDifference = Math.abs(hsluvColorToCompare[0] - hsluvSpriteColor[0]);
        if (colorDifference < minColorDifference.get()) {
            minColorDifference.set(colorDifference);
        }
    }

    private void updateBlockScoreAndStacks(Block block, IItemBlockColorSaver itemBlock, double minColorDifference) {
        if (minColorDifference != Double.MAX_VALUE) {
            itemBlock.blocksort_main$setScore(minColorDifference);
            if (itemBlock.blocksort_main$getLength() > 0 && client.world != null ) {
                stacks.add(new ItemStack(block));
            }
        }
    }
}
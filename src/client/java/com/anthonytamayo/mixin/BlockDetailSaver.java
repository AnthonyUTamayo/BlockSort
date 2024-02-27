package com.anthonytamayo.mixin;

import com.anthonytamayo.IItemBlockColorSaver;
import com.anthonytamayo.SpriteDetails;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedList;

//Mixin, which adds one string to the Item class, and getter and setter functions. String color stores the hexcode value of the block.
@Mixin(Item.class)
public abstract class BlockDetailSaver implements IItemBlockColorSaver {
    @Unique
    private final LinkedList<SpriteDetails> spriteDetails = new LinkedList<>();

    @Unique
    private double score = 0;

    @Override
    public SpriteDetails blocksort_main$getSpriteDetails(int i) {
        while (i >= this.spriteDetails.size()) {
            this.spriteDetails.add(new SpriteDetails());
        }
        return spriteDetails.get(i);
    }

    @Override
    public void blocksort_main$addSpriteDetails(SpriteDetails spriteDetails) {
        this.spriteDetails.add(spriteDetails);
    }

    @Override
    public void blocksort_main$clearSpriteDetails() {
        this.spriteDetails.clear();
    }

    @Override
    public int blocksort_main$getLength() {
        return this.spriteDetails.size();
    }

    @Override
    public double blocksort_main$getScore() {
        return this.score;
    }

    @Override
    public void blocksort_main$setScore(double score) {
        this.score = score;
    }
}

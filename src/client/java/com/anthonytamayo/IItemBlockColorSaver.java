package com.anthonytamayo;

//Definition of IItemBlockColorSaver class. See BlockDetailSaver for implementation.
public interface IItemBlockColorSaver {
    SpriteDetails blocksort_main$getSpriteDetails(int i);

    void blocksort_main$addSpriteDetails(SpriteDetails spriteDetails);

    int blocksort_main$getLength();

    void blocksort_main$clearSpriteDetails();

    double blocksort_main$getScore();

    void blocksort_main$setScore(double score);
}

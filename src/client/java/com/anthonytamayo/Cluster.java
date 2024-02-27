package com.anthonytamayo;

import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Cluster {

    private static void mergeClusters(Set<ColorGroup> groups, ColorGroup group1, ColorGroup group2) {
        // Add all pixels from group2 to group1 and update the mean color and weight incrementally.
        for (Vector3i pixel : group2.pixels) {
            group1.pixels.add(pixel);
            group1.meanColor = group1.pixels.size() > 1
                    ? group1.meanColor.mul(group1.pixels.size() - 1).add(pixel).div(group1.pixels.size())
                    : pixel; // Assign directly if it's the first pixel
        }

        // Remove group2 from the set of groups.
        groups.remove(group2);
    }

    private static ColorGroup[] findClosestPair(Set<ColorGroup> groups) {
        validateInputSet(groups);

        Iterator<ColorGroup> iterator = groups.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Input set cannot be empty");
        }

        ColorGroup closestFirst = iterator.next();
        ColorGroup closestSecond = null; // No need for initial assignment
        double minDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            ColorGroup current = iterator.next();
            double distance = closestFirst.meanColor.distance(current.meanColor);

            if (distance < minDistance) {
                minDistance = distance;
                closestSecond = current;
            }
        }

        return new ColorGroup[]{closestFirst, closestSecond};
    }

    private static void validateInputSet(Set<ColorGroup> groups) {
        if (groups.size() < 2) {
            throw new IllegalArgumentException("Input set must contain at least two elements.");
        }
    }

    public static Set<ColorGroup> groupColors(ArrayList<Vector3i> rgbList) {
        // Initialize: Start with each data point in its own cluster.
        Set<ColorGroup> groups = new LinkedHashSet<>();
        for (Vector3i pixel : rgbList) {
            ColorGroup group = new ColorGroup();
            group.pixels.add(pixel);
            calculateGroupProperties(group);  // Add this line
            groups.add(group);
        }

        // Repeat until only one cluster remains.
        while (groups.size() > 1) {
            // Find the closest pair: Use a k-nearest neighbor join closest-pair ranking algorithm to find the pair of clusters that are closest to each other.
            ColorGroup[] closestPair = findClosestPair(groups);

            // Merge clusters: Combine the two closest clusters into a single cluster.
            mergeClusters(groups, closestPair[0], closestPair[1]);
        }

        // Calculate group properties for the final cluster.
        for (ColorGroup group : groups) {
            calculateGroupProperties(group);
        }

        return groups;
    }

    private static void calculateGroupProperties(ColorGroup group) {
        Vector3i sum = new Vector3i(0, 0, 0);

        for (Vector3i color : group.pixels) {
            sum.add(color);
        }

        group.meanColor = sum.div(group.pixels.size());
    }
}

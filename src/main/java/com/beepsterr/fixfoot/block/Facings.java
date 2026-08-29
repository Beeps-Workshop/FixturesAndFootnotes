package com.beepsterr.fixfoot.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Quarter-turn helpers for the drainage blocks, whose models are all authored facing north and
 * turned to their facing by the generated blockstate. Their collision shapes have to make the same
 * turn, and a {@code VoxelShape} cannot be rotated by the blockstate.
 */
final class Facings {
    private Facings() {
    }

    /** Takes a shape from the authored north-facing frame into a block facing {@code facing}. */
    static VoxelShape turn(VoxelShape shape, Direction facing) {
        return turn(shape, quarters(facing));
    }

    /** As above, but by a count — for shapes turned independently of the block's facing. */
    static VoxelShape turn(VoxelShape shape, int quarters) {
        for (int i = Math.floorMod(quarters, 4); i > 0; i--) {
            shape = rotateClockwise(shape);
        }
        return shape;
    }

    /** Quarter turns clockwise from north, the same count the blockstate rotates a model by. */
    static int quarters(Direction facing) {
        int turns = 0;
        for (Direction from = Direction.NORTH; from != facing; from = from.getClockWise()) {
            turns++;
        }
        return turns;
    }

    /** Takes a direction from the authored north-facing frame into a block facing {@code facing}. */
    static Direction turn(Direction base, Direction facing) {
        Direction turned = base;
        for (Direction from = Direction.NORTH; from != facing; from = from.getClockWise()) {
            turned = turned.getClockWise();
        }
        return turned;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        List<VoxelShape> boxes = new ArrayList<>();
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> boxes.add(Shapes.box(1.0 - z2, y1, x1, 1.0 - z1, y2, x2)));
        return boxes.stream().reduce(Shapes::or).orElse(Shapes.empty());
    }
}

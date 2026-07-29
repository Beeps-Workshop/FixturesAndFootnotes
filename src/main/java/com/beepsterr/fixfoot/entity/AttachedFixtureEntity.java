package com.beepsterr.fixfoot.entity;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared base for the mod's item-frame-style fixtures — planters, shelves and storage nets that are
 * right-clicked onto a block face and then hang off it as an entity instead of occupying a block.
 *
 * <p>Two things separate these from a vanilla {@link HangingEntity}:
 * <ul>
 *   <li>{@link #survives()} accepts any non-air support instead of demanding a solid face, so a
 *       fixture can hang on slat panels, trellises, fences and other partial blocks.</li>
 *   <li>{@link #canAttachTo(Direction)} lets each fixture choose its faces. The default is
 *       wall-mounted; ceiling- or floor-mounted fixtures override it to allow the vertical axis.</li>
 * </ul>
 *
 * <p>VERSION SEAM: written against the 1.21.1 API. In 1.21.2+ {@code dropItem} gains a ServerLevel
 * parameter and {@code spawnAtLocation} moved to server-level overloads, so a 1.21.4 port needs
 * Stonecutter conditionals on {@link #dropItem} and {@link #dropStack}.
 */
public abstract class AttachedFixtureEntity extends HangingEntity {
    /**
     * How far the support's own geometry falls short of its block boundary, so the fixture can be
     * pulled in to touch it. Cached from the last {@link #faceHuggingBox} call because the renderer
     * needs the same number to keep the model on the hitbox.
     */
    private double surfaceGap;

    /** Counts down to the next re-measure; starts at 0 so the first tick always takes one. */
    private int remeasureTimer;

    protected AttachedFixtureEntity(EntityType<? extends AttachedFixtureEntity> type, Level level) {
        super(type, level);
    }

    protected AttachedFixtureEntity(EntityType<? extends AttachedFixtureEntity> type, Level level,
                                    BlockPos pos, Direction facing) {
        super(type, level, pos);
        setDirection(facing);
    }

    // --- Attachment ---

    /**
     * Whether this fixture may hang off the given block face. {@code facing} points away from the
     * support block, so the four horizontal values are wall mounts, DOWN is a ceiling mount and UP
     * is a floor mount.
     */
    public boolean canAttachTo(Direction facing) {
        return facing.getAxis().isHorizontal();
    }

    /**
     * Vanilla {@link HangingEntity#setDirection} asserts a horizontal facing and throws otherwise —
     * from the constructor, so it fires before {@link #canAttachTo} can refuse the face. Overridden
     * for the same reason ItemFrame overrides it: branch on the axis and drive xRot rather than yRot
     * for ceiling and floor mounts. Without this, vertical faces aren't merely rejected, they crash.
     */
    @Override
    protected void setDirection(Direction facing) {
        this.direction = Objects.requireNonNull(facing);
        if (facing.getAxis().isHorizontal()) {
            setXRot(0.0F);
            setYRot(facing.get2DDataValue() * 90.0F);
        } else {
            setXRot(-90.0F * facing.getAxisDirection().getStep());
            setYRot(0.0F);
        }
        this.xRotO = getXRot();
        this.yRotO = getYRot();
        recalculateBoundingBox();
    }

    @Override
    public boolean survives() {
        if (!level().noCollision(this) || !canAttachTo(direction)) {
            return false;
        }
        BlockState support = level().getBlockState(pos.relative(direction.getOpposite()));
        // Deliberately looser than vanilla, which requires state.isSolid(). Fixtures are meant to
        // hang off slats, trellises and fences, so anything that isn't air or a replaceable plant
        // counts as something to mount on.
        if (support.isAir() || support.canBeReplaced()) {
            return false;
        }
        return level().getEntities(this, getBoundingBox(), HANGING_ENTITY).isEmpty();
    }

    /**
     * Box for a fixture that hugs the face it hangs on: {@code width} across that face, {@code
     * height} tall and {@code depth} standing out of it. Works for all six faces — for a ceiling or
     * floor mount the depth runs along Y instead.
     *
     * <p>The box is pulled in to the support's actual outline shape rather than its block boundary,
     * so hanging one on a slat panel or a fence leaves no gap.
     */
    protected AABB faceHuggingBox(BlockPos attachPos, Direction facing, double width, double height, double depth) {
        this.surfaceGap = measureSurfaceGap(attachPos, facing);
        // Push the box back against the support instead of leaving it centred in the block.
        Vec3 centre = Vec3.atCenterOf(attachPos).relative(facing, -(0.5D - depth / 2.0D) - surfaceGap);
        double sizeX = facing.getAxis() == Direction.Axis.X ? depth : width;
        double sizeY = facing.getAxis() == Direction.Axis.Y ? depth : height;
        double sizeZ = facing.getAxis() == Direction.Axis.Z ? depth : width;
        return AABB.ofSize(centre, sizeX, sizeY, sizeZ);
    }

    /**
     * The empty space between the support's block boundary and the surface the fixture should rest
     * on. A full block gives 0; a slat panel hugging the wall behind it gives most of a block.
     *
     * <p>Measured against the outline shape — the box the player actually sees highlighted — so
     * what you aim at is what the fixture lands on. Because the fixture is moved to exactly touch
     * that surface it never overlaps it, and {@link #survives()}'s collision check still passes.
     */
    private double measureSurfaceGap(BlockPos attachPos, Direction facing) {
        BlockPos supportPos = attachPos.relative(facing.getOpposite());
        VoxelShape shape = level().getBlockState(supportPos).getShape(level(), supportPos);
        if (shape.isEmpty()) {
            return 0.0D;
        }
        Direction.Axis axis = facing.getAxis();
        // How far the support reaches towards us, measured inside its own block.
        double reach = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? shape.max(axis)
                : 1.0D - shape.min(axis);
        return Math.max(0.0D, 1.0D - reach);
    }

    /**
     * Where the fixture's model origin sits: the centre of the block it hangs in, pulled in by the
     * same gap as the hitbox. Renderers anchor to this so the art tracks the box on partial blocks.
     */
    public Vec3 getModelAnchor() {
        return Vec3.atCenterOf(pos).relative(direction, -surfaceGap);
    }

    @Override
    public Vec3 getLightProbePosition(float partialTick) {
        // Entity's default probes the eye position — position plus eye height — which for something
        // hugging its support lands inside the support block and samples zero light. A ceiling net
        // renders solid black that way. Sample the open block the fixture hangs in instead.
        return Vec3.atCenterOf(pos);
    }

    @Override
    public void tick() {
        // Two reasons to re-measure rather than trust the gap taken at placement: on world load the
        // entity can deserialise before the support's chunk exists (measuring against air), and a
        // support can change shape underneath us — slats gaining vines, a fence growing an arm.
        // Own counter because BlockAttachedEntity#tick never calls baseTick, so tickCount is frozen.
        if (direction != null && remeasureTimer-- <= 0) {
            remeasureTimer = 100;
            recalculateBoundingBox();
        }
        super.tick();
    }

    // --- Breaking ---

    /** The item that places this fixture, and the one it drops when broken. */
    public abstract ItemStack getFixtureItem();

    /** Anything the fixture is holding (displayed plants, stored stacks) that spills when it breaks. */
    protected void dropContents() {
    }

    protected SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    protected void dropStack(ItemStack stack) {
        if (!stack.isEmpty()) {
            spawnAtLocation(stack);
        }
    }

    @Override
    public void dropItem(Entity breaker) {
        //? if >=1.21.2 {
        /*if (!(level() instanceof ServerLevel server)
                || !server.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        *///?} else {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        //?}
        playSound(getBreakSound(), 1.0F, 1.0F);
        // Vanilla item-frame rule: a creative player doesn't get the fixture itself back, but
        // whatever it was holding still drops.
        if (!(breaker instanceof Player player) || !player.getAbilities().instabuild) {
            dropStack(getFixtureItem());
        }
        dropContents();
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.ITEM_FRAME_PLACE, 1.0F, 1.0F);
    }

    // --- Persistence & client sync ---
    // HangingEntity tracks `direction` but doesn't save it; each subclass family does that itself.

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Facing", (byte) direction.get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDirection(Direction.from3DDataValue(tag.getByte("Facing")));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        // Spare int carries the facing, the way item frames and paintings do. The BlockPos matters
        // just as much: BlockAttachedEntity#setPos re-derives the attachment block from the entity
        // position, and pulling a fixture back onto its support's surface can drag its centre into
        // the support's own block. Let the client infer it and BlockPos.containing lands a block too
        // far back, re-measuring against whatever is behind the support. Paintings send it for the
        // same reason — they span blocks, so their centre isn't in their attachment block either.
        return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), pos);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }
}

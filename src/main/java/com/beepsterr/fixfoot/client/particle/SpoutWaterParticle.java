package com.beepsterr.fixfoot.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Vanilla's falling water, but keeping the velocity it was spawned with.
 *
 * <p>Vanilla's own {@code DripParticle} zeroes its velocity in its constructor, so every drip can
 * only fall straight down — no use for water leaving a spout with some speed behind it. Extending
 * it is not an option either: the constructor is package-private in the compiled class whatever the
 * decompiled source says, and widening access across two loaders is a poor trade for a handful of
 * lines. So this rebuilds it on the public base instead, and uses vanilla's sprite.
 */
public class SpoutWaterParticle extends TextureSheetParticle {
    /**
     * {@code Particle#tick} scales this by 0.04 before applying, so this lands on the same 0.06
     * per tick a vanilla drip falls at.
     */
    private static final float DRIP_GRAVITY = 1.5F;
    /** One landing in this many is audible, so a running spout patters rather than roars. */
    private static final int SOUND_CHANCE = 18;

    private SpoutWaterParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        setSize(0.01F, 0.01F);
        // The sprite is white; vanilla tints every water drip with exactly this, and without it
        // the drop comes out looking like milk.
        setColor(0.2F, 0.3F, 1.0F);
        this.gravity = DRIP_GRAVITY;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = (int) (64.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    /**
     * Lands the way a vanilla drip does: a splash where it hits, and now and then the sound of it.
     *
     * <p>Vanilla only makes the noise for dripstone, which drips slowly enough to get away with a
     * sound every time. A spout throws several drops a tick, so the same treatment would be a wall
     * of noise — hence the roll. The volume range is vanilla's.
     */
    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            remove();
            this.level.addParticle(ParticleTypes.SPLASH, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            if (this.random.nextInt(SOUND_CHANCE) == 0) {
                this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                        SoundSource.BLOCKS, Mth.randomBetween(this.random, 0.3F, 1.0F), 1.0F, false);
            }
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            SpoutWaterParticle particle = new SpoutWaterParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}

package com.hollowfrontier.client;

import com.hollowfrontier.content.frontier.weather.DustDevil;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A mote of a {@link DustDevil}'s dust (29). It goes round the devil's middle, fastest near the ground, and climbs as
 * it goes, held to the funnel's width at its height (each mote a little in or out of it), leaning with the kink that
 * travels up the column ({@link DustDevil#lean}). Near the top it spirals out and shrinks away. Motes laid down along a
 * band at the same height keep turning together, and the slower turn higher up winds each band into a spiral. If its
 * devil is gone it keeps its last middle, so the dust thins out where the devil died down. Drawn with vanilla's dust
 * sprites.
 */
public class DustDevilMote extends SingleQuadParticle {
    /** How fast it goes round at the foot and at the top, in radians a tick. */
    static final double SPIN_FOOT = 0.42, SPIN_TOP = 0.16;

    private final SpriteSet sprites;
    private final int devilId;
    private final float size;
    private final double rise, width;
    private double centreX, centreZ, baseY;
    private double angle, radius;

    DustDevilMote(ClientLevel level, double x, double y, double z, int devilId, ColorParticleOption colour, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.devilId = devilId;
        hasPhysics = false;
        gravity = 0.0F;
        xd = yd = zd = 0.0;
        float shade = 0.68F + random.nextFloat() * 0.3F; // a shade under the ground's own colour: dust in the air reads darker against the sky
        rCol = Mth.clamp(colour.getRed() * shade, 0.0F, 1.0F);
        gCol = Mth.clamp(colour.getGreen() * shade, 0.0F, 1.0F);
        bCol = Mth.clamp(colour.getBlue() * shade, 0.0F, 1.0F);
        size = 0.12F + random.nextFloat() * 0.16F;
        quadSize = size;
        rise = 0.05 + random.nextDouble() * 0.05;
        width = 0.55 + random.nextDouble() * 0.6;
        lifetime = 40 + random.nextInt(50);
        Entity devil = level.getEntity(devilId);
        baseY = devil != null ? devil.getY() : y;
        centreX = devil != null ? devil.getX() : x;
        centreZ = devil != null ? devil.getZ() : z;
        angle = Math.atan2(z - centreZ, x - centreX);
        radius = Math.hypot(x - centreX, z - centreZ);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        Entity devil = level.getEntity(devilId);
        if (devil instanceof DustDevil d && d.isAlive()) {
            centreX = d.getX();
            centreZ = d.getZ();
            baseY = d.getY();
        }
        double h = y - baseY, f = Mth.clamp(h / DustDevil.HEIGHT, 0.0, 1.0);
        angle += Mth.lerp(f, SPIN_FOOT, SPIN_TOP);
        if (h > DustDevil.HEIGHT * 0.85) {
            radius += 0.035; // out over the top
        } else {
            radius += (DustDevil.funnel(h) * width - radius) * 0.12;
        }
        Vec3 lean = DustDevil.lean(level.getGameTime(), h);
        double ny = y + rise * (h > DustDevil.HEIGHT * 0.85 ? 0.4 : 1.0);
        setPos(centreX + lean.x + Math.cos(angle) * radius, ny, centreZ + lean.z + Math.sin(angle) * radius);
        if (h > DustDevil.HEIGHT + 0.5) {
            lifetime = Math.min(lifetime, age + 6);
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (age + partialTick) / lifetime;
        return size * Mth.clamp(Math.min(life * 6.0F, (1.0F - life) * 4.0F), 0.0F, 1.0F);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption colour, ClientLevel level, double x, double y, double z, double devilId, double unusedY,
                double unusedZ, RandomSource random) {
            return new DustDevilMote(level, x, y, z, (int) devilId, colour, sprites);
        }
    }
}

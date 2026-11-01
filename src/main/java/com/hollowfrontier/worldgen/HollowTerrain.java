package com.hollowfrontier.worldgen;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Hollow Earth's shape, as density functions (positive = solid). Three parts, joined with max():
 * <ul>
 * <li><b>The land</b>: a height field in blocks ({@link #HEIGHT}) plus a little 3D noise for overhangs and cliffs. It
 *     follows the same climate noises the biomes are chosen from ({@link HollowBiomes}), so the terrain suits its biome:
 *     the Highlands rise where continentalness is high or it's cold, the Ember Badlands step up in mesa terraces where it's
 *     hot and dry, the Steppes lie flat and open, and rivers wind through everything but the Badlands.</li>
 * <li><b>The crust</b> overhead: its underside sways gently around {@link HollowEarth#CEILING_BASE}, with great stone spires
 *     hanging down out of it in clusters (some all the way to the hills, making pillars) and smaller teeth all over.</li>
 * <li><b>Floating islands</b>: rare flat-topped chunks of crust drifting a little under the ceiling, with rocky cones
 *     beneath.</li>
 * </ul>
 * Caves come from vanilla carvers (in the biomes), not from noise, which keeps the crust and islands solid.
 */
public final class HollowTerrain {
    private HollowTerrain() {}

    // ---------------------------------------------------------------- noises (each its own seed, so nothing lines up with the Overworld)

    public static final ResourceKey<NormalNoise> TEMPERATURE = noise("temperature");
    public static final ResourceKey<NormalNoise> HUMIDITY = noise("humidity");
    public static final ResourceKey<NormalNoise> CONTINENTS = noise("continents");
    public static final ResourceKey<NormalNoise> EROSION = noise("erosion");
    public static final ResourceKey<NormalNoise> RIDGES = noise("ridges");
    public static final ResourceKey<NormalNoise> RIVERS = noise("rivers");
    public static final ResourceKey<NormalNoise> CEILING = noise("ceiling");
    public static final ResourceKey<NormalNoise> SPIRES = noise("spires");
    public static final ResourceKey<NormalNoise> SPIRE_FIELDS = noise("spire_fields");
    public static final ResourceKey<NormalNoise> TEETH = noise("teeth");
    public static final ResourceKey<NormalNoise> ISLANDS = noise("islands");
    public static final ResourceKey<NormalNoise> ISLAND_FIELDS = noise("island_fields");

    // ---------------------------------------------------------------- named density functions

    public static final ResourceKey<DensityFunction> F_TEMPERATURE = function("temperature");
    public static final ResourceKey<DensityFunction> F_HUMIDITY = function("humidity");
    public static final ResourceKey<DensityFunction> F_CONTINENTS = function("continents");
    public static final ResourceKey<DensityFunction> F_EROSION = function("erosion");
    public static final ResourceKey<DensityFunction> F_RIDGES = function("ridges");
    /** The land's height in blocks at x/z. */
    public static final ResourceKey<DensityFunction> HEIGHT = function("height");
    /** The crust's underside in blocks at x/z (spires included). */
    public static final ResourceKey<DensityFunction> CEILING_BOTTOM = function("ceiling_bottom");
    public static final ResourceKey<DensityFunction> FINAL_DENSITY = function("final_density");

    private static final ResourceKey<DensityFunction> SHIFT_X = ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace("shift_x"));
    private static final ResourceKey<DensityFunction> SHIFT_Z = ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace("shift_z"));
    private static final ResourceKey<DensityFunction> Y = ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace("y"));

    private static ResourceKey<NormalNoise> noise(String name) {
        return ResourceKey.create(Registries.NOISE, HollowFrontier.id("hollow_earth/" + name));
    }

    private static ResourceKey<DensityFunction> function(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, HollowFrontier.id("hollow_earth/" + name));
    }

    /*
     * Sizes: a noise sampled at x * 0.25 with first octave -n has its broadest swell about 2^n * 4 blocks across.
     * Biome climate (-8): regions some 600-1000 blocks across, so a walk from a gateway soon finds a second biome.
     */
    public static void bootstrapNoises(BootstrapContext<NormalNoise> context) {
        context.register(TEMPERATURE, NormalNoise.createParity(-8, 1.5, 0.0, 0.4, 0.0));
        context.register(HUMIDITY, NormalNoise.createParity(-8, 1.0, 0.5, 0.0));
        context.register(CONTINENTS, NormalNoise.createParity(-8, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0));
        context.register(EROSION, NormalNoise.createParity(-8, 1.0, 1.0, 0.0, 1.0));
        context.register(RIDGES, NormalNoise.createParity(-6, 1.0, 2.0, 1.0, 0.5));
        context.register(RIVERS, NormalNoise.createParity(-8, 1.0, 0.0, 0.6));
        context.register(CEILING, NormalNoise.createParity(-7, 1.0, 1.0, 0.5));
        context.register(SPIRES, NormalNoise.createParity(-4, 1.0, 0.6));
        context.register(SPIRE_FIELDS, NormalNoise.createParity(-7, 1.0));
        context.register(TEETH, NormalNoise.createParity(-2, 1.0));
        context.register(ISLANDS, NormalNoise.createParity(-5, 1.0, 0.5));
        context.register(ISLAND_FIELDS, NormalNoise.createParity(-7, 1.0));
    }

    public static void bootstrapFunctions(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise> noises = context.lookup(Registries.NOISE);
        HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
        DensityFunction shiftX = get(functions, SHIFT_X);
        DensityFunction shiftZ = get(functions, SHIFT_Z);
        DensityFunction t = register(context, F_TEMPERATURE, DensityFunctions.cache(flat(shiftX, shiftZ, noises.getOrThrow(TEMPERATURE))));
        DensityFunction h = register(context, F_HUMIDITY, DensityFunctions.cache(flat(shiftX, shiftZ, noises.getOrThrow(HUMIDITY))));
        DensityFunction c = register(context, F_CONTINENTS, DensityFunctions.cache(flat(shiftX, shiftZ, noises.getOrThrow(CONTINENTS))));
        DensityFunction e = register(context, F_EROSION, DensityFunctions.cache(flat(shiftX, shiftZ, noises.getOrThrow(EROSION))));
        DensityFunction r = register(context, F_RIDGES, DensityFunctions.cache(flat(shiftX, shiftZ, noises.getOrThrow(RIDGES))));
        DensityFunction rivers = flat(shiftX, shiftZ, noises.getOrThrow(RIVERS));

        // ---- the land's height, in blocks
        // How much of each biome's character a column has, 0..1 (thresholds shared with HollowBiomes)
        DensityFunction upland = DensityFunctions.clampedMap(c, HollowBiomes.HIGHLAND_CONTINENTS - 0.1F, HollowBiomes.HIGHLAND_CONTINENTS + 0.35F, 0.0F, 1.0F);
        DensityFunction cold = DensityFunctions.clampedMap(t, HollowBiomes.COLD - 0.3F, HollowBiomes.COLD + 0.1F, 1.0F, 0.0F);
        DensityFunction hot = DensityFunctions.clampedMap(t, HollowBiomes.HOT - 0.02F, HollowBiomes.HOT + 0.08F, 0.0F, 1.0F);
        DensityFunction dry = DensityFunctions.clampedMap(h, HollowBiomes.WET - 0.1F, HollowBiomes.WET + 0.02F, 1.0F, 0.0F);
        DensityFunction mesa = DensityFunctions.cache(hot.mul(dry).mul(DensityFunctions.constant(1.0F).sub(upland)));
        DensityFunction steppe = dry.mul(DensityFunctions.constant(1.0F).sub(hot)).mul(DensityFunctions.constant(1.0F).sub(cold));
        DensityFunction rise = DensityFunctions.max(upland, cold.mul(0.6F));

        // Rolling hills: rougher where erosion is low, a third as rough on the open steppe
        DensityFunction hillAmp = DensityFunctions.clampedMap(e, -0.6F, 0.6F, 44.0F, 6.0F)
                .mul(DensityFunctions.constant(1.0F).sub(steppe.mul(0.65F)));
        DensityFunction land = DensityFunctions.constant(72.0F).add(c.mul(30.0F)).add(hillAmp.mul(r));
        // The Highlands: lifted up to ~70 blocks, with ridged peaks
        land = land.add(rise.mul(DensityFunctions.constant(70.0F).add(r.abs().mul(42.0F))));
        // The Badlands: stepped into mesa terraces, and the high ground pushed up into tablelands
        DensityFunction terraced = DensityFunctions.floor(land.div(8.0F)).mul(8.0F).add(2.0F)
                .add(DensityFunctions.floor(DensityFunctions.clampedMap(r, -0.1F, 0.4F, 0.0F, 44.0F).div(8.0F)).mul(8.0F));
        land = DensityFunctions.lerp(mesa, land, terraced);
        // Rivers: narrow winding channels cut down to below sea level, everywhere but the dry red country
        DensityFunction river = DensityFunctions.clampedMap(rivers.abs(), 0.02F, 0.07F, 1.0F, 0.0F)
                .mul(DensityFunctions.constant(1.0F).sub(mesa)).mul(DensityFunctions.constant(1.0F).sub(upland.mul(0.6F)));
        land = DensityFunctions.lerp(river, land, DensityFunctions.min(land, DensityFunctions.constant(HollowEarth.SEA_LEVEL - 5.0F)));
        // Nothing climbs into the crust: above 150 the ground rises at 40% (the tallest peaks end around 180)
        land = DensityFunctions.min(land, land.sub(150.0F).mul(0.4F).add(150.0F));
        DensityFunction height = register(context, HEIGHT, DensityFunctions.cache(land));

        // ---- the crust's underside, in blocks
        DensityFunction sway = flat(shiftX, shiftZ, noises.getOrThrow(CEILING)).mul((float) HollowEarth.CEILING_SWAY);
        DensityFunction fields = DensityFunctions.clampedMap(flat(shiftX, shiftZ, noises.getOrThrow(SPIRE_FIELDS)), -0.3F, 0.3F, 0.35F, 1.0F);
        DensityFunction spire = DensityFunctions.clampedMap(flat(shiftX, shiftZ, noises.getOrThrow(SPIRES)), 0.3F, 0.9F, 0.0F, 1.0F)
                .square().mul(fields).mul(110.0F);
        DensityFunction teeth = DensityFunctions.clampedMap(flat(shiftX, shiftZ, noises.getOrThrow(TEETH)), 0.35F, 1.0F, 0.0F, 1.0F)
                .square().mul(10.0F);
        DensityFunction ceilingBottom = register(context, CEILING_BOTTOM,
                DensityFunctions.cache(DensityFunctions.constant((float) HollowEarth.CEILING_BASE).add(sway).sub(spire).sub(teeth)));

        // ---- density
        DensityFunction y = get(functions, Y);
        DensityFunction overhang = new BlendedNoise(0.25, 0.125, 80.0, 120.0, 8.0);
        // The land: solid below the height, roughened into cliffs and overhangs (more in rough country)
        DensityFunction rough = DensityFunctions.clampedMap(e, -0.6F, 0.6F, 0.35F, 0.1F).add(rise.mul(0.2F));
        DensityFunction ground = height.sub(y).div(24.0F).add(overhang.mul(rough));
        ground = DensityFunctions.rangeChoice(y, HollowEarth.MIN_Y, 200.0F, ground, DensityFunctions.constant(-1.0F));
        // The crust: solid above its underside, a little ragged
        DensityFunction crust = y.sub(ceilingBottom).div(10.0F).add(overhang.mul(0.12F));
        crust = DensityFunctions.rangeChoice(y, 100.0F, HollowEarth.TOP_Y + 1.0F, crust, DensityFunctions.constant(-1.0F));
        // Floating islands: flat tops a few blocks either side of 200, rocky cones under them
        DensityFunction islandNoise = flat(shiftX, shiftZ, noises.getOrThrow(ISLANDS));
        DensityFunction islandShape = DensityFunctions.clampedMap(islandNoise, 0.4F, 0.8F, 0.0F, 1.0F)
                .mul(DensityFunctions.clampedMap(flat(shiftX, shiftZ, noises.getOrThrow(ISLAND_FIELDS)), 0.1F, 0.4F, 0.0F, 1.0F));
        DensityFunction islandTop = DensityFunctions.constant(200.0F).add(islandNoise.mul(8.0F));
        DensityFunction island = DensityFunctions.min(
                DensityFunctions.min(islandTop.sub(y).div(3.0F), y.sub(islandTop.sub(2.0F).sub(islandShape.sqrt().mul(34.0F))).div(4.0F)),
                islandShape.sub(0.04F).mul(40.0F));
        island = DensityFunctions.rangeChoice(y, 150.0F, 216.0F, island, DensityFunctions.constant(-1.0F));

        DensityFunction density = DensityFunctions.max(ground, DensityFunctions.max(crust, island));
        density = DensityFunctions.interpolated(density.mul(0.64F), 4, 8).squeeze();
        register(context, FINAL_DENSITY, DensityFunctions.add(density, DensityFunctions.beardifier()));
    }

    /** The noise router: climate for the biome source, the land height for aquifers and material rules, and the density. */
    public static NoiseRouter router(HolderGetter<DensityFunction> functions) {
        return new NoiseRouter(
                get(functions, F_TEMPERATURE),
                get(functions, F_HUMIDITY),
                get(functions, F_CONTINENTS),
                get(functions, F_EROSION),
                DensityFunctions.zero(),
                get(functions, F_RIDGES),
                get(functions, HEIGHT),
                get(functions, FINAL_DENSITY));
    }

    /** Vanilla's aquifers (dry caves, the odd flooded or lava pocket deep down), measured from our land height. */
    public static Aquifer.Config aquifers(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise> noises) {
        return new Aquifer.Config(
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA)),
                DensityFunctions.constant(-1.0F),
                get(functions, HEIGHT));
    }

    private static DensityFunction flat(DensityFunction shiftX, DensityFunction shiftZ, Holder<NormalNoise> noise) {
        return DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noise);
    }

    private static DensityFunction register(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> key, DensityFunction value) {
        return new DensityFunctions.HolderHolder(context.register(key, value));
    }

    private static DensityFunction get(HolderGetter<DensityFunction> functions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(key));
    }
}

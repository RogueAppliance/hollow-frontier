package com.hollowfrontier.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyBoardEntity;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidTimings;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.BlackjackMenu;
import com.hollowfrontier.content.hollow.gateway.GatewayEffects;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.PolarBlueprints;
import com.hollowfrontier.worldgen.structure.TownLayout;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Dev-only trailer shoot (`gradlew runTrailer`, i.e. -Dhollowfrontier.trailer=true): opens run/saves/hf_trailer, finds a
 * frontier town, a badlands range and a Crystal Steppes plain, then stages each shot of the trailer, rolls a moving
 * camera through it and records it (TrailerRecorder) to trailer/clips/NN_name.mp4, NN being its place in the cut, and
 * quits. Shots are shot in the order that suits the world (day before night, the town's raid last); trailer/cut.ps1 puts
 * them in order with the music. `-PtrailerShots=gallop,shootout` re-shoots just those. Does nothing unless the property
 * is set.
 *
 * <p>The camera: in 26.3 the camera is set up before the frame events fire, so a rig places the camera's entity (the
 * player, or a free dolly when the player is busy riding) each tick as an entity moves, at the pose for this tick and
 * the next, and the game's own interpolation carries it between; the angles and FOV are set every frame
 * (ComputeCameraAngles, ComputeFov) for where the camera is by then.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class Trailer {
    private Trailer() {}

    private static final boolean ENABLED = Boolean.getBoolean("hollowfrontier.trailer");
    /** Filming through a shader pack (`runTrailer -Pshaders`). */
    private static final boolean SHADERS = Boolean.getBoolean("hollowfrontier.trailer.shaders");
    private static final Set<String> ONLY = Arrays.stream(System.getProperty("hollowfrontier.trailer.shots", "").split(","))
            .map(String::trim).filter(s -> !s.isEmpty() && !s.equals("all")).collect(Collectors.toSet());
    private static final Path OUT = Path.of(System.getProperty("hollowfrontier.trailer.out", "trailer/clips"));
    private static final int FPS = Integer.getInteger("hollowfrontier.trailer.fps", 60);
    /** Which frontier town to film: 0 is the nearest to the search point, higher numbers search further out. */
    private static final int TOWN = Integer.getInteger("hollowfrontier.trailer.town", 0);
    /** Where the town search starts (since the structure overhaul it finds a town with the plain saloon at 11296,6672), so re-shoots return to it. */
    private static final int[] TOWN_SEARCH = Arrays.stream(System.getProperty("hollowfrontier.trailer.search", "6884,4507").split(",")).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
    /** Where the cow is abducted (north of the town, which stands behind it); "search" finds a pasture afresh. */
    private static final String PASTURE = System.getProperty("hollowfrontier.trailer.pasture", "11259,6639");
    /** Which street out of the square is the main street, as "x,z" (that town's west street; "search" picks the flattest). */
    private static final String STREET = System.getProperty("hollowfrontier.trailer.street", "-1,0");
    /** The plain saloon's piece in any palette (saloon, saloon_2), not the grand one. */
    private static final java.util.regex.Pattern PLAIN_SALOON = java.util.regex.Pattern.compile("town/saloon(_\\d+)?]");
    /** Seconds rolled before and after each shot's slot in the cut, for cut.ps1 to trim. */
    private static final double HANDLES = 2.0;
    private static final int Y = 250; // the sky stage (the boiler, and a gateway crater if no real one is found)
    private static final int GENERATE_TICKS = 300;

    private static final TrailerRecorder RECORDER = new TrailerRecorder(FPS);
    private static final Map<String, Vec3> MARKS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> SUBJECTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Entity> SEEN = new ConcurrentHashMap<>();
    /** Run on the server every tick while a shot needs it (riders at a gallop). */
    private static final List<Runnable> JOBS = new CopyOnWriteArrayList<>();

    private static int ticks = -1;
    private static int baseX;
    private static int baseZ;
    private static volatile StructureStart town;
    private static volatile PoolElementStructurePiece saloon;
    private static volatile TownSite raidSite;
    private static volatile BlockPos landed;
    private static volatile PoolElementStructurePiece gatewayPiece;

    private static List<Step> steps;
    private static int stepIndex;
    private static int stepTicks = -1;
    private static List<Shot> shots;
    private static int shotIndex;
    private static Shot current;
    private static boolean finished;
    private static int phaseTicks;
    private static boolean rolling;
    private static int rollTicks;
    private static long frameNanos;
    private static ArmorStand dolly;

    // ---------------------------------------------------------------- the camera

    /** Where the camera is and what it looks at. */
    record Pose(Vec3 eye, Vec3 look, float roll, float fov) {}

    /** The camera by seconds into the roll; partial is how far between the last tick and the next the subjects are drawn. */
    @FunctionalInterface
    interface Rig {
        Pose at(double s, float partial);
    }

    private static final class Shot {
        final int number;
        final String name;
        final double seconds;
        int rollOverride; // ticks, when a shot rolls longer than its slot and handles
        int maxWait = 600; // ticks past the settle to wait for it to be ready before rolling anyway
        Runnable setup = () -> {};
        Runnable wrap = () -> {};
        int settle = 60;
        Rig rig;           // null leaves the game's own camera (a screen, the fall)
        boolean detached;  // the rig moves a dolly of its own; the player stays where they are (riding, say)
        java.util.function.Function<Float, Vec3> aim; // where the player looks while a detached rig films them
        boolean waitForChunks = true;
        BooleanSupplier ready = () -> true;
        final TreeMap<Integer, Runnable> settleCues = new TreeMap<>();
        final TreeMap<Integer, Runnable> cues = new TreeMap<>();

        Shot(int number, String name, double seconds) {
            this.number = number;
            this.name = name;
            this.seconds = seconds;
        }

        /** A cue {@code second}s into the roll. */
        Shot at(double second, Runnable cue) {
            cues.merge((int) Math.round(second * 20), cue, (a, b) -> () -> { a.run(); b.run(); });
            return this;
        }

        /** A cue {@code tick}s into the settle, before the roll. */
        Shot during(int tick, Runnable cue) {
            settleCues.merge(tick, cue, (a, b) -> () -> { a.run(); b.run(); });
            return this;
        }

        int rollTicks() {
            return rollOverride > 0 ? rollOverride : (int) Math.round((seconds + HANDLES) * 20);
        }

        String file() {
            return String.format(Locale.ROOT, "%02d_%s.mp4", number, name);
        }
    }

    private record Step(String label, Runnable action, int minTicks, BooleanSupplier until) {}

    // ---------------------------------------------------------------- the loop

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) {
            return;
        }
        if (ticks < 0) {
            // The sky stage stands out at the town search point, well away from the town (so never in its sky)
            baseX = TOWN_SEARCH[0];
            baseZ = TOWN_SEARCH[1];
            steps = prep(mc, server);
            HollowFrontier.LOGGER.info("[Trailer] world loaded, stage at {} {} {}", baseX, Y, baseZ);
        }
        ticks++;
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.gui.setScreen(null);
        }
        if (ticks < 40) {
            return;
        }
        if (stepIndex < steps.size()) {
            step(steps.get(stepIndex));
            return;
        }
        if (shots == null) {
            shots = shots(mc, server).stream().filter(s -> ONLY.isEmpty() || ONLY.contains(s.name)).toList();
            HollowFrontier.LOGGER.info("[Trailer] shooting {} shots: {}", shots.size(), shots.stream().map(s -> s.name).toList());
        }
        mc.gui.toastManager().clear(); // no advancement or recipe toasts in the shots
        shoot(mc, server);
        place(mc);
    }

    private static void step(Step step) {
        if (stepTicks < 0) {
            HollowFrontier.LOGGER.info("[Trailer] prep: {}", step.label());
            step.action().run();
            stepTicks = 0;
            return;
        }
        stepTicks++;
        boolean done = step.until().getAsBoolean();
        if (stepTicks < step.minTicks() || !done && stepTicks < step.minTicks() + 1200) {
            return;
        }
        if (!done) {
            HollowFrontier.LOGGER.warn("[Trailer] prep: gave up waiting on {}", step.label());
        }
        stepIndex++;
        stepTicks = -1;
    }

    private static void shoot(Minecraft mc, MinecraftServer server) {
        if (finished) {
            return;
        }
        if (current == null) {
            if (shotIndex >= shots.size()) {
                HollowFrontier.LOGGER.info("[Trailer] all shots done, quitting");
                jadeTooltip(true);
                jeiOverlay(true);
                run(server, "execute in minecraft:overworld run forceload remove all");
                run(server, "execute in " + HollowEarth.LEVEL.identifier() + " run forceload remove all");
                server.execute(() -> mc.execute(mc::stop));
                finished = true;
                return;
            }
            current = shots.get(shotIndex++);
            HollowFrontier.LOGGER.info("[Trailer] shot {}: {}", current.number, current.name);
            SEEN.clear();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            current.setup.run();
            phaseTicks = 0;
            rolling = false;
            return;
        }
        if (!rolling) {
            Runnable cue = current.settleCues.get(phaseTicks);
            if (cue != null) {
                cue.run();
            }
            phaseTicks++;
            boolean drawn = !current.waitForChunks || mc.levelRenderer.hasRenderedAllSections();
            int late = phaseTicks - current.settle;
            if (late >= 0 && (current.ready.getAsBoolean() && drawn || late > current.maxWait)) {
                if (late > current.maxWait) {
                    HollowFrontier.LOGGER.warn("[Trailer] {}: rolling without everything ready", current.name);
                }
                rolling = true;
                rollTicks = 0;
                var target = mc.gameRenderer.mainRenderTarget();
                RECORDER.start(OUT.resolve(current.file()), target.width, target.height);
                runCue(0);
            }
            return;
        }
        rollTicks++;
        runCue(rollTicks);
        if (rollTicks >= current.rollTicks()) {
            RECORDER.stop(System.nanoTime());
            current.wrap.run();
            JOBS.clear();
            if (mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
            current = null;
            rolling = false;
        }
    }

    private static void runCue(int tick) {
        Runnable cue = current.cues.get(tick);
        if (cue != null) {
            cue.run();
        }
    }

    /** Puts the camera's entity at this tick's pose, heading for the next tick's. */
    private static void place(Minecraft mc) {
        if (current == null || current.rig == null) {
            return;
        }
        Entity camera = mc.player;
        if (current.detached) {
            if (dolly == null || dolly.level() != mc.level) {
                dolly = new ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
                // Never added to the level, so it has no id of its own: comparing entities (a saucer asking whether the
                // camera is aboard) reads it, and an unassigned id throws
                dolly.setId(-0x7A11E7);
            }
            camera = dolly;
            if (mc.getCameraEntity() != dolly) {
                mc.setCameraEntity(dolly);
            }
        }
        double s0 = rolling ? rollTicks / 20.0 : 0, s1 = rolling ? (rollTicks + 1) / 20.0 : 0;
        Pose a = current.rig.at(s0, 0.0F), b = current.rig.at(s1, 1.0F);
        double eye = camera.getEyeHeight();
        camera.setPos(b.eye().x, b.eye().y - eye, b.eye().z);
        camera.xo = a.eye().x;
        camera.yo = a.eye().y - eye;
        camera.zo = a.eye().z;
        camera.setDeltaMovement(Vec3.ZERO);
        camera.setYRot(yawOf(b.look().subtract(b.eye())));
        camera.setXRot(pitchOf(b.look().subtract(b.eye())));
        camera.yRotO = yawOf(a.look().subtract(a.eye()));
        camera.xRotO = pitchOf(a.look().subtract(a.eye()));
        camera.resetFallDistance();
        if (current.detached && current.aim != null) {
            // The player, filmed from outside, turns to what they're shooting at (the server fires along their look)
            Vec3 at = current.aim.apply(1.0F).subtract(mc.player.getEyePosition());
            mc.player.yRotO = mc.player.getYRot();
            mc.player.xRotO = mc.player.getXRot();
            mc.player.setYRot(yawOf(at));
            mc.player.setXRot(pitchOf(at));
            mc.player.setYHeadRot(yawOf(at));
            mc.player.setYBodyRot(yawOf(at));
        }
    }

    /** Jade's tooltip would caption whatever the camera looks at; it's off while filming (Jade is optional, so by name). */
    private static void jadeTooltip(boolean show) {
        try {
            Object config = Class.forName("snownee.jade.api.config.IWailaConfig").getMethod("get").invoke(null);
            Object general = config.getClass().getMethod("general").invoke(config);
            general.getClass().getMethod("setDisplayTooltip", boolean.class).invoke(general, show);
        } catch (ReflectiveOperationException | LinkageError e) {
            // No Jade
        }
    }

    /** JEI's ingredient list would sit beside the blackjack table's screen; it's off while filming (JEI's internal toggle, by name). */
    private static void jeiOverlay(boolean show) {
        try {
            Object toggles = Class.forName("mezz.jei.common.Internal").getMethod("getClientToggleState").invoke(null);
            Class.forName("mezz.jei.common.config.IClientToggleState").getMethod("setOverlayEnabled", boolean.class).invoke(toggles, show);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // No JEI, or it isn't up yet
        }
    }

    private static Pose framePose(float partial) {
        double s = rolling ? (rollTicks + partial) / 20.0 : 0;
        return current.rig.at(s, partial);
    }

    private static boolean rigged() {
        return ENABLED && current != null && current.rig != null;
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!rigged()) {
            return;
        }
        Pose pose = framePose((float) event.getPartialTick());
        Vec3 v = pose.look().subtract(event.getCamera().position());
        event.setYaw(yawOf(v));
        event.setPitch(pitchOf(v));
        event.setRoll(pose.roll());
    }

    @SubscribeEvent
    static void onFov(ViewportEvent.ComputeFov event) {
        if (rigged()) {
            event.setFOV(framePose((float) event.getPartialTick()).fov());
        }
    }

    @SubscribeEvent
    static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        // No HUD in the shots, only our own overlays (the gateway's frost and light), and not even those when filmed from
        // outside the player (a pilot's crosshair and gauge); a screen still draws
        if (ENABLED && current != null && (current.detached || !event.getName().getNamespace().equals(HollowFrontier.MODID))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        // Filmed from outside the player, their first-person hand mustn't draw
        if (ENABLED && current != null && current.detached) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event) {
        // No names over anyone's head: the trailer carries no text of its own (the player's the nameless cowboy, and the
        // barmaid and the outlaws go unnamed)
        if (ENABLED && current != null) {
            event.setCanRender(net.minecraft.util.TriState.FALSE);
        }
    }

    private static final net.minecraft.resources.Identifier COWBOY_SKIN = HollowFrontier.id("trailer/cowboy_skin");
    private static net.minecraft.core.ClientAsset.@org.jspecify.annotations.Nullable Texture cowboySkin;
    private static boolean cowboySkinTried;

    @SubscribeEvent
    static void onRenderPlayer(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre<?> event) {
        // The cowboy wears a cowboy's own skin under his gear, not the dev account's default
        var state = event.getRenderState();
        var skin = ENABLED ? cowboySkin() : null;
        if (skin != null) {
            state.skin = new net.minecraft.world.entity.player.PlayerSkin(skin, state.skin.cape(), state.skin.elytra(),
                    net.minecraft.world.entity.player.PlayerModelType.WIDE, state.skin.secure());
        }
    }

    /**
     * The wrangler's texture as a player skin: the humanoid NPCs share the player's layout for head, body, limbs, but keep
     * their hat's brim and crown where a skin has its overlay layers, so those are cleared (the gear goes on over it).
     */
    private static net.minecraft.core.ClientAsset.@org.jspecify.annotations.Nullable Texture cowboySkin() {
        if (!cowboySkinTried) {
            cowboySkinTried = true;
            Minecraft mc = Minecraft.getInstance();
            try (var in = mc.getResourceManager().open(HollowFrontier.id("textures/entity/townsfolk_wrangler.png"))) {
                var image = com.mojang.blaze3d.platform.NativeImage.read(in);
                for (int y = 0; y < 64; y++) {
                    for (int x = 0; x < 64; x++) {
                        boolean overlay = y < 16 && x >= 32 || y >= 32 && y < 48 || y >= 48 && (x < 16 || x >= 48);
                        if (overlay) {
                            image.setPixel(x, y, 0);
                        }
                    }
                }
                mc.getTextureManager().register(COWBOY_SKIN, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "trailer cowboy skin", image));
                cowboySkin = new net.minecraft.core.ClientAsset.ResourceTexture(COWBOY_SKIN, COWBOY_SKIN);
            } catch (java.io.IOException e) {
                HollowFrontier.LOGGER.warn("[Trailer] no cowboy skin: {}", e.getMessage());
            }
        }
        return cowboySkin;
    }

    @SubscribeEvent
    static void onParticleProviders(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        // The Hollow's drifting spores (jungle) and sparkles (steppes) are lit like dust under the pack, which doesn't fade
        // them into the haze as vanilla's fog does: dark squares and dark crosses over the steppes. The shader shoot leaves
        // them out (no shot uses either).
        if (ENABLED && SHADERS) {
            event.registerSpecial(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, (options, level, x, y, z, xa, ya, za, random) -> null);
            event.registerSpecial(net.minecraft.core.particles.ParticleTypes.GLOW, (options, level, x, y, z, xa, ya, za, random) -> null);
        }
    }

    @SubscribeEvent
    static void onFrameStart(RenderFrameEvent.Pre event) {
        frameNanos = System.nanoTime();
    }

    @SubscribeEvent
    static void onFrameEnd(RenderFrameEvent.Post event) {
        if (ENABLED && RECORDER.recording()) {
            RECORDER.capture(Minecraft.getInstance().gameRenderer.mainRenderTarget(), frameNanos);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (ENABLED) {
            JOBS.forEach(Runnable::run);
        }
    }

    // ---------------------------------------------------------------- prep: finding the places

    private static List<Step> prep(Minecraft mc, MinecraftServer server) {
        List<Step> list = new ArrayList<>();
        list.add(new Step("options", () -> {
            mc.getWindow().setWindowed(1920, 1080);
            jadeTooltip(false);
            jeiOverlay(false);
            mc.options.renderDistance().set(16);
            mc.options.bobView().set(false);
            mc.options.framerateLimit().set(120);
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            // Wherever the save left the player, back to the Overworld (commands here run from it)
            run(server, String.format("execute in minecraft:overworld run tp @a %d %d %d", baseX, Y + 2, baseZ));
            run(server, "gamemode spectator @a");
            run(server, "weather clear 1000000");
            run(server, "time set 6000");
            run(server, "kill @e[type=!player,distance=..200]");
        }, 20, () -> true));
        // The sky stage over the spawn: the boiler, the depot and the gateway crater, built afresh each run
        list.add(new Step("sky stage", () -> buildStage(server), 40, () -> true));
        list.add(new Step("find a frontier town", () -> findTown(server), 5, () -> MARKS.containsKey("town_search")));
        list.add(new Step("let the town generate", () -> {}, GENERATE_TICKS, () -> true));
        list.add(new Step("frame the town", () -> frameTown(server), 5, () -> MARKS.containsKey("town") || MARKS.containsKey("no_town")));
        list.add(new Step("find a badlands range", () -> findRange(server), 5, () -> MARKS.containsKey("range_search")));
        list.add(new Step("let the range generate", () -> {}, GENERATE_TICKS, () -> true));
        list.add(new Step("pick the gallop and the camp", () -> pickRange(server), 5, () -> MARKS.containsKey("range")));
        list.add(new Step("find a polar gateway", () -> findGateway(server), 5, () -> MARKS.containsKey("gateway_search")));
        list.add(new Step("let the gateway generate", () -> {}, GENERATE_TICKS, () -> true));
        list.add(new Step("find its well", () -> frameGateway(server), 5, () -> MARKS.containsKey("well") || MARKS.containsKey("no_gateway")));
        return list;
    }

    /** The nearest Polar Gateway crater (frozen biomes) to the town, out in the snow where it generated. */
    private static void findGateway(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(com.hollowfrontier.worldgen.structure.ModStructures.POLAR_GATEWAY);
            BlockPos from = BlockPos.containing(MARKS.getOrDefault("town", new Vec3(baseX, 64, baseZ)));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure), from, 400, false);
            if (found == null) {
                HollowFrontier.LOGGER.warn("[Trailer] no polar gateway near {}", from);
                MARKS.put("gateway_search", Vec3.ZERO);
                MARKS.put("no_gateway", Vec3.ZERO);
                return;
            }
            BlockPos at = found.getFirst();
            MARKS.put("gateway_search", Vec3.atCenterOf(at));
            run(server, String.format("forceload add %d %d %d %d", at.getX() - 48, at.getZ() - 48, at.getX() + 48, at.getZ() + 48));
            run(server, String.format("tp @a %d 200 %d 0 90", at.getX(), at.getZ()));
            HollowFrontier.LOGGER.info("[Trailer] polar gateway at {}", at);
        });
    }

    private static void frameGateway(MinecraftServer server) {
        server.execute(() -> {
            if (MARKS.containsKey("no_gateway")) {
                return;
            }
            ServerLevel level = server.overworld();
            Vec3 search = MARKS.get("gateway_search");
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(com.hollowfrontier.worldgen.structure.ModStructures.POLAR_GATEWAY).value();
            StructureStart start = level.getChunk(Mth.floor(search.x) >> 4, Mth.floor(search.z) >> 4).getStartForStructure(structure);
            if (start == null || !start.isValid() || !(start.getPieces().getFirst() instanceof PoolElementStructurePiece piece)) {
                HollowFrontier.LOGGER.warn("[Trailer] the polar gateway didn't generate");
                MARKS.put("no_gateway", Vec3.ZERO);
                return;
            }
            BlockPos well = BlockPos.containing(inPiece(piece, new Vec3(PolarBlueprints.CENTER, PolarBlueprints.RING_Y, PolarBlueprints.CENTER)));
            MARKS.put("well", Vec3.atLowerCornerOf(well));
            gatewayPiece = piece;
            MARKS.put("crater_rim", new Vec3(well.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, well.getX() + 16, well.getZ() + 16), well.getZ()));
            HollowFrontier.LOGGER.info("[Trailer] the gateway's well at {} (piece at {}, turned {})", well, piece.getPosition(), piece.getRotation());
        });
    }

    /** A point in a structure piece's template, where the piece put it in the world. */
    private static Vec3 inPiece(PoolElementStructurePiece piece, Vec3 local) {
        Vec3 turned = StructureTemplate.transform(local, net.minecraft.world.level.block.Mirror.NONE, piece.getRotation(), BlockPos.ZERO);
        return turned.add(piece.getPosition().getX(), piece.getPosition().getY(), piece.getPosition().getZ());
    }

    private static void buildStage(MinecraftServer server) {
        int x = baseX, z = baseZ;
        run(server, String.format("forceload add %d %d %d %d", x - 48, z - 48, x + 160, z + 48));
        for (int y = Y; y <= Y + 24; y++) {
            run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 40, y, z - 40, x + 40, y, z + 40));
            run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x + 90, y, z - 40, x + 160, y, z + 40));
        }
        run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 40, Y - 1, z - 40, x + 40, Y - 1, z + 40));
        // The steam row against a brick wall: boiler, two pipes, tank (gauge on top), crusher
        run(server, String.format("fill %d %d %d %d %d %d minecraft:bricks", x - 3, Y, z + 1, x + 9, Y + 4, z + 1));
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone_bricks", x - 3, Y - 1, z - 3, x + 9, Y - 1, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:boiler", x, Y, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:copper_pipe[east=true,west=true]", x + 1, Y, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:copper_pipe[east=true,west=true]", x + 2, Y, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:steam_tank", x + 3, Y, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:pressure_gauge", x + 3, Y + 1, z));
        run(server, String.format("setblock %d %d %d hollowfrontier:crusher", x + 5, Y, z));
        run(server, String.format("setblock %d %d %d minecraft:lantern", x - 2, Y, z));
        run(server, String.format("setblock %d %d %d minecraft:lantern", x + 7, Y, z));
        // (The depot and the polar gateway are filmed where they stand on the land: by the town, and out in the snow.)
    }

    /** The real crater's well centre (found in prep); without one, a crater placed on the sky stage, east. */
    private static BlockPos well() {
        Vec3 found = MARKS.get("well");
        return found != null ? BlockPos.containing(found)
                : new BlockPos(baseX + 130 + PolarBlueprints.CENTER, Y - PolarBlueprints.SINK + PolarBlueprints.RING_Y, baseZ - 30 + PolarBlueprints.CENTER);
    }

    /**
     * Puts the crater back as it generated (an earlier take may have opened it): the real one placed again where and as
     * it was turned, or, without one, the stage's own, east.
     */
    private static void resetCrater(MinecraftServer server) {
        PoolElementStructurePiece piece = gatewayPiece;
        if (piece != null) {
            BlockPos at = piece.getPosition();
            run(server, String.format("place template hollowfrontier:polar_gateway %d %d %d %s", at.getX(), at.getY(), at.getZ(), piece.getRotation().getSerializedName()));
            return;
        }
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone", baseX + 90, Y - 1, baseZ - 40, baseX + 160, Y - 1, baseZ + 40));
        run(server, String.format("place template hollowfrontier:polar_gateway %d %d %d", baseX + 130, Y - PolarBlueprints.SINK, baseZ - 30));
    }

    /**
     * The best-looking land round a point, by the biomes the generator will put there (no chunks needed): open dry country
     * (plains, savanna, desert, badlands) scores best, woods and hills a little, water counts against.
     */
    private static double openCountry(ServerLevel level, double x, double z, int radius, int step) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var resolver = source.createUncachedResolver(level.getChunkSource().randomState());
        double score = 0;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                var biome = resolver.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(Mth.floor(x) + dx), net.minecraft.core.QuartPos.fromBlock(72),
                        net.minecraft.core.QuartPos.fromBlock(Mth.floor(z) + dz));
                if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)
                        || biome.is(net.minecraft.world.level.biome.Biomes.SWAMP) || biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) {
                    score -= 2;
                } else if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                        || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS) || biome.is(net.minecraft.world.level.biome.Biomes.DESERT)) {
                    score += 2;
                } else if (!biome.is(BiomeTags.IS_JUNGLE) && !biome.is(BiomeTags.IS_MOUNTAIN)) {
                    score += 0.5;
                }
            }
        }
        return score;
    }

    /** How much of the land round a point is red-sand badlands (plain or eroded, not the wooded plateau). */
    private static double redBadlands(ServerLevel level, double x, double z, int radius, int step) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var resolver = source.createUncachedResolver(level.getChunkSource().randomState());
        double score = 0;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                var biome = resolver.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(Mth.floor(x) + dx), net.minecraft.core.QuartPos.fromBlock(72),
                        net.minecraft.core.QuartPos.fromBlock(Mth.floor(z) + dz));
                if (biome.is(net.minecraft.world.level.biome.Biomes.BADLANDS) || biome.is(net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS)) {
                    score++;
                }
            }
        }
        return score;
    }

    /** Of the frontier towns nearest a ring of search points, the one in the most open, driest country. */
    private static void findTown(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(TownStyle.FRONTIER.structure());
            int ox = TOWN_SEARCH[0] + TOWN * 6000, oz = TOWN_SEARCH[1];
            ChunkPos chunk = null;
            double best = -Double.MAX_VALUE;
            Set<ChunkPos> seen = new java.util.HashSet<>();
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI * 2 / 7;
                BlockPos from = i == 0 ? new BlockPos(ox, 0, oz) : new BlockPos(ox + (int) (1600 * Math.cos(angle)), 0, oz + (int) (1600 * Math.sin(angle)));
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure), from, 100, false);
                if (found == null || !seen.add(ChunkPos.containing(found.getFirst()))) {
                    continue;
                }
                double score = openCountry(level, found.getFirst().getX(), found.getFirst().getZ(), 128, 16);
                // The saloon shots need the plain saloon (the grand one is laid out differently): read the town's plan,
                // which is settled before any of it is built
                ChunkPos at = ChunkPos.containing(found.getFirst());
                StructureStart plan = level.getChunk(at.x(), at.z(), net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS)
                        .getStartForStructure(structure.value());
                boolean plainSaloon = plan != null && plan.getPieces().stream()
                        .anyMatch(p -> p instanceof PoolElementStructurePiece piece && PLAIN_SALOON.matcher(piece.getElement().toString()).find());
                score += plainSaloon ? 400 : 0;
                HollowFrontier.LOGGER.info("[Trailer] town candidate at {}: open country {}, plain saloon {}", found.getFirst(), score, plainSaloon);
                if (score > best) {
                    best = score;
                    chunk = ChunkPos.containing(found.getFirst());
                }
            }
            if (chunk == null) {
                HollowFrontier.LOGGER.warn("[Trailer] no frontier town near {} {}", ox, oz);
                MARKS.put("town_search", Vec3.ZERO);
                return;
            }
            int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
            MARKS.put("town_search", new Vec3(x, 0, z));
            run(server, String.format("forceload add %d %d %d %d", x - 112, z - 112, x + 112, z + 112));
            run(server, String.format("tp @a %d 220 %d 0 90", x, z));
            HollowFrontier.LOGGER.info("[Trailer] frontier town at chunk {}", chunk);
        });
    }

    private static void frameTown(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            Vec3 search = MARKS.get("town_search");
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(TownStyle.FRONTIER.structure()).value();
            StructureStart start = search == null ? null
                    : level.getChunk(Mth.floor(search.x) >> 4, Mth.floor(search.z) >> 4).getStartForStructure(structure);
            if (start == null || !start.isValid()) {
                HollowFrontier.LOGGER.warn("[Trailer] the town didn't generate");
                MARKS.put("no_town", Vec3.ZERO);
                return;
            }
            town = start;
            BoundingBox square = start.getPieces().getFirst().getBoundingBox();
            Vec3 c = new Vec3(square.getCenter().getX() + 0.5, square.minY() + 1, square.getCenter().getZ() + 0.5);
            // Of the streets out of the square, the one that runs flattest and clearest (no hill to film into)
            Vec3 d = new Vec3(0, 0, 1);
            double flattest = Double.MAX_VALUE;
            for (var piece : start.getPieces()) {
                if (!(piece instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains("street_"))) {
                    continue;
                }
                Vec3 v = Vec3.atCenterOf(piece.getBoundingBox().getCenter()).subtract(c);
                Vec3 dir = Math.abs(v.x) > Math.abs(v.z) ? new Vec3(Math.signum(v.x), 0, 0) : new Vec3(0, 0, Math.signum(v.z));
                double rough = 0;
                for (int step = TownLayout.MID; step <= TownLayout.MID + 40; step += 2) {
                    Vec3 at = c.add(dir.scale(step));
                    rough += Math.abs(ground(level, at).y - c.y) + 3 * Math.max(0, canopy(level, at) - c.y - 1);
                }
                HollowFrontier.LOGGER.info("[Trailer] street toward {}: roughness {}", dir, rough);
                if (rough < flattest) {
                    flattest = rough;
                    d = dir;
                }
            }
            String[] street = STREET.split(",");
            if (street.length == 2) {
                d = new Vec3(Integer.parseInt(street[0].trim()), 0, Integer.parseInt(street[1].trim()));
            }
            MARKS.put("town", c);
            MARKS.put("dir", d);
            saloon = start.getPieces().stream().filter(p -> p instanceof PoolElementStructurePiece piece && PLAIN_SALOON.matcher(piece.getElement().toString()).find())
                    .map(p -> (PoolElementStructurePiece) p).findFirst().orElse(null);
            // The outskirts behind the square (where the main street doesn't run): the airship's field, and the cows'
            BoundingBox bb = start.getBoundingBox();
            double back = d.x > 0 ? c.x - bb.minX() : d.x < 0 ? bb.maxX() - c.x : d.z > 0 ? c.z - bb.minZ() : bb.maxZ() - c.z;
            Vec3 across = new Vec3(-d.z, 0, d.x);
            Vec3 outskirts = ground(level, c.subtract(d.scale(back + 16)));
            MARKS.put("outskirts", outskirts);
            MARKS.put("field", ground(level, outskirts.add(across.scale(26))));
            Vec3 o = outskirts;
            run(server, String.format("forceload add %d %d %d %d", (int) o.x - 64, (int) o.z - 64, (int) o.x + 64, (int) o.z + 64));
            HollowFrontier.LOGGER.info("[Trailer] town: {} pieces, square at {}, street {}, saloon {}, outskirts {}", start.getPieces().size(), c, d,
                    saloon == null ? "none (a grand one)" : saloon.getPosition(), outskirts);
        });
    }

    private static void findRange(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            Vec3 from = MARKS.getOrDefault("town", new Vec3(baseX, 64, baseZ));
            // Red sand under mesas and hoodoos (not the wooded plateau, whose top is coarse dirt)
            var found = level.findClosestBiome3d(b -> b.is(net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS) || b.is(net.minecraft.world.level.biome.Biomes.BADLANDS),
                    BlockPos.containing(from), 6400, 32, 64);
            if (found == null) {
                found = level.findClosestBiome3d(b -> b.is(BiomeTags.IS_BADLANDS), BlockPos.containing(from), 6400, 32, 64);
            }
            if (found == null) {
                found = level.findClosestBiome3d(b -> b.is(net.minecraft.world.level.biome.Biomes.DESERT), BlockPos.containing(from), 6400, 32, 64);
            }
            BlockPos at = found == null ? BlockPos.containing(from).offset(400, 0, 0) : found.getFirst();
            // Move into the heart of it: the point nearby with the most badlands (or other open country) all round
            double best = -Double.MAX_VALUE;
            BlockPos heart = at;
            for (int dx = -480; dx <= 480; dx += 48) {
                for (int dz = -480; dz <= 480; dz += 48) {
                    double score = openCountry(level, at.getX() + dx, at.getZ() + dz, 112, 16) + 3 * redBadlands(level, at.getX() + dx, at.getZ() + dz, 112, 16)
                            - Math.hypot(dx, dz) / 200;
                    if (score > best) {
                        best = score;
                        heart = at.offset(dx, 0, dz);
                    }
                }
            }
            at = heart;
            MARKS.put("range_search", Vec3.atCenterOf(at));
            run(server, String.format("forceload add %d %d %d %d", at.getX() - 96, at.getZ() - 96, at.getX() + 96, at.getZ() + 96));
            run(server, String.format("tp @a %d 200 %d 0 90", at.getX(), at.getZ()));
            HollowFrontier.LOGGER.info("[Trailer] badlands at {}", at);
        });
    }

    /** The gallop runs the flattest way out from the range's middle; the camp sits on a levelled pad behind it. */
    private static void pickRange(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            Vec3 at = MARKS.get("range_search");
            Vec3 best = new Vec3(1, 0, 0);
            double bestScore = Double.MAX_VALUE;
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8;
                Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                double score = 0, last = canopy(level, at);
                for (int step = 4; step <= 90; step += 3) {
                    Vec3 p = at.add(dir.scale(step));
                    double y = canopy(level, p);
                    score += Math.abs(y - last) * Math.abs(y - last);
                    // Trees in the riders' way or the camera's (running alongside), and leaving open country, cost a lot
                    score += 40 * (canopy(level, p) - ground(level, p).y) + 40 * (canopy(level, p.add(side.scale(8))) - ground(level, p.add(side.scale(8))).y);
                    score += openCountry(level, p.x, p.z, 0, 1) > 0 ? 0 : 200;
                    score += level.getFluidState(BlockPos.containing(p.x, ground(level, p).y - 1, p.z)).isEmpty() ? 0 : 300; // no wading
                    last = y;
                }
                if (score < bestScore) {
                    bestScore = score;
                    best = dir;
                }
            }
            MARKS.put("range_dir", best);
            // The camera runs on whichever side the ground is lower (a rise beside the riders would cross the frame)
            Vec3 side = new Vec3(-best.z, 0, best.x);
            double left = 0, right = 0;
            for (int step = 0; step <= 90; step += 6) {
                Vec3 p = at.add(best.scale(step));
                left += canopy(level, p.add(side.scale(7)));
                right += canopy(level, p.subtract(side.scale(7)));
            }
            MARKS.put("range_side", left <= right ? side : side.scale(-1));
            MARKS.put("range", ground(level, at));
            // A 17 x 17 pad 30 blocks back from the gallop's start, the camp placed on it
            Vec3 camp = ground(level, at.subtract(best.scale(34)));
            int cx = Mth.floor(camp.x) - 7, cy = Mth.floor(camp.y) - 1, cz = Mth.floor(camp.z) - 7;
            MARKS.put("camp", new Vec3(cx, cy, cz));
            HollowFrontier.LOGGER.info("[Trailer] gallop from {} toward {} (bumpiness {}), camp at {} {} {}", at, best, bestScore, cx, cy, cz);
        });
    }

    // ---------------------------------------------------------------- the shots

    private static List<Shot> shots(Minecraft mc, MinecraftServer server) {
        List<Shot> list = new ArrayList<>();
        Vec3 c = MARKS.get("town");
        Vec3 d = MARKS.getOrDefault("dir", new Vec3(0, 0, 1));
        Vec3 across = new Vec3(-d.z, 0, d.x);
        boolean haveTown = c != null;
        if (haveTown) {
            list.add(coldOpen(server, c, d, across));
            list.add(townAerial(server, c, d, across));
            list.add(mainStreet(server, c, d, across));
            if (saloon != null) {
                list.add(saloonDoors(server));
                list.add(barmaid(server));
                list.add(blackjack(mc, server));
                list.add(bountyBoard(server));
                list.add(mrGray(server));
            } else {
                HollowFrontier.LOGGER.warn("[Trailer] this town has the grand saloon; skipping the saloon shots (try -PtrailerTown=1)");
            }
            list.add(airship(server, c, d));
        }
        if (MARKS.containsKey("range")) {
            list.add(gallop(server));
            list.add(revolverLoad(server));
            list.add(shootout(server));
            list.add(dynamite(server));
        }
        list.add(boiler(server));
        if (haveTown) {
            list.add(quietNight(server, c, d));
            list.add(abduction(server, c));
            list.add(warden(server, c));
            list.add(mothership(server, c, d));
            list.add(nightRaid(server));
            list.add(endTown(server, c, d));
        }
        list.add(gatewayWake(server));
        list.add(theFall(mc, server));
        list.add(hollowVista(server));
        list.add(saucerFlight(server));
        list.add(captives(server));
        list.add(archon(server));
        list.add(crystalRuins(server));
        list.add(kindledSite(server));
        list.add(endHollow(server));
        return list;
    }

    // 1. Sunup in the town square: over the shoulder of a cowboy squared up to the saloon, and the first shot fired into
    // the sky (the music comes in on it). The saloon fronts the square, so there's nothing but open ground between them.
    private static Shot coldOpen(MinecraftServer server, Vec3 c, Vec3 d, Vec3 across) {
        Shot shot = new Shot(1, "cold_open", 2.5);
        shot.setup = () -> {
            run(server, "time set 23800");
            handGun(server, "revolver", false);
            cowboy(server);
            Minecraft.getInstance().player.getInventory().setSelectedSlot(0);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Vec3 feet = ground(level, saloon != null ? local(new Vec3(6.0, 1, -6.0)) : c);
                MARKS.put("open_feet", feet);
                // Over whichever shoulder has a clear line to him and to the saloon doors past him (a hitching post or
                // a lamp can stand right where the lens wants to be)
                Vec3 head = feet.add(0, 1.62, 0);
                Vec3 toward = flat((saloon != null ? local(new Vec3(6.0, 0, 4)) : c.add(d.scale(30))).subtract(feet));
                Vec3 right = new Vec3(-toward.z, 0, toward.x);
                double side = 1;
                for (double tryside : new double[] {1, -1, 0.4, -0.4}) {
                    Vec3 eye = head.subtract(toward.scale(3.6)).add(right.scale(1.5 * tryside)).add(0, 0.45, 0);
                    if (clear(level, eye, head) && clear(level, eye, head.add(toward.scale(9)))) {
                        side = tryside;
                        break;
                    }
                }
                MARKS.put("open_side", new Vec3(side, 0, 0));
                MARKS.put("open_toward", toward);
                tpFeet(server, feet, head.add(toward.scale(12)));
            });
        };
        shot.detached = true;
        // He faces the saloon doors, then fires into the sky over its roof
        shot.aim = p -> saloon != null ? local(new Vec3(6.0, 16, 10)) : mark("open_feet").add(d.scale(50)).add(0, 20, 0);
        shot.rig = (s, p) -> {
            Vec3 head = mark("open_feet").add(0, 1.62, 0);
            Vec3 toward = MARKS.getOrDefault("open_toward", d);
            Vec3 right = new Vec3(-toward.z, 0, toward.x).scale(mark("open_side").x);
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = head.subtract(toward.scale(Mth.lerp(k, 3.6, 2.6))).add(right.scale(Mth.lerp(k, 1.5, 1.1))).add(0, 0.45, 0);
            return new Pose(eye, head.add(toward.scale(12)).add(0, Mth.lerp(k, 2.6, 1.4), 0), 0, (float) Mth.lerp(k, 70, 60));
        };
        shot.at(1.0 + 1.7, () -> fire(server));
        shot.wrap = () -> {
            run(server, "clear @a");
            spectator(server);
        };
        return shot;
    }

    // 2. The town from the air at sunrise, coming down toward the square
    private static Shot townAerial(MinecraftServer server, Vec3 c, Vec3 d, Vec3 across) {
        Shot shot = new Shot(2, "town_aerial", 3.5);
        shot.setup = () -> {
            run(server, "time set 0");
            tpEye(server, c.subtract(d.scale(95)).add(across.scale(45)).add(0, 70, 0), c);
        };
        shot.settle = 100;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = lerp(k, c.subtract(d.scale(95)).add(across.scale(45)).add(0, 70, 0), c.subtract(d.scale(38)).add(across.scale(16)).add(0, 26, 0));
            return new Pose(eye, lerp(k, c.add(d.scale(10)), c.add(d.scale(26)).add(0, 3, 0)), (float) Mth.lerp(k, -4, 0), 70);
        };
        return shot;
    }

    // 3. Down the main street at a walk, boardwalk height
    private static Shot mainStreet(MinecraftServer server, Vec3 c, Vec3 d, Vec3 across) {
        Shot shot = new Shot(3, "main_street", 2.5);
        shot.setup = () -> {
            run(server, "time set 1200");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                MARKS.put("street_a", ground(level, c.add(d.scale(TownLayout.MID + 2)).add(across.scale(2.5))).add(0, 2.0, 0));
                MARKS.put("street_b", ground(level, c.add(d.scale(TownLayout.MID + 16)).add(across.scale(1.5))).add(0, 2.2, 0));
                tpEye(server, mark("street_a"), c.add(d.scale(60)));
            });
        };
        shot.rig = (s, p) -> {
            double k = s / (shot.seconds + HANDLES);
            Vec3 eye = lerp(k, mark("street_a"), mark("street_b"));
            return new Pose(eye, eye.add(d.scale(30)).add(across.scale(-2)).add(0, -0.6, 0), 0, 74);
        };
        return shot;
    }

    // 4. Through the swinging doors into the saloon
    private static Shot saloonDoors(MinecraftServer server) {
        Shot shot = new Shot(4, "saloon_doors", 2.5);
        shot.setup = () -> {
            run(server, "time set 4000");
            tpEye(server, local(new Vec3(6.0, 2.62, -3.5)), local(new Vec3(6.0, 2.2, 12)));
        };
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = lerp(k, local(new Vec3(6.0, 2.62, -3.5)), local(new Vec3(6.0, 2.55, 6.0)));
            return new Pose(eye, local(new Vec3(6.0, 2.1, 12.0)), 0, 72);
        };
        shot.at(1.0 + 0.6, () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            for (Vec3 door : List.of(new Vec3(5.5, 1.5, 3.5), new Vec3(6.5, 1.5, 3.5))) {
                openDoor(level, BlockPos.containing(local(door)));
            }
        }));
        shot.wrap = () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            for (Vec3 door : List.of(new Vec3(5.5, 1.5, 3.5), new Vec3(6.5, 1.5, 3.5))) {
                closeDoor(level, BlockPos.containing(local(door)));
            }
        });
        return shot;
    }

    // 5. The barmaid behind the bar, the camera sliding past her
    private static Shot barmaid(MinecraftServer server) {
        Shot shot = new Shot(5, "barmaid", 1.5);
        Vec3 front = localDir(0, -1), side = localDir(1, 0);
        shot.setup = () -> {
            run(server, "time set 4000");
            server.execute(() -> {
                Vec3 inside = local(new Vec3(6, 1.5, 9));
                missBelle(server);
                server.overworld().getEntitiesOfClass(Barmaid.class, new AABB(inside, inside).inflate(8)).stream().findFirst().ifPresent(b -> {
                    SUBJECTS.put("barmaid", b.getUUID());
                    MARKS.put("barmaid", b.position());
                    // She stands still, turned to the room (and the lens), while the camera slides past
                    float yaw = yawOf(front);
                    b.setNoAi(true);
                    b.setYRot(yaw);
                    b.setYBodyRot(yaw);
                    b.setYHeadRot(yaw);
                });
                tpEye(server, local(new Vec3(5, 2.6, 7)), local(new Vec3(5, 2.6, 10)));
            });
        };
        // Her eyes follow the camera across
        shot.at(0, () -> JOBS.add(() -> {
            if (server.overworld().getEntity(SUBJECTS.getOrDefault("barmaid", new UUID(0, 0))) instanceof Barmaid b) {
                double k = ease(rollTicks / (20 * (shot.seconds + HANDLES)));
                Vec3 eye = mark("barmaid").add(front.scale(2.3)).add(side.scale(Mth.lerp(k, -1.8, 1.8)));
                b.setYHeadRot(yawOf(eye.subtract(b.position())));
                b.setYBodyRot(yawOf(front));
            }
        }));
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 her = seen("barmaid", p);
            Vec3 eye = her.add(front.scale(2.3)).add(side.scale(Mth.lerp(k, -1.8, 1.8))).add(0, 1.75, 0);
            return new Pose(eye, her.add(0, 1.5, 0), 0, 50);
        };
        shot.wrap = () -> server.execute(() -> {
            if (server.overworld().getEntity(SUBJECTS.getOrDefault("barmaid", new UUID(0, 0))) instanceof Barmaid b) {
                b.setNoAi(false);
            }
        });
        return shot;
    }

    /** The saloon's barmaid is Miss Belle, whoever the town drew (behind the bar and dealing the blackjack hand). */
    private static void missBelle(MinecraftServer server) {
        Vec3 inside = local(new Vec3(6, 1.5, 9));
        server.overworld().getEntitiesOfClass(Barmaid.class, new AABB(inside, inside).inflate(16))
                .forEach(b -> b.setHostess(com.hollowfrontier.content.frontier.saloon.Hostess.BELLE));
    }

    // 6. A hand of blackjack: dealt, then the dealer turns her card
    private static Shot blackjack(Minecraft mc, MinecraftServer server) {
        Shot shot = new Shot(6, "blackjack", 2.0);
        BlockPos table = BlockPos.containing(local(new Vec3(9.5, 1.5, 5.5)));
        shot.setup = () -> {
            mc.options.guiScale().set(4); // the table big in frame (cut.ps1 crops in on it)
            run(server, "time set 4000");
            run(server, "gamemode survival @a");
            run(server, "clear @a");
            run(server, "give @a hollowfrontier:gold_coin 4");
            server.execute(() -> {
                missBelle(server);
                ServerPlayer player = player(server);
                player.setData(ModAttachments.WANTED.get(), com.hollowfrontier.content.frontier.law.Wanted.NONE);
                Vec3 feet = local(new Vec3(7.6, 1.0, 4.4));
                tpFeet(server, feet, Vec3.atCenterOf(table));
            });
        };
        shot.at(0.6, () -> server.execute(() -> {
            ServerPlayer player = player(server);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(table).relative(Direction.UP, 0.5), Direction.UP, table, false);
            server.overworld().getBlockState(table).useWithoutItem(server.overworld(), player, hit);
            if (player.containerMenu instanceof BlackjackMenu menu) {
                menu.clickMenuButton(player, BlackjackMenu.DEAL);
                menu.broadcastChanges();
            }
        }));
        shot.at(2.0, () -> server.execute(() -> {
            ServerPlayer player = player(server);
            if (player.containerMenu instanceof BlackjackMenu menu) {
                menu.clickMenuButton(player, BlackjackMenu.STAND);
                menu.broadcastChanges();
            }
        }));
        shot.wrap = () -> {
            server.execute(() -> player(server).closeContainer());
            mc.options.guiScale().set(0);
            spectator(server);
        };
        return shot;
    }

    // 7. Pushing in on the saloon's bounty board; a poster gets taken
    private static Shot bountyBoard(MinecraftServer server) {
        Shot shot = new Shot(7, "bounty_board", 2.5);
        Vec3 out = localDir(-1, 0); // the board hangs on the east wall, facing west
        shot.setup = () -> {
            run(server, "time set 4000");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Vec3 near = local(new Vec3(11, 2, 5));
                level.getEntitiesOfClass(BountyBoardEntity.class, new AABB(near, near).inflate(3)).stream().findFirst().ifPresent(board -> {
                    MARKS.put("board", board.position());
                    postBoard(level, board, null);
                });
                Vec3 at = mark("board");
                tpEye(server, at.add(out.scale(3.4)), at);
            });
        };
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 at = mark("board");
            Vec3 eye = at.add(out.scale(Mth.lerp(k, 3.4, 1.7))).add(0, Mth.lerp(k, 0.2, 0.05), 0);
            return new Pose(eye, at, (float) Mth.lerp(k, 2, 0), 62);
        };
        shot.at(1.0 + 1.3, () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            Vec3 at = mark("board");
            level.getEntitiesOfClass(BountyBoardEntity.class, new AABB(at, at).inflate(1.5)).stream().findFirst()
                    .ifPresent(board -> postBoard(level, board, "Cole"));
        }));
        return shot;
    }

    // 15. The airship at sunset with the cowboy at the helm: already aloft over the town, climbing and driving on, the
    // camera flying level with it off its beam (side on), the town and the sunset beyond
    private static Shot airship(MinecraftServer server, Vec3 c, Vec3 d) {
        Shot shot = new Shot(15, "airship", 5.0);
        Vec3 across = new Vec3(-d.z, 0, d.x);
        shot.setup = () -> {
            run(server, "time set 12400");
            run(server, "kill @e[type=hollowfrontier:airship]");
            run(server, "gamemode creative @a");
            run(server, "clear @a");
            cowboy(server);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                // Starting over the square, 14 up, heading out along the main street
                Vec3 site = c.subtract(d.scale(10)).add(0, 14, 0);
                MARKS.put("airship", site);
                Airship ship = ModEntities.AIRSHIP.get().create(level, EntitySpawnReason.COMMAND);
                ship.snapTo(site.x, site.y, site.z, yawOf(d), 0);
                ship.setSteam(Airship.CAPACITY);
                ship.setFuel(Airship.COAL_TICKS * 4);
                level.addFreshEntity(ship);
                SUBJECTS.put("airship", ship.getUUID());
                tpEye(server, site.add(across.scale(14)).add(0, 3, 0), site);
            });
        };
        shot.during(20, () -> server.execute(() -> forEach(server.overworld(), Airship.class, mark("airship"), 10, ship -> {
            ServerPlayer player = player(server);
            player.getAbilities().flying = false;
            player.startRiding(ship, true, false);
        })));
        // The pilot's helm is read from their input: hold climb until the climb is done, and the throttle open throughout
        shot.during(30, () -> JOBS.add(() -> player(server).setLastClientInput(
                new net.minecraft.world.entity.player.Input(true, false, false, false, rollTicks < 20 * 3.5 || !rolling, false, false))));
        shot.settle = 70;
        shot.detached = true;
        // The pilot looks ahead along the heading (the ship steers by its rudder, not his look)
        shot.aim = p -> seen("airship", p, mark("airship")).add(d.scale(40)).add(0, 2, 0);
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 ship = seen("airship", p, mark("airship"));
            // Off its beam, a little below and behind (the envelope against the sky), easing round toward its quarter
            Vec3 eye = ship.add(across.scale(Mth.lerp(k, 14, 10.5))).subtract(d.scale(Mth.lerp(k, 2, 6))).add(0, Mth.lerp(k, 0.5, 2.5), 0);
            return new Pose(eye, ship.add(d.scale(1.5)).add(0, 2.2, 0), (float) Mth.lerp(k, -2, 2), 66);
        };
        shot.wrap = () -> {
            JOBS.clear();
            run(server, "ride @a dismount");
            run(server, "kill @e[type=hollowfrontier:airship]");
            run(server, "clear @a");
            spectator(server);
        };
        return shot;
    }

    // 8. Three riders at a gallop across the badlands at golden hour, the camera running alongside
    private static Shot gallop(MinecraftServer server) {
        Shot shot = new Shot(8, "gallop", 4.5);
        Vec3 start = mark("range"), dir = mark("range_dir");
        Vec3 side = MARKS.getOrDefault("range_side", new Vec3(-dir.z, 0, dir.x));
        shot.setup = () -> {
            run(server, "time set 12600");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                clear(level, start, 60);
                clearCorridor(level, start, dir, side);
                float yaw = yawOf(dir);
                List<UUID> horses = new ArrayList<>();
                AbstractOutlaw lead = mounted(level, ModEntities.OUTLAW_BOSS.get(), start, yaw, new ItemStack(ModItems.REVOLVER.get()));
                horses.add(lead.getVehicle().getUUID());
                horses.add(mounted(level, ModEntities.OUTLAW_GUNSLINGER.get(), ground(level, start.subtract(dir.scale(4)).add(side.scale(-3))), yaw,
                        new ItemStack(ModItems.PLAINS_RIFLE.get())).getVehicle().getUUID());
                horses.add(mounted(level, ModEntities.OUTLAW.get(), ground(level, start.subtract(dir.scale(6)).add(side.scale(2.5))), yaw,
                        new ItemStack(ModItems.SHOTGUN.get())).getVehicle().getUUID());
                SUBJECTS.put("lead", lead.getVehicle().getUUID());
                HORSES.clear();
                HORSES.addAll(horses);
                tpEye(server, start.add(side.scale(8)).add(0, 1.6, 0), start);
            });
        };
        shot.rig = (s, p) -> {
            Vec3 lead = seen("lead", p, start);
            double k = s / (shot.seconds + HANDLES);
            Vec3 eye = lead.add(side.scale(Mth.lerp(k, 6.0, 4.6))).add(dir.scale(Mth.lerp(k, 2.0, -1.0))).add(0, Mth.lerp(k, 2.8, 3.3), 0);
            return new Pose(eye, lead.add(dir.scale(1.5)).add(0, 1.6, 0), 0, 66);
        };
        shot.at(0, () -> JOBS.add(new Runnable() {
            int tick;

            @Override
            public void run() {
                tick++;
                ServerLevel level = server.overworld();
                double speed = Math.min(1, tick / 14.0) * 0.6;
                float yaw = yawOf(dir);
                for (UUID id : HORSES) {
                    if (level.getEntity(id) instanceof LivingEntity horse) {
                        Vec3 to = horse.position().add(dir.scale(speed));
                        double y = to.y + Mth.clamp(ground(level, to).y - to.y, -0.4, 0.55);
                        horse.setPos(to.x, y, to.z);
                        horse.setYRot(yaw);
                        horse.setYBodyRot(yaw);
                        horse.setYHeadRot(yaw);
                    }
                }
            }
        }));
        shot.wrap = () -> server.execute(() -> clear(server.overworld(), start, 160));
        return shot;
    }

    private static final List<UUID> HORSES = new CopyOnWriteArrayList<>();

    private static Vec3 camp(double x, double y, double z) {
        return mark("camp").add(x, y, z);
    }

    // 9. The revolver loaded chamber by chamber by the outlaws' fire
    private static Shot revolverLoad(MinecraftServer server) {
        Shot shot = new Shot(9, "revolver_load", 2.0);
        shot.setup = () -> {
            run(server, "time set 12200");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Vec3 o = mark("camp");
                int x = (int) o.x, y = (int) o.y, z = (int) o.z;
                clear(level, o.add(7, 0, 7), 30);
                // The pad is laid in the land's own top (red sand in the badlands), not bare dirt. Only natural ground
                // counts: on a re-shoot the last camp is still there, and its lit campfire laid a pad of campfires
                var top = level.getBlockState(new BlockPos(x + 7, y, z + 7));
                String surface = top.is(BlockTags.SAND) || top.is(BlockTags.DIRT) || top.is(BlockTags.TERRACOTTA)
                        ? net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(top.getBlock()).toString() : "minecraft:red_sand";
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 3, y + 1, z - 12, x + 17, y + 12, z + 17));
                run(server, String.format("fill %d %d %d %d %d %d %s", x - 3, y, z - 12, x + 17, y, z + 17, surface));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:dirt", x - 3, y - 4, z - 12, x + 17, y - 1, z + 17));
                run(server, String.format("place template hollowfrontier:outlaw_camp %d %d %d", x, y, z));
            });
            handGun(server, "revolver", true);
            Minecraft.getInstance().player.getInventory().setSelectedSlot(0);
            tpEye(server, camp(7.5, 2.62, -6), camp(7.5, 1.2, 7.5));
        };
        shot.settle = 80;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = camp(7.5 + 0.6 * k, 2.62, -6 + 0.5 * k);
            return new Pose(eye, camp(7.5, 1.0 + 0.6 * k, 7.5), (float) Mth.lerp(k, 3, 0), 68);
        };
        shot.at(0.3, () -> server.execute(() -> {
            ServerPlayer player = player(server);
            ItemStack gun = player.getMainHandItem();
            GunItem.setAmmo(gun, 0);
            GunItem.setReloadStart(gun, player.level().getGameTime());
        }));
        return shot;
    }

    // 10. The shootout, through the cowboy's own eyes: he fires into the camp, the gang returns fire through the smoke
    private static Shot shootout(MinecraftServer server) {
        Shot shot = new Shot(10, "shootout", 4.5);
        shot.setup = () -> {
            run(server, "time set 12200");
            handGun(server, "revolver", false);
            cowboy(server);
            Minecraft.getInstance().player.getInventory().setSelectedSlot(0);
            run(server, "gamemode survival @a");
            run(server, "effect give @a minecraft:resistance infinite 4 true");
            run(server, "effect give @a minecraft:regeneration infinite 4 true");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                List<AbstractOutlaw> gang = level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(camp(-4, -2, -4), camp(19, 8, 19)));
                HollowFrontier.LOGGER.info("[Trailer] camp gang: {}", gang.size());
                for (int i = 0; i < gang.size(); i++) {
                    SUBJECTS.put("outlaw" + i, gang.get(i).getUUID());
                }
                tpFeet(server, camp(7.5, 1, -5), camp(7.5, 1.5, 7.5));
            });
        };
        shot.settle = 40;
        // His own eyes, standing (so he doesn't fall), swinging from one of the gang to the next with a little sway;
        // the server fires along where he looks
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = camp(7.5 + 0.3 * Math.sin(s * 1.3), 2.62, -5 + 0.6 * k);
            Vec3 target = aliveOutlaw(p, camp(7.5, 0.4, 7.5)).add(0, 1.1, 0);
            return new Pose(eye, lerp(0.85, eye.add(0, -0.3, 8), target), (float) (0.6 * Math.sin(s * 2.1)), 70);
        };
        for (double t : new double[] {1.2, 1.9, 2.6, 3.4, 4.1, 4.8}) {
            shot.at(t, () -> fire(server));
        }
        shot.wrap = () -> {
            run(server, "effect clear @a");
            run(server, "clear @a");
            spectator(server);
        };
        return shot;
    }

    // 11. A stick of dynamite lobbed at the camera
    private static Shot dynamite(MinecraftServer server) {
        Shot shot = new Shot(11, "dynamite", 2.5);
        shot.setup = () -> {
            run(server, "time set 12300");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(camp(-4, -2, -4), camp(19, 8, 19))).forEach(Entity::discard);
                Vec3 at = camp(7.5, 1, 5.5);
                float yaw = yawOf(new Vec3(0, 0, -1));
                AbstractOutlaw thrower = staged(level, ModEntities.OUTLAW_GUNSLINGER.get(), at, yaw, new ItemStack(ModItems.DYNAMITE.get()), GunWielder.GunPose.THROWING);
                SUBJECTS.put("thrower", thrower.getUUID());
                tpEye(server, camp(9.5, 2.4, -4.0), at);
            });
        };
        // Back and off to the side: the thrower on the left of frame, where the stick lands (and blows) on the right
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            return new Pose(camp(Mth.lerp(k, 9.5, 9.0), Mth.lerp(k, 2.4, 2.1), Mth.lerp(k, -4.0, -3.4)), camp(Mth.lerp(k, 6.6, 5.6), 1.4, Mth.lerp(k, 4.6, 4.0)),
                    (float) Mth.lerp(k, -2, 1), 74);
        };
        shot.at(1.2, () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            if (level.getEntity(SUBJECTS.getOrDefault("thrower", new UUID(0, 0))) instanceof AbstractOutlaw thrower) {
                DynamiteEntity stick = new DynamiteEntity(level, thrower, new ItemStack(ModItems.DYNAMITE.get()));
                Vec3 from = thrower.position().add(0, 1.6, 0), to = camp(4.5, 1, 3.5);
                int flight = 16;
                stick.setPos(from.x, from.y, from.z);
                stick.setDeltaMovement((to.x - from.x) / flight, 0.32, (to.z - from.z) / flight);
                stick.setFuse(30);
                level.addFreshEntity(stick);
            }
        }));
        return shot;
    }

    // 13. The boiler fired up at dusk, the camera sliding along the steam line
    private static Shot boiler(MinecraftServer server) {
        Shot shot = new Shot(13, "boiler", 1.5);
        Vec3 b = new Vec3(baseX, Y, baseZ);
        shot.setup = () -> {
            run(server, "time set 13600");
            server.execute(() -> {
                if (server.overworld().getBlockEntity(BlockPos.containing(b)) instanceof BoilerBlockEntity boiler) {
                    boiler.waterTank().add(FluidResource.of(Fluids.WATER), BoilerBlockEntity.WATER_CAPACITY);
                    boiler.setItem(0, new ItemStack(Items.COAL_BLOCK));
                }
            });
            tpEye(server, b.add(-1.0, 1.3, -2.6), b.add(0.5, 0.7, 0.5));
        };
        shot.settle = 80;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            return new Pose(b.add(Mth.lerp(k, -1.0, 4.6), Mth.lerp(k, 1.2, 1.5), -2.4), b.add(Mth.lerp(k, 0.5, 3.5), 0.8, 0.5), 0, 56);
        };
        return shot;
    }

    // 16. The town asleep: the banjo's gone quiet; the camera lifts to the stars
    private static Shot quietNight(MinecraftServer server, Vec3 c, Vec3 d) {
        Shot shot = new Shot(16, "quiet_night", 2.0);
        shot.setup = () -> {
            night(server);
            server.execute(() -> {
                MARKS.put("night_eye", ground(server.overworld(), c.add(d.scale(TownLayout.MID + 6))).add(0, 1.2, 0));
                tpEye(server, mark("night_eye"), c.add(d.scale(40)));
            });
        };
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = mark("night_eye").add(d.scale(1.5 * k)).add(0, 0.3 * k, 0);
            return new Pose(eye, lerp(k, eye.add(d.scale(30)).add(0, 3, 0), eye.add(d.scale(14)).add(0, 26, 0)), 0, 70);
        };
        return shot;
    }

    // 17. The hinge into the Grays' part: a cow in the pasture at dusk, a saucer roaring in over the lens to stop above
    // it, the beam lighting the grass, and the cow drawn up into the belly as the camera tilts after it; then it's gone
    private static final double ABDUCT_ARRIVE = 1.0; // seconds into the roll the saucer is sent in
    private static final double ABDUCT_RUN = 30.0;   // blocks it comes in from (passing over the camera)
    private static final double ABDUCT_HEIGHT = 11.0; // over the cow (lower than a wild one, so the lift is quick)

    private static Shot abduction(MinecraftServer server, Vec3 c) {
        Shot shot = new Shot(17, "abduction", 8.0);
        List<BlockPos> lights = new ArrayList<>();
        shot.setup = () -> {
            spectator(server);
            night(server);
            run(server, "time set 13300"); // the last of the dusk: the hull and the beam read against a deep blue sky
            // Nothing else in the sky over the town (an ambient airship, a craft left by an earlier take)
            for (String craft : List.of("gray_saucer", "airship_sighting", "airship", "mothership", "gray_drone", "phlogiston_beam", "gray_probe")) {
                run(server, "kill @e[type=hollowfrontier:" + craft + "]");
            }
            run(server, "kill @e[type=minecraft:cow]");
            run(server, "kill @e[type=minecraft:item]"); // what the cows dropped, here and in earlier takes
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Vec3 at = pinned(level, PASTURE);
                if (at == null) {
                    at = pasture(level, c);
                }
                // The camera stands on the far side of the cow from the town (its lights behind the cow); the saucer
                // comes in from behind the camera, over it, heading for the town
                Vec3 back = flat(at.subtract(c));
                Vec3 side = new Vec3(-back.z, 0, back.x);
                MARKS.put("abduct_back", back);
                clearBox(level, at.add(back.scale(5)), 16, 16);
                at = ground(level, at);
                MARKS.put("cow", at);
                Entity cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
                cow.snapTo(at.x, at.y, at.z, yawOf(side), 0);
                level.addFreshEntity(cow);
                SUBJECTS.put("cow", cow.getUUID());
                // Two more grazing off to the side, for scale (the beam takes the one in its middle)
                for (int i = -1; i <= 1; i += 2) {
                    Vec3 o = ground(level, at.add(side.scale(6.5 * i)).subtract(back.scale(3)));
                    run(server, String.format(Locale.ROOT, "summon minecraft:cow %.2f %.2f %.2f {Rotation:[%.1ff,0f]}", o.x, o.y, o.z, yawOf(back) + 70 * i));
                }
                // Cows wander; these stand and graze until the beam has them (the beam lifts, it doesn't walk them)
                run(server, "effect give @e[type=minecraft:cow] minecraft:slowness infinite 255 true");
                tpEye(server, at.add(back.scale(10)).add(0, 0.7, 0), at.add(0, 2, 0));
            });
        };
        shot.settle = 100;
        // Leaves left hanging where trees were cleared decay and drop saplings: keep the grass clear of them
        shot.during(5, () -> JOBS.add(() -> forEach(server.overworld(), ItemEntity.class, mark("cow"), 64, Entity::discard)));
        shot.at(ABDUCT_ARRIVE, () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            Vec3 at = mark("cow");
            Vec3 in = mark("abduct_back").scale(-1);
            Vec3 stop = new Vec3(at.x, at.y + ABDUCT_HEIGHT, at.z);
            GraySaucer saucer = ModEntities.GRAY_SAUCER.get().create(level, EntitySpawnReason.EVENT);
            saucer.setPos(stop.subtract(in.scale(ABDUCT_RUN)));
            saucer.abduction(in, stop);
            level.addFreshEntity(saucer);
            SUBJECTS.put("saucer", saucer.getUUID());
            MARKS.put("saucer", stop);
        }));
        // While it beams: the column thick with light, motes streaming up it, a ring on the grass, and the ground lit
        shot.at(ABDUCT_ARRIVE, () -> JOBS.add(() -> {
            ServerLevel level = server.overworld();
            forEach(level, GraySaucer.class, mark("cow"), 40, saucer -> {
                if (!saucer.isBeaming()) {
                    if (!lights.isEmpty() && saucer.run() == GraySaucer.Run.LEAVE) {
                        lights.forEach(pos -> level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2));
                        lights.clear();
                    }
                    return;
                }
                Vec3 at = mark("cow");
                if (lights.isEmpty()) {
                    for (int y = 1; y < ABDUCT_HEIGHT - 1; y += 3) {
                        BlockPos pos = BlockPos.containing(at.x, at.y + y, at.z);
                        if (level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState(), 2);
                            lights.add(pos);
                        }
                    }
                }
                RandomSource random = level.getRandom();
                double top = saucer.getY(), height = top - at.y;
                // A hollow tube, thin enough that the cow shows through it
                for (int i = 0; i < 14; i++) {
                    double y = random.nextDouble();
                    double a = random.nextDouble() * Math.PI * 2, r = Mth.lerp(y, 2.3, 1.2) * (0.85 + 0.15 * random.nextDouble());
                    level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(i % 3 == 0 ? 0xFFFFFF : 0xC8FFD8, 1.4F),
                            at.x + Math.cos(a) * r, at.y + y * height, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = 1.2 + random.nextDouble() * 0.8;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x + Math.cos(a) * r, at.y + random.nextDouble() * 2, at.z + Math.sin(a) * r,
                            0, 0, 1, 0, 0.35);
                }
                for (int i = 0; i < 16; i++) {
                    double a = i / 16.0 * Math.PI * 2 + saucer.tickCount * 0.05;
                    level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xC8FFD8, 1.6F), at.x + Math.cos(a) * 2.4, at.y + 0.15, at.z + Math.sin(a) * 2.4, 1, 0, 0, 0, 0);
                }
            });
        }));
        // Low in the grass by the cow, looking up at the sky it comes out of; then pushing in and tilting up after the cow
        shot.rig = (s, p) -> {
            Vec3 at = mark("cow");
            Vec3 saucer = seen("saucer", p, at.add(0, ABDUCT_HEIGHT, 0));
            Vec3 cow = seen("cow", p, saucer); // once it's taken, the camera stays on the saucer as it goes
            double k = ease(s / (shot.seconds + HANDLES));
            double rise = ease(Mth.clamp((cow.y - at.y) / (ABDUCT_HEIGHT - 2), 0, 1));
            Vec3 back = mark("abduct_back");
            Vec3 side = new Vec3(-back.z, 0, back.x);
            Vec3 eye = at.add(back.scale(Mth.lerp(k, 10, 7))).add(side.scale(Mth.lerp(k, 1.5, 0.5))).add(0, Mth.lerp(k, 0.6, 1.0), 0);
            // Cow and saucer both in frame, the frame climbing with the cow to the belly
            Vec3 look = lerp(rise, at.add(0, 4.5, 0), cow.add(0, 0.7, 0).add(saucer).scale(0.5));
            return new Pose(eye, look, (float) Mth.lerp(k, -3, 2), (float) Mth.lerp(k, 78, 70));
        };
        shot.wrap = () -> {
            server.execute(() -> lights.forEach(pos -> server.overworld().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2)));
            run(server, "kill @e[type=hollowfrontier:gray_saucer]");
            run(server, "kill @e[type=minecraft:cow]");
            run(server, "kill @e[type=minecraft:item]");
        };
        return shot;
    }

    /** The ground at "x,z", or null for "search" (or anything unreadable). */
    private static Vec3 pinned(ServerLevel level, String xz) {
        String[] parts = xz.split(",");
        try {
            return ground(level, new Vec3(Double.parseDouble(parts[0].trim()), 0, Double.parseDouble(parts[1].trim())));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The flattest open grass round the town (no water, no trees, out from under the sky stage, the ground behind it for
     * the camera as flat), 40 to 90 blocks out from the square so the town sits in the background.
     */
    private static Vec3 pasture(ServerLevel level, Vec3 c) {
        BoundingBox bb = town != null ? town.getBoundingBox().inflatedBy(4) : null;
        Vec3 best = mark("field");
        double bestScore = Double.MAX_VALUE;
        for (int dx = -90; dx <= 90; dx += 6) {
            for (int dz = -90; dz <= 90; dz += 6) {
                Vec3 p = ground(level, c.add(dx, 0, dz));
                double dist = Math.sqrt(p.distanceToSqr(c.x, p.y, c.z));
                // Real grass (not the sky stage over the spawn, nor sand or stone)
                if (dist < 40 || dist > 90 || bb != null && bb.isInside(BlockPos.containing(p)) || p.y > Y - 50
                        || !level.getBlockState(BlockPos.containing(p).below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                Vec3 back = flat(p.subtract(c));
                // Best east of the town, so the camera looks west over the cow and the town into the last of the sunset
                double score = 30 * (1 - back.x);
                // The cow's patch and the strip back to where the camera stands
                for (int r = -6; r <= 14; r += 2) {
                    for (int s = -6; s <= 6; s += 3) {
                        Vec3 q = p.add(back.scale(r)).add(-back.z * s, 0, back.x * s);
                        double h = ground(level, q).y;
                        if (h > Y - 50) {
                            score += 100; // under the sky stage, which would hang in the shot
                            continue;
                        }
                        score += Math.min(Math.abs(h - p.y), 20) * 2 + (canopy(level, q) > h + 0.5 ? 4 : 0);
                        if (!level.getFluidState(BlockPos.containing(q.x, h - 1, q.z)).isEmpty()) {
                            score += 8;
                        }
                    }
                }
                if (score < bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
        }
        HollowFrontier.LOGGER.info("[Trailer] pasture at {} (roughness {})", best, bestScore);
        return best;
    }

    // 21. A Gray Warden, close
    private static Shot warden(MinecraftServer server, Vec3 c) {
        Shot shot = new Shot(21, "warden", 1.0);
        Vec3 field = mark("field");
        Vec3 toTown = flat(c.subtract(field));
        Vec3 side = new Vec3(-toTown.z, 0, toTown.x);
        shot.setup = () -> {
            night(server);
            run(server, "kill @e[type=hollowfrontier:gray_warden]");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Vec3 at = ground(level, field.add(side.scale(7)));
                MARKS.put("warden", at);
                run(server, String.format(Locale.ROOT, "summon hollowfrontier:gray_warden %.2f %.2f %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[%.1ff,0f]}",
                        at.x, at.y, at.z, yawOf(toTown)));
                tpEye(server, at.add(toTown.scale(3.2)).add(0, 1.9, 0), at.add(0, 1.7, 0));
            });
        };
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 at = mark("warden");
            return new Pose(at.add(toTown.scale(Mth.lerp(k, 3.4, 1.5))).add(side.scale(0.4)).add(0, Mth.lerp(k, 1.6, 1.85), 0), at.add(0, 1.8, 0), (float) Mth.lerp(k, -6, 0), 48);
        };
        shot.wrap = () -> run(server, "kill @e[type=hollowfrontier:gray_warden]");
        return shot;
    }

    // 22. The Mothership over the town square, its drones out, the death ray coming down in the street
    private static Shot mothership(MinecraftServer server, Vec3 c, Vec3 d) {
        Shot shot = new Shot(22, "mothership", 1.8);
        BlockPos beacon = BlockPos.containing(c);
        shot.setup = () -> {
            night(server);
            run(server, "clear @a");
            run(server, "gamemode survival @a");
            run(server, "effect give @a minecraft:resistance infinite 4 true");
            run(server, "effect give @a minecraft:fire_resistance infinite 0 true");
            run(server, String.format("setblock %d %d %d hollowfrontier:phlogiston_beacon[lit=true]", beacon.getX(), beacon.getY(), beacon.getZ()));
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Mothership ship = ModEntities.MOTHERSHIP.get().create(level, EntitySpawnReason.COMMAND);
                ship.snapTo(c.x, c.y + 16, c.z, 0, 0);
                ship.setAnchor(beacon);
                ship.setEndsTheLeash(false);
                ship.engage();
                level.addFreshEntity(ship);
                for (int i = 0; i < 4; i++) {
                    GrayDrone.launch(level, ship);
                }
                SUBJECTS.put("ship", ship.getUUID());
                MARKS.put("ship_eye", ground(level, c.add(d.scale(9))).add(0, 1.4, 0));
                MARKS.put("ray", ground(level, c.add(d.scale(6))));
                // The ship's quarry stands behind the camera, which is a dolly of its own (no arm in frame)
                tpFeet(server, ground(level, c.add(d.scale(14))), c.add(0, 16, 0));
            });
        };
        shot.detached = true;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = mark("ship_eye").subtract(d.scale(3 * k)).add(0, 0.2 * k, 0);
            return new Pose(eye, seen("ship", p, c.add(0, 16, 0)).add(0, -1.0, 0), (float) Mth.lerp(k, 4, -2), 84);
        };
        shot.at(1.0 + 0.3, () -> server.execute(() -> forEach(server.overworld(), Mothership.class, c, 80, ship -> ship.startRay(mark("ray")))));
        shot.wrap = () -> {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                forEach(level, Mothership.class, c, 120, Entity::discard);
                forEach(level, GrayDrone.class, c, 120, Entity::discard);
            });
            run(server, String.format("setblock %d %d %d minecraft:air", beacon.getX(), beacon.getY(), beacon.getZ()));
            run(server, "effect clear @a");
            spectator(server);
        };
        return shot;
    }

    // 12. The night raid: the gang rides into the street, the law shooting back; the camera backs off before them
    private static Shot nightRaid(MinecraftServer server) {
        Shot shot = new Shot(12, "night_raid", 5.0);
        shot.setup = () -> {
            night(server);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                raidSite = TownSite.of(level, town, RandomSource.create(7));
                Vec3 square = Vec3.atBottomCenterOf(raidSite.rally());
                Vec3 out = Vec3.atBottomCenterOf(raidSite.edge()).subtract(square);
                Vec3 dir = Math.abs(out.x) > Math.abs(out.z) ? new Vec3(Math.signum(out.x), 0, 0) : new Vec3(0, 0, Math.signum(out.z));
                MARKS.put("raid_dir", dir);
                // Out by the edge of town where the gang rides in, backing off up the street before them
                Vec3 edge = Vec3.atBottomCenterOf(raidSite.edge());
                // The gang gathers 20 past the town's bounds, which now take in the outskirts and fields (some 70 out
                // from the street's end): gather them 30 out, so they're through the town-limits arch and on the lens in the cut
                raidSite = new TownSite(raidSite.key(), raidSite.bounds(), raidSite.rally(), raidSite.edge(), BlockPos.containing(edge.add(dir.scale(30))),
                        raidSite.bell(), raidSite.vault(), raidSite.crackAt(), raidSite.workAt(), raidSite.size());
                MARKS.put("raid_a", ground(level, edge.subtract(dir.scale(5))).add(0, 2.0, 0));
                MARKS.put("raid_b", ground(level, edge.subtract(dir.scale(16))).add(0, 2.6, 0));
                var raid = TownRaids.get(level).start(level, raidSite, new RaidTimings(40, 600, 2400, 200), 6, true);
                HollowFrontier.LOGGER.info("[Trailer] raid {}: rally {}, in by {}", raid.isPresent() ? "started" : "REFUSED", raidSite.rally(), raidSite.edge());
                tpEye(server, mark("raid_a"), mark("raid_a").add(dir.scale(20)));
            });
        };
        shot.settle = 25;
        shot.waitForChunks = false; // the town is drawn already, and the gang won't wait
        shot.rollOverride = 20 * 14;
        shot.rig = (s, p) -> {
            double k = ease(s / 14.0);
            Vec3 dir = mark("raid_dir");
            Vec3 eye = lerp(k, mark("raid_a"), mark("raid_b"));
            return new Pose(eye, eye.add(dir.scale(24)).add(0, -0.6, 0), (float) (1.5 * Math.sin(s * 1.7)), 72);
        };
        shot.wrap = () -> server.execute(() -> {
            ServerLevel level = server.overworld();
            if (raidSite != null) {
                TownRaids.get(level).stop(level, raidSite.key());
            }
            Vec3 at = mark("raid_a");
            level.getEntitiesOfClass(Entity.class, new AABB(at, at).inflate(80, 30, 80), e -> e instanceof AbstractOutlaw || e instanceof Horse
                    || e instanceof ItemEntity || e instanceof DynamiteEntity).forEach(Entity::discard);
        });
        return shot;
    }

    // 18. The polar gateway waking, its beam going up
    private static Shot gatewayWake(MinecraftServer server) {
        Shot shot = new Shot(18, "gateway_wake", 3.0);
        Vec3 w = Vec3.atBottomCenterOf(well());
        shot.setup = () -> {
            run(server, "time set 12600");
            resetCrater(server);
            tpEye(server, w.add(5, 26, 8), w);
        };
        shot.during(30, () -> server.execute(() -> GatewayEffects.awaken(server.overworld(), well())));
        shot.settle = 30 + Math.max(20, GatewayEffects.WAKE_TICKS - 40);
        // High over the crater, looking down its throat at the well; rising and tilting up the beam as it fires
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = w.add(Mth.lerp(k, 5, 10), Mth.lerp(k, 26, 31), Mth.lerp(k, 8, 16));
            return new Pose(aboveGround(eye, 4), w.add(0, Mth.lerp(k, 0, 45), 0), 0, (float) Mth.lerp(k, 66, 74));
        };
        return shot;
    }

    // 19. Through the gateway and down Symmes' Hole, the player's own view (the game's camera)
    private static Shot theFall(Minecraft mc, MinecraftServer server) {
        Shot shot = new Shot(19, "the_fall", 2.0);
        shot.rollOverride = 20 * 11; // the charge, the flash, the shaft, out over the land: cut.ps1 picks the moment
        shot.setup = () -> {
            run(server, "time set 6000");
            resetCrater(server);
            server.execute(() -> PolarGateways.open(server.overworld(), well()));
            run(server, "gamemode survival @a");
            run(server, "clear @a");
            server.execute(() -> player(server).setData(ModAttachments.VISITED_HOLLOW_EARTH, false));
            BlockPos well = well();
            run(server, String.format("tp @a %d.5 %d %d.5 0 25", well.getX(), well.getY(), well.getZ()));
        };
        shot.settle = 10;
        shot.waitForChunks = false;
        shot.wrap = () -> {
            server.execute(() -> landed = player(server).blockPosition());
            spectator(server);
        };
        return shot;
    }

    // 20. The Crystal Steppes under the crust: a mammoth herd, the camera craning up to the inner sun's sky
    private static Shot hollowVista(MinecraftServer server) {
        Shot shot = new Shot(20, "hollow_vista", 2.5);
        String hollowTp = "execute in " + HollowEarth.LEVEL.identifier() + " run tp @a ";
        shot.setup = () -> {
            run(server, "gamemode spectator @a");
            server.execute(() -> {
                ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                BlockPos from = landed != null ? landed : new BlockPos(0, 80, 0);
                var found = level.findClosestBiome3d(b -> b.is(HollowBiomes.CRYSTAL_STEPPES), from, 6400, 32, 64);
                BlockPos at = found == null ? from : found.getFirst();
                Vec3 high = new Vec3(at.getX() + 0.5, HollowEarth.CEILING_LOWEST - 60, at.getZ() + 0.5);
                MARKS.put("steppe", high);
                run(server, String.format("execute in %s run forceload add %d %d %d %d", HollowEarth.LEVEL.identifier(), at.getX() - 64, at.getZ() - 64, at.getX() + 64, at.getZ() + 64));
                run(server, String.format(Locale.ROOT, hollowTp + "%.1f %.1f %.1f 180 30", high.x, high.y, high.z));
                HollowFrontier.LOGGER.info("[Trailer] steppes at {}", at);
            });
        };
        shot.during(GENERATE_TICKS, () -> server.execute(() -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            // Dry ground under the open air (no lakes, no trees): the first spot out from the middle whose top isn't water
            // and with no leaves or trunk in its column or in the box the camera cranes through and the herd stands in
            // (a jungle tree's canopy filled the first second, and hid the herd)
            Vec3 high = mark("steppe");
            Vec3 ground = null;
            for (int ring = 0; ring < 6 && ground == null; ring++) {
                for (int i = 0; i < Math.max(1, ring * 8) && ground == null; i++) {
                    double angle = i * Math.PI * 2 / Math.max(1, ring * 8);
                    Vec3 at = high.add(ring * 20 * Math.cos(angle), 0, ring * 20 * Math.sin(angle));
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(at.x, HollowEarth.CEILING_LOWEST - 20, at.z);
                    boolean tree = false;
                    while (pos.getY() > HollowEarth.SEA_LEVEL && (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.LEAVES)
                            || level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty())) {
                        tree |= level.getBlockState(pos).is(BlockTags.LEAVES);
                        pos.move(0, -1, 0);
                    }
                    tree |= level.getBlockState(pos).is(BlockTags.LOGS);
                    for (int dx = -4; dx <= 4 && !tree; dx += 2) {
                        for (int dz = -20; dz <= 16 && !tree; dz += 2) {
                            for (int dy = 1; dy <= 26 && !tree; dy += 2) {
                                BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                                tree = state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
                            }
                        }
                    }
                    if (!tree && level.getFluidState(pos).isEmpty() && pos.getY() > HollowEarth.SEA_LEVEL) {
                        ground = new Vec3(at.x, pos.getY() + 1, at.z);
                    }
                }
            }
            if (ground == null) {
                ground = high.subtract(0, 40, 0);
            }
            MARKS.put("steppe", ground);
            // The heightmap is the shell's top here, not the land under the crust: earlier shoots stood their herds up
            // there, out of every shot
            level.getEntitiesOfClass(Entity.class, new AABB(ground.x - 200, HollowEarth.CEILING_LOWEST, ground.z - 200, ground.x + 200, level.getMaxY() + 2, ground.z + 200),
                    e -> e.getType() == ModEntities.WOOLLY_MAMMOTH.get()).forEach(Entity::discard);
            for (int i = 0; i < 5; i++) {
                Vec3 m = ground.add(-6 + i * 3.5, 0, -12 - (i % 2) * 4);
                run(server, String.format(Locale.ROOT, "execute in %s run summon hollowfrontier:woolly_mammoth %.1f %d %.1f {PersistenceRequired:1b}",
                        HollowEarth.LEVEL.identifier(), m.x, hollowTop(level, Mth.floor(m.x), Mth.floor(m.z), false), m.z));
            }
        }));
        shot.settle = GENERATE_TICKS + 60;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 at = mark("steppe");
            Vec3 eye = at.add(0, Mth.lerp(k, 2.5, 24), Mth.lerp(k, 6, 14));
            return new Pose(eye, lerp(k, at.add(0, 2, -14), at.add(0, 34, -90)), 0, 74);
        };
        return shot;
    }

    // ---------------------------------------------------------------- the Hollow's sites

    /** Where a site's camera is, given its piece (to turn template points into the world), its box and the roll. */
    @FunctionalInterface
    interface SiteRig {
        Pose at(PoolElementStructurePiece piece, BoundingBox box, double k, double s);
    }

    private static final Map<String, PoolElementStructurePiece> SITES = new ConcurrentHashMap<>();
    private static final Map<String, BoundingBox> SITE_BOXES = new ConcurrentHashMap<>();

    /**
     * A site as it generated in the Hollow: found from where the player landed (plus {@code offset}, so two searches
     * don't find the same one), hung over while it generates, then filmed by {@code rig}, kept clear of the hills.
     */
    private static Shot hollowSite(MinecraftServer server, int number, String name, double seconds,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key, BlockPos offset, SiteRig rig) {
        Shot shot = new Shot(number, name, seconds);
        String inHollow = "execute in " + HollowEarth.LEVEL.identifier() + " run ";
        shot.setup = () -> {
            run(server, "gamemode spectator @a");
            server.execute(() -> {
                ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
                BlockPos from = (landed != null ? landed : new BlockPos(0, 80, 0)).offset(offset);
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), from, 200, false);
                if (found == null) {
                    HollowFrontier.LOGGER.warn("[Trailer] no {} near {}", key.identifier(), from);
                    return;
                }
                BlockPos at = found.getFirst();
                MARKS.put(name + "_search", Vec3.atCenterOf(at));
                run(server, String.format("%sforceload add %d %d %d %d", inHollow, at.getX() - 80, at.getZ() - 80, at.getX() + 80, at.getZ() + 80));
                run(server, String.format(Locale.ROOT, "%stp @a %d.5 %d %d.5 0 90", inHollow, at.getX(), HollowEarth.CEILING_LOWEST - 40, at.getZ()));
                HollowFrontier.LOGGER.info("[Trailer] {} at {}", key.identifier(), at);
            });
        };
        shot.during(GENERATE_TICKS, () -> server.execute(() -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            Vec3 search = MARKS.get(name + "_search");
            if (search == null) {
                return;
            }
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key).value();
            StructureStart start = level.getChunk(Mth.floor(search.x) >> 4, Mth.floor(search.z) >> 4).getStartForStructure(structure);
            if (start == null || !start.isValid() || !(start.getPieces().getFirst() instanceof PoolElementStructurePiece piece)) {
                HollowFrontier.LOGGER.warn("[Trailer] the {} at {} didn't generate", key.identifier(), search);
                return;
            }
            SITES.put(name, piece);
            SITE_BOXES.put(name, piece.getBoundingBox());
            level.getEntitiesOfClass(ItemEntity.class, AABB.of(piece.getBoundingBox()).inflate(40)).forEach(Entity::discard);
            HollowFrontier.LOGGER.info("[Trailer] {}: {} at {} turned {}", name, piece.getElement(), piece.getPosition(), piece.getRotation());
            Pose first = rig.at(piece, piece.getBoundingBox(), 0, 0);
            run(server, String.format(Locale.ROOT, "%stp @a %.2f %.2f %.2f", inHollow, first.eye().x, first.eye().y - 1.62, first.eye().z));
        }));
        shot.settle = GENERATE_TICKS + 80;
        // The server can be busy a good while generating a site's land: wait for it to have found the piece
        shot.ready = () -> SITES.containsKey(name);
        shot.maxWait = 20 * 120;
        shot.rig = (s, p) -> {
            PoolElementStructurePiece piece = SITES.get(name);
            Vec3 search = MARKS.getOrDefault(name + "_search", Vec3.ZERO);
            if (piece == null) {
                return new Pose(search.add(0, 30, 30), search, 0, 70);
            }
            double k = ease(s / (shot.seconds + HANDLES));
            Pose pose = rig.at(piece, SITE_BOXES.get(name), k, s);
            return new Pose(aboveGround(pose.eye(), 2.5), pose.look(), pose.roll(), pose.fov());
        };
        return shot;
    }

    /** Whether nothing solid stands between two points (on the server). */
    private static boolean clear(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()
                == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    /** At least {@code clearance} over the ground (on the client, where the camera is) so a hill never swallows the lens. */
    private static Vec3 aboveGround(Vec3 eye, double clearance) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return eye;
        }
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(eye.x), Mth.floor(eye.z));
        return eye.y < ground + clearance ? new Vec3(eye.x, ground + clearance, eye.z) : eye;
    }

    /** Round a site at a sweep: {@code from} to {@code to} degrees, {@code pad} out past its edge, high to low, eyes on its middle. */
    private static SiteRig orbit(double from, double to, double pad, double high, double low, double lookUp, float fov) {
        return (piece, box, k, s) -> {
            double cx = (box.minX() + box.maxX() + 1) / 2.0, cz = (box.minZ() + box.maxZ() + 1) / 2.0, floor = piece.getPosition().getY();
            double r = Math.max(box.getXSpan(), box.getZSpan()) / 2.0 + pad;
            double a = Math.toRadians(Mth.lerp(k, from, to));
            Vec3 eye = new Vec3(cx + r * Math.cos(a), floor + Mth.lerp(k, high, low), cz + r * Math.sin(a));
            return new Pose(eye, new Vec3(cx, floor + lookUp, cz), (float) Mth.lerp(k, -3, 2), fov);
        };
    }

    // 25. The Crystal Ruins in the Concavian Highlands: sweeping low round the plaza, the whole tower and its spire
    private static Shot crystalRuins(MinecraftServer server) {
        return hollowSite(server, 25, "crystal_ruins", 4.0, com.hollowfrontier.worldgen.structure.ModStructures.CRYSTAL_RUINS, BlockPos.ZERO,
                orbit(200, 270, 8, 20, 7, 7, 70));
    }

    // 26. A Kindled site (henge, gate or temple, whichever the land made), circling down onto it
    private static Shot kindledSite(MinecraftServer server) {
        return hollowSite(server, 26, "kindled_site", 3.5, com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, new BlockPos(900, 0, 300),
                orbit(20, 85, 12, 22, 10, 4, 70));
    }

    // 29. A plate for the title card: high and far over the Hollow, drifting slowly round the Crystal Ruins
    private static Shot endHollow(MinecraftServer server) {
        return hollowSite(server, 29, "end_hollow", 6.0, com.hollowfrontier.worldgen.structure.ModStructures.CRYSTAL_RUINS, BlockPos.ZERO,
                orbit(300, 330, 70, 55, 48, 12, 72));
    }

    // 30. The cowboy's own saucer racing low over the Crystal Steppes and the mammoth herd, the camera chasing it
    private static Shot saucerFlight(MinecraftServer server) {
        Shot shot = new Shot(30, "saucer_flight", 4.0);
        String inHollow = "execute in " + HollowEarth.LEVEL.identifier() + " run ";
        shot.setup = () -> {
            run(server, "gamemode creative @a");
            run(server, "clear @a");
            cowboy(server);
            server.execute(() -> {
                ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                Vec3 steppe = MARKS.get("steppe");
                if (steppe == null) {
                    BlockPos from = landed != null ? landed : new BlockPos(0, 80, 0);
                    var found = level.findClosestBiome3d(b -> b.is(HollowBiomes.CRYSTAL_STEPPES), from, 6400, 32, 64);
                    BlockPos at = found == null ? from : found.getFirst();
                    run(server, String.format("%sforceload add %d %d %d %d", inHollow, at.getX() - 64, at.getZ() - 64, at.getX() + 64, at.getZ() + 64));
                    steppe = new Vec3(at.getX() + 0.5, hollowTop(level, at.getX(), at.getZ(), false), at.getZ() + 0.5);
                }
                // The heading with the lowest land and trees along the run (a jungle's edge swallowed the chase camera), and
                // high enough over it all; in from 50 back, so it's over the herd (if the vista left one) early in the roll
                Vec3 dir = null;
                int top = Integer.MAX_VALUE;
                for (int i = 0; i < 8; i++) {
                    Vec3 tryDir = Vec3.directionFromRotation(0, i * 45);
                    int highest = Integer.MIN_VALUE;
                    for (int step = -50; step <= 130; step += 10) {
                        Vec3 at = steppe.add(tryDir.scale(step));
                        highest = Math.max(highest, hollowTop(level, Mth.floor(at.x), Mth.floor(at.z), true));
                    }
                    if (highest < top) {
                        top = highest;
                        dir = tryDir;
                    }
                }
                Vec3 start = steppe.subtract(dir.scale(50));
                start = new Vec3(start.x, Math.max(top + 12, steppe.y + 14), start.z);
                MARKS.put("saucer", start);
                MARKS.put("saucer_dir", dir);
                level.getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(start, start).inflate(200)).forEach(Entity::discard);
                var saucer = ModEntities.PROTOTYPE_SAUCER.get().create(level, EntitySpawnReason.COMMAND);
                saucer.snapTo(start.x, start.y, start.z, yawOf(dir), 0);
                saucer.outfit(com.hollowfrontier.content.ufo.SaucerFittings.STANDARD, com.hollowfrontier.content.ufo.PrototypeSaucer.MAX_CHARGE, BlockPos.containing(start));
                level.addFreshEntity(saucer);
                SUBJECTS.put("saucer", saucer.getUUID());
                run(server, String.format(Locale.ROOT, "%stp @a %.1f %.1f %.1f", inHollow, start.x, start.y + 1, start.z));
            });
        };
        shot.during(40, () -> server.execute(() -> {
            if (server.getLevel(HollowEarth.LEVEL).getEntity(SUBJECTS.getOrDefault("saucer", new UUID(0, 0))) instanceof com.hollowfrontier.content.ufo.PrototypeSaucer saucer) {
                ServerPlayer player = player(server);
                player.getAbilities().flying = false;
                player.startRiding(saucer, true, false);
            }
        }));
        // Throttle open from the roll (it hovers till then), holding its height
        shot.at(0, () -> JOBS.add(() -> player(server).setLastClientInput(
                new net.minecraft.world.entity.player.Input(true, false, false, false, false, false, false))));
        shot.settle = 80;
        shot.detached = true;
        // It faces where its pilot looks: dead ahead, level
        shot.aim = p -> seen("saucer", p, mark("saucer")).add(mark("saucer_dir").scale(40)).add(0, 1.6, 0);
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 dir = mark("saucer_dir"), side = new Vec3(-dir.z, 0, dir.x);
            Vec3 craft = seen("saucer", p, mark("saucer"));
            Vec3 eye = craft.subtract(dir.scale(Mth.lerp(k, 10, 7))).add(side.scale(Mth.lerp(k, 5, 3))).add(0, Mth.lerp(k, 3, 1.5), 0);
            return new Pose(eye, craft.add(dir.scale(6)).add(0, 0.5, 0), (float) Mth.lerp(k, -4, 3), 74);
        };
        shot.wrap = () -> {
            JOBS.clear();
            run(server, "ride @a dismount");
            run(server, "execute in " + HollowEarth.LEVEL.identifier() + " run kill @e[type=hollowfrontier:prototype_saucer]");
            run(server, "clear @a");
            spectator(server);
        };
        return shot;
    }

    /**
     * The top of the land under the crust at x, z (the heightmap there is the shell's top): the first block down from under
     * the crust that isn't open air, with trees counted or not.
     */
    private static int hollowTop(ServerLevel level, int x, int z, boolean trees) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, HollowEarth.CEILING_LOWEST - 20, z);
        while (pos.getY() > HollowEarth.SEA_LEVEL) {
            BlockState state = level.getBlockState(pos);
            boolean open = state.isAir() || !trees && (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS))
                    || state.canBeReplaced() && state.getFluidState().isEmpty();
            if (!open) {
                break;
            }
            pos.move(0, -1, 0);
        }
        return pos.getY() + 1;
    }

    /**
     * Server work a Hollow site's shot needs once its piece is found (a captive and his door, the Archon on his throne):
     * started the first time it's asked for, and ready once {@code task} has put {@code name + "_ready"} in the marks.
     */
    private static BooleanSupplier prepared(MinecraftServer server, String name, Runnable task) {
        boolean[] started = {false};
        return () -> {
            if (!SITES.containsKey(name)) {
                return false;
            }
            if (!started[0]) {
                started[0] = true;
                server.execute(task);
            }
            return MARKS.containsKey(name + "_ready");
        };
    }

    // 23. In a Gray pen: outside a cell, the iron door swings open and the captive walks out, free
    private static Shot captives(MinecraftServer server) {
        Shot shot = hollowSite(server, 23, "captives", 2.0, com.hollowfrontier.worldgen.structure.ModStructures.GRAY_PENS, new BlockPos(-600, 0, 900),
                orbit(0, 20, 6, 14, 12, 3, 70));
        shot.ready = prepared(server, "captives", () -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            PoolElementStructurePiece piece = SITES.get("captives");
            for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, AABB.of(piece.getBoundingBox()).inflate(2),
                    m -> com.hollowfrontier.content.hollow.gray.Captives.of(m) instanceof com.hollowfrontier.content.hollow.gray.Captive c && !c.table())) {
                BlockPos door = null;
                for (BlockPos pos : BlockPos.betweenClosed(mob.blockPosition().offset(-4, -1, -4), mob.blockPosition().offset(4, 1, 4))) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER && (door == null || pos.distSqr(mob.blockPosition()) < door.distSqr(mob.blockPosition()))) {
                        door = pos.immutable();
                    }
                }
                if (door != null) {
                    stageCell(server, mob, door, Vec3.atBottomCenterOf(door).subtract(mob.position()).multiply(1, 0, 1).normalize());
                    break;
                }
            }
            if (!MARKS.containsKey("cell_door")) {
                // Every man here was freed by an earlier shoot (they stay freed): a new one in a cell. The corridor side of
                // a cell door is the side its button is on.
                for (BlockPos pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
                        piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                        continue;
                    }
                    Direction normal = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    BlockPos door = pos.immutable();
                    BlockPos button = null;
                    for (BlockPos near : BlockPos.betweenClosed(door.offset(-2, 0, -2), door.offset(2, 1, 2))) {
                        if (level.getBlockState(near).is(BlockTags.BUTTONS)) {
                            button = near.immutable();
                        }
                    }
                    if (button == null) {
                        continue;
                    }
                    int sign = Integer.signum(normal.getAxis().choose(button.getX() - door.getX(), 0, button.getZ() - door.getZ()));
                    if (sign == 0) {
                        continue;
                    }
                    Vec3 out = Vec3.atLowerCornerOf(normal.getUnitVec3i()).scale(sign * normal.getAxisDirection().getStep());
                    Vec3 cell = Vec3.atBottomCenterOf(door).subtract(out.scale(2));
                    var mob = ModEntities.OUTLAW.get().create(level, EntitySpawnReason.COMMAND);
                    mob.snapTo(cell.x, cell.y, cell.z, yawOf(out), 0);
                    mob.setData(ModAttachments.CAPTIVE.get(), new com.hollowfrontier.content.hollow.gray.Captive(false, java.util.Optional.empty(), java.util.Optional.empty()));
                    level.addFreshEntity(mob);
                    com.hollowfrontier.content.hollow.gray.Captives.restrain(mob);
                    stageCell(server, mob, door, out);
                    HollowFrontier.LOGGER.info("[Trailer] a new captive in the cell at {}", door);
                    break;
                }
            }
            MARKS.put("captives_ready", Vec3.ZERO);
        });
        // Out in the corridor, eye height, looking through the door at him; drifting in a step as it opens
        shot.rig = (s, p) -> {
            Vec3 door = MARKS.get("cell_door"), out = MARKS.get("cell_out");
            if (door == null) {
                return new Pose(mark("captives_search").add(0, 30, 30), mark("captives_search"), 0, 70);
            }
            double k = ease(s / (shot.seconds + HANDLES));
            // A little to the side the door doesn't swing to, so it opens away from him (square on, it hid him; further
            // over, the wall filled the frame), the next cell's bars in the foreground
            Vec3 eye = door.add(out.scale(Mth.lerp(k, 3.4, 2.6))).add(out.yRot(90).scale(0.4)).add(0, 1.55, 0);
            return new Pose(eye, mark("captive").add(0, 1.4, 0), (float) Mth.lerp(k, 1.5, -1), 66);
        };
        shot.at(1.3, () -> server.execute(() -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            Vec3 door = MARKS.get("cell_door");
            if (door != null) {
                BlockPos pos = BlockPos.containing(door);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock block) {
                    block.setOpen(null, level, state, pos, true);
                }
            }
        }));
        shot.at(1.5, () -> server.execute(() -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            if (level.getEntity(SUBJECTS.getOrDefault("captive", new UUID(0, 0))) instanceof net.minecraft.world.entity.Mob mob && MARKS.containsKey("cell_door")) {
                com.hollowfrontier.content.hollow.gray.Captives.free(level, player(server), mob);
                // Walked out by hand: a freed man's own wandering kept him in the cell
                mob.setNoAi(true);
            }
        }));
        // From where he stood, through the doorway and a stride into the corridor, in 1.3 s
        shot.at(1.6, () -> {
            int from = rollTicks;
            JOBS.add(() -> {
                ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                if (level.getEntity(SUBJECTS.getOrDefault("captive", new UUID(0, 0))) instanceof net.minecraft.world.entity.Mob mob) {
                    Vec3 start = mark("captive"), door = mark("cell_door"), end = door.add(mark("cell_out").scale(1.6));
                    double k = Mth.clamp((rollTicks - from) / 26.0, 0, 1);
                    double split = start.distanceTo(door) / (start.distanceTo(door) + door.distanceTo(end));
                    Vec3 at = k < split ? lerp(k / split, start, door) : lerp((k - split) / (1 - split), door, end);
                    Vec3 heading = (k < split ? door.subtract(start) : end.subtract(door)).multiply(1, 0, 1);
                    float yaw = yawOf(heading);
                    mob.snapTo(at.x, start.y, at.z, yaw, 0);
                    mob.setYHeadRot(yaw);
                    mob.setYBodyRot(yaw);
                }
            });
        });
        shot.wrap = JOBS::clear;
        return shot;
    }

    /** The captive {@code mob} behind {@code door}, the corridor {@code out} from it: marked for the shot, the camera taken there. */
    private static void stageCell(MinecraftServer server, net.minecraft.world.entity.Mob mob, BlockPos door, Vec3 out) {
        Vec3 at = Vec3.atBottomCenterOf(door);
        SUBJECTS.put("captive", mob.getUUID());
        MARKS.put("cell_door", at);
        MARKS.put("cell_out", out);
        MARKS.put("captive", mob.position());
        run(server, String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f", HollowEarth.LEVEL.identifier(), at.x + out.x * 3, at.y, at.z + out.z * 3));
    }

    // 27. The Sun Temple: pushing in on the Archon on his throne as he wakes
    private static Shot archon(MinecraftServer server) {
        Shot shot = hollowSite(server, 27, "archon", 2.0, com.hollowfrontier.worldgen.structure.ModStructures.SUN_TEMPLE, new BlockPos(400, 0, -800),
                orbit(90, 110, 6, 16, 12, 6, 70));
        shot.ready = prepared(server, "archon", () -> {
            ServerLevel level = server.getLevel(HollowEarth.LEVEL);
            PoolElementStructurePiece piece = SITES.get("archon");
            level.getEntitiesOfClass(com.hollowfrontier.content.concavia.SunArchon.class, AABB.of(piece.getBoundingBox()).inflate(8)).stream().findFirst().ifPresent(archon -> {
                BlockPos throne = archon.throne() != null ? archon.throne() : archon.blockPosition();
                if (!archon.blockPosition().equals(throne)) {
                    archon.snapTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, archon.getYRot(), 0);
                }
                Vec3 at = Vec3.atBottomCenterOf(throne);
                Vec3 fwd = Vec3.directionFromRotation(0, archon.getYRot());
                SUBJECTS.put("archon", archon.getUUID());
                MARKS.put("archon", at);
                MARKS.put("archon_fwd", fwd);
                run(server, String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f", HollowEarth.LEVEL.identifier(), at.x + fwd.x * 9, at.y + 1, at.z + fwd.z * 9));
            });
            MARKS.put("archon_ready", Vec3.ZERO);
        });
        // Low in the hall before him, pushing in and down as he rises
        shot.rig = (s, p) -> {
            Vec3 at = MARKS.get("archon"), fwd = MARKS.get("archon_fwd");
            if (at == null) {
                return new Pose(mark("archon_search").add(0, 30, 30), mark("archon_search"), 0, 70);
            }
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = at.add(fwd.scale(Mth.lerp(k, 10, 6.5))).add(0, Mth.lerp(k, 2.8, 1.8), 0);
            return new Pose(eye, at.add(0, 2.6, 0), 0, (float) Mth.lerp(k, 66, 58));
        };
        shot.at(1.4, () -> server.execute(() -> {
            if (server.getLevel(HollowEarth.LEVEL).getEntity(SUBJECTS.getOrDefault("archon", new UUID(0, 0))) instanceof com.hollowfrontier.content.concavia.SunArchon archon) {
                archon.wake(null);
            }
        }));
        return shot;
    }

    // 31. After the title: Mr. Gray on his stool at the bar of an evening, Miss Belle behind it; he turns, slowly, to the lens
    private static Shot mrGray(MinecraftServer server) {
        Shot shot = new Shot(31, "mr_gray", 4.0);
        Vec3 front = localDir(0, -1), side = localDir(1, 0);
        shot.setup = () -> {
            run(server, "time set 13000");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                missBelle(server);
                Vec3 inside = local(new Vec3(6, 1.5, 9));
                level.getEntitiesOfClass(com.hollowfrontier.content.ufo.DisguisedGray.class, new AABB(inside, inside).inflate(20)).forEach(g -> {
                    g.stopRiding();
                    g.discard();
                });
                // The stool nearest Miss Belle (becomeRegular would take the first it found, by the window), facing her
                Vec3 belle = level.getEntitiesOfClass(Barmaid.class, new AABB(inside, inside).inflate(8)).stream().findFirst()
                        .map(Entity::position).orElse(local(new Vec3(6, 1, 9)));
                BlockPos stool = null;
                for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(belle).offset(-6, -2, -6), BlockPos.containing(belle).offset(6, 1, 6))) {
                    if (level.getBlockState(pos).getBlock() instanceof com.hollowfrontier.content.frontier.furniture.BarStoolBlock
                            && (stool == null || pos.distToCenterSqr(belle) < stool.distToCenterSqr(belle))) {
                        stool = pos.immutable();
                    }
                }
                Vec3 seat = stool != null ? Vec3.atBottomCenterOf(stool) : belle.add(front.scale(2));
                var gray = ModEntities.DISGUISED_GRAY.get().create(level, EntitySpawnReason.COMMAND);
                gray.snapTo(seat.x, seat.y, seat.z, yawOf(belle.subtract(seat)), 0);
                level.addFreshEntity(gray);
                gray.startVisit(0, BlockPos.containing(seat));
                if (stool != null) {
                    com.hollowfrontier.content.frontier.furniture.SeatEntity.sit(level, stool, gray);
                }
                gray.becomeRegular();
                gray.setNoAi(true);
                SUBJECTS.put("gray", gray.getUUID());
                MARKS.put("gray", gray.position());
                MARKS.put("belle", belle);
                tpEye(server, grayEye(0, side, front), gray.position().add(0, 1.2, 0));
            });
        };
        shot.during(10, () -> server.execute(() -> {
            if (server.overworld().getEntity(SUBJECTS.getOrDefault("gray", new UUID(0, 0))) instanceof com.hollowfrontier.content.ufo.DisguisedGray gray) {
                MARKS.put("gray", gray.position()); // on the stool now
            }
        }));
        shot.settle = 60;
        // Facing the bar, then the head turns to the camera and holds there
        shot.at(0, () -> JOBS.add(() -> {
            if (server.overworld().getEntity(SUBJECTS.getOrDefault("gray", new UUID(0, 0))) instanceof com.hollowfrontier.content.ufo.DisguisedGray gray) {
                double k = ease(rollTicks / (20 * (shot.seconds + HANDLES)));
                Vec3 bar = mark("belle").subtract(gray.position());
                float turn = (float) Mth.clamp((rollTicks - 20 * 3.2) / 16.0, 0, 1);
                float yaw = Mth.rotLerp(turn, yawOf(bar), yawOf(grayEye(k, side, front).subtract(gray.position())));
                gray.setYHeadRot(yaw);
                gray.setYBodyRot(yawOf(bar));
            }
        }));
        // Off his shoulder on the room side, the bar and Miss Belle beyond him, easing in
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 g = mark("gray").add(0, 1.25, 0), belle = mark("belle").add(0, 1.4, 0);
            return new Pose(grayEye(k, side, front), lerp(0.3, g, belle), 0, (float) Mth.lerp(k, 58, 50));
        };
        shot.wrap = () -> {
            JOBS.clear();
            server.execute(() -> {
                if (server.overworld().getEntity(SUBJECTS.getOrDefault("gray", new UUID(0, 0))) instanceof com.hollowfrontier.content.ufo.DisguisedGray gray) {
                    gray.stopRiding();
                    gray.discard();
                }
            });
        };
        return shot;
    }

    /** The camera for Mr. Gray, {@code k} into its move: beside him and out in the room, so he and the bar share the frame. */
    private static Vec3 grayEye(double k, Vec3 side, Vec3 front) {
        Vec3 g = mark("gray"), toBar = mark("belle").subtract(g).multiply(1, 0, 1).normalize();
        Vec3 across = new Vec3(-toBar.z, 0, toBar.x);
        // The side away from the room's middle would be in the wall: take whichever side is toward the saloon's front
        if (across.dot(front.add(side.scale(0.01))) < 0) {
            across = across.scale(-1);
        }
        // Close: from further out the lens started in the saloon's wall
        return g.add(across.scale(Mth.lerp(k, 1.9, 1.6))).subtract(toBar.scale(Mth.lerp(k, 1.15, 0.95))).add(0, 1.3, 0);
    }

    // 28. A plate for the title card: the town at dusk, its lamps lit, the camera drawing slowly up and away
    private static Shot endTown(MinecraftServer server, Vec3 c, Vec3 d) {
        Shot shot = new Shot(28, "end_town", 6.0);
        Vec3 across = new Vec3(-d.z, 0, d.x);
        shot.setup = () -> {
            run(server, "time set 12900");
            run(server, "gamemode spectator @a");
            tpEye(server, c.subtract(d.scale(30)).add(across.scale(22)).add(0, 26, 0), c);
        };
        shot.settle = 100;
        shot.rig = (s, p) -> {
            double k = ease(s / (shot.seconds + HANDLES));
            Vec3 eye = lerp(k, c.subtract(d.scale(30)).add(across.scale(22)).add(0, 26, 0), c.subtract(d.scale(62)).add(across.scale(38)).add(0, 44, 0));
            return new Pose(eye, c.add(d.scale(14)).add(0, 4, 0), 0, 64);
        };
        return shot;
    }

    // ---------------------------------------------------------------- staging helpers

    private static void night(MinecraftServer server) {
        run(server, "time set 14500"); // dark, with blue left in the sky so the shapes read
        for (String monster : List.of("zombie", "skeleton", "creeper", "spider", "enderman", "witch", "zombie_villager", "phantom")) {
            run(server, "kill @e[type=minecraft:" + monster + "]");
        }
        Minecraft.getInstance().options.gamma().set(1.0);
    }

    private static void spectator(MinecraftServer server) {
        run(server, "gamemode spectator @a");
        Minecraft.getInstance().options.gamma().set(0.5);
    }

    /** Creative, the gun in the first hotbar slot, loaded; flying when the camera is their eyes (it holds them up). */
    private static void handGun(MinecraftServer server, String gun, boolean flying) {
        run(server, "gamemode creative @a");
        run(server, "clear @a");
        run(server, "item replace entity @a hotbar.0 with hollowfrontier:" + gun);
        server.execute(() -> {
            ServerPlayer player = player(server);
            player.getAbilities().flying = flying;
            player.onUpdateAbilities();
            GunItem.setAmmo(player.getInventory().getItem(0), 6);
        });
    }

    /** Hat, duster, chaps and boots: the player as the trailer's cowboy. */
    private static void cowboy(MinecraftServer server) {
        run(server, "item replace entity @a armor.head with hollowfrontier:cowboy_hat");
        run(server, "item replace entity @a armor.chest with hollowfrontier:duster");
        run(server, "item replace entity @a armor.legs with hollowfrontier:chaps");
        run(server, "item replace entity @a armor.feet with hollowfrontier:cowboy_boots");
    }

    /** Fires the gun in hand where the player is looking (topping it up first, so a survival shootout never runs dry). */
    private static void fire(MinecraftServer server) {
        server.execute(() -> {
            ServerPlayer player = player(server);
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) {
                GunItem.setAmmo(stack, 6);
                GunItem.setReloadStart(stack, null);
                gun.use(player.level(), player, InteractionHand.MAIN_HAND);
            }
        });
    }

    private static Vec3 aliveOutlaw(float partial, Vec3 fallback) {
        for (int i = 0; i < 6; i++) {
            UUID id = SUBJECTS.get("outlaw" + i);
            if (id != null && clientEntity(id) instanceof LivingEntity living && living.isAlive() && !living.isDeadOrDying()) {
                return living.getPosition(partial);
            }
        }
        return fallback;
    }

    private static void postBoard(ServerLevel level, BountyBoardEntity board, String takenBy) {
        board.sync(level);
        BountyBoards boards = BountyBoards.get(level);
        long day = BountyBoards.day(level);
        RandomSource random = RandomSource.create(17);
        BlockPos at = board.blockPosition();
        java.util.function.Function<String, BountyData> bounty = name -> {
            BountyData b = BountyBoards.newBounty(at, random);
            return new BountyData(b.id(), name, b.x(), b.z(), b.reward(), false);
        };
        List<BoardPosting> postings = new ArrayList<>();
        BoardPosting first = BoardPosting.open(bounty.apply("Black Bart Calloway"), false, day);
        postings.add(takenBy == null ? first : first.taken(takenBy));
        postings.add(BoardPosting.open(bounty.apply("Two-Gun Tess"), false, day));
        postings.add(BoardPosting.open(bounty.apply("Rattlesnake Ricky"), false, day).dead());
        postings.add(BoardPosting.open(bounty.apply("Doc Hollister"), false, day));
        boards.put(board.key(level), postings, day);
        board.sync(level);
    }

    private static void openDoor(Level level, BlockPos pos) {
        setOpen(level, pos, true);
    }

    private static void closeDoor(Level level, BlockPos pos) {
        setOpen(level, pos, false);
    }

    private static void setOpen(Level level, BlockPos pos, boolean open) {
        for (BlockPos at : List.of(pos, pos.above())) {
            BlockState state = level.getBlockState(at);
            if (state.hasProperty(BlockStateProperties.OPEN)) {
                level.setBlock(at, state.setValue(BlockStateProperties.OPEN, open), 10);
            }
        }
    }

    private static AbstractOutlaw staged(ServerLevel level, EntityType<? extends AbstractOutlaw> type, Vec3 at, float yaw, ItemStack hand, GunWielder.GunPose pose) {
        AbstractOutlaw outlaw = type.create(level, EntitySpawnReason.COMMAND);
        outlaw.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        outlaw.setYBodyRot(yaw);
        outlaw.setYHeadRot(yaw);
        outlaw.setNoAi(true);
        outlaw.setPersistenceRequired();
        outlaw.setItemSlot(EquipmentSlot.MAINHAND, hand);
        outlaw.setGunPose(pose);
        level.addFreshEntity(outlaw);
        return outlaw;
    }

    private static AbstractOutlaw mounted(ServerLevel level, EntityType<? extends AbstractOutlaw> type, Vec3 at, float yaw, ItemStack hand) {
        Horse horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
        horse.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        horse.setYBodyRot(yaw);
        horse.setYHeadRot(yaw);
        horse.setNoAi(true);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        AbstractOutlaw rider = type.create(level, EntitySpawnReason.COMMAND);
        rider.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        rider.setNoAi(true);
        rider.setPersistenceRequired();
        rider.setItemSlot(EquipmentSlot.MAINHAND, hand);
        rider.startRiding(horse, true, false);
        level.addFreshEntityWithPassengers(horse);
        return rider;
    }

    /** Clears what earlier takes left: outlaws, horses, items, sticks. */
    private static void clear(ServerLevel level, Vec3 at, double radius) {
        level.getEntitiesOfClass(Entity.class, new AABB(at, at).inflate(radius, 40, radius), e -> e instanceof AbstractOutlaw || e instanceof Horse
                || e instanceof ItemEntity || e instanceof DynamiteEntity).forEach(Entity::discard);
    }

    /**
     * Clears the gallop's run of plants and trees: the riders' line and the camera's beside it, 130 blocks out, so
     * nothing stands between them (grass would fill the frame at the camera's height, and a trunk would wipe it).
     */
    private static void clearCorridor(ServerLevel level, Vec3 start, Vec3 dir, Vec3 side) {
        int cleared = 0;
        int base = Mth.floor(start.y);
        for (int step = -8; step <= 130; step++) {
            for (int across = -4; across <= 12; across++) {
                Vec3 at = start.add(dir.scale(step)).add(side.scale(across));
                for (int y = base - 6; y <= base + 24; y++) {
                    BlockPos pos = BlockPos.containing(at.x, y, at.z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || !state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        cleared++;
                    }
                }
            }
        }
        HollowFrontier.LOGGER.info("[Trailer] gallop corridor: {} plants and trees cleared", cleared);
    }

    /** Clears plants and trees from a box round {@code at} ({@code radius} out, {@code height} up), so nothing grows in front of the lens. */
    private static void clearBox(ServerLevel level, Vec3 at, int radius, int height) {
        int base = Mth.floor(at.y);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = base - 3; y <= base + height; y++) {
                    BlockPos pos = BlockPos.containing(at.x + dx, y, at.z + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || !state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static <T extends Entity> void forEach(ServerLevel level, Class<T> type, Vec3 at, double radius, Consumer<T> action) {
        level.getEntitiesOfClass(type, new AABB(at, at).inflate(radius)).forEach(action);
    }

    // ---------------------------------------------------------------- geometry

    /** A point in the town saloon's template, where the piece put it in the world. */
    private static Vec3 local(Vec3 local) {
        PoolElementStructurePiece piece = saloon;
        if (piece == null) {
            return Vec3.ZERO;
        }
        Vec3 turned = StructureTemplate.transform(local, net.minecraft.world.level.block.Mirror.NONE, piece.getRotation(), BlockPos.ZERO);
        return turned.add(piece.getPosition().getX(), piece.getPosition().getY(), piece.getPosition().getZ());
    }

    /** A direction in the saloon's template (x, z), turned as the piece is. */
    private static Vec3 localDir(double x, double z) {
        return local(new Vec3(x, 0, z)).subtract(local(Vec3.ZERO));
    }

    private static Vec3 ground(ServerLevel level, Vec3 at) {
        return new Vec3(at.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z)), at.z);
    }

    /** The top of whatever's there, leaves included. */
    private static double canopy(ServerLevel level, Vec3 at) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static Vec3 mark(String key) {
        return MARKS.getOrDefault(key, Vec3.ZERO);
    }

    /** Where a subject is drawn this frame on the client (or {@code fallback} if it isn't there). */
    private static Vec3 seen(String key, float partial, Vec3 fallback) {
        UUID id = SUBJECTS.get(key);
        Entity entity = id == null ? null : clientEntity(id);
        return entity == null ? fallback : entity.getPosition(partial);
    }

    private static Vec3 seen(String key, float partial) {
        return seen(key, partial, MARKS.getOrDefault(key, Vec3.ZERO));
    }

    private static Entity clientEntity(UUID id) {
        Entity cached = SEEN.get(id);
        Minecraft mc = Minecraft.getInstance();
        if (cached != null && !cached.isRemoved() && cached.level() == mc.level) {
            return cached;
        }
        if (mc.level == null) {
            return null;
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) {
                SEEN.put(id, e);
                return e;
            }
        }
        return null;
    }

    private static Vec3 lerp(double k, Vec3 a, Vec3 b) {
        return a.add(b.subtract(a).scale(k));
    }

    /** Smooth in and out (a camera move that starts and stops gently). */
    private static double ease(double k) {
        k = Mth.clamp(k, 0, 1);
        return 0.5 - 0.5 * Math.cos(Math.PI * k);
    }

    private static float yawOf(Vec3 v) {
        return (float) Math.toDegrees(Math.atan2(-v.x, v.z));
    }

    private static float pitchOf(Vec3 v) {
        return (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    /** Puts the player's eyes at {@code eye} looking at {@code target} (so the server loads the chunks there). */
    private static void tpEye(MinecraftServer server, Vec3 eye, Vec3 target) {
        tpFeet(server, eye.subtract(0, 1.62, 0), target, eye);
    }

    private static void tpFeet(MinecraftServer server, Vec3 feet, Vec3 target) {
        tpFeet(server, feet, target, feet.add(0, 1.62, 0));
    }

    private static void tpFeet(MinecraftServer server, Vec3 feet, Vec3 target, Vec3 eye) {
        Vec3 v = target.subtract(eye);
        run(server, String.format(Locale.ROOT, "execute in minecraft:overworld run tp @a %.2f %.2f %.2f %.1f %.1f", feet.x, feet.y, feet.z, yawOf(v), pitchOf(v)));
    }

    private static void run(MinecraftServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command));
    }
}

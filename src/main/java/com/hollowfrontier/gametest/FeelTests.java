package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.content.WorldFeel;
import com.hollowfrontier.content.frontier.gun.GunFeel;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// 46: what things feel like. The numbers only; the view and the dust need a client.
final class FeelTests {
    private FeelTests() {}

    /** 46a: the Blunderbuss kicks most, then the rifle, then the revolver; a ray hardly at all; nothing past the cap. */
    static void kickGoesByTheLoad(GameTestHelper h) {
        float shotgun = GunFeel.kick(GunTypes.SHOTGUN), rifle = GunFeel.kick(GunTypes.RIFLE), revolver = GunFeel.kick(GunTypes.REVOLVER);
        float phlogiston = GunFeel.kick(GunTypes.PHLOGISTON_RIFLE), ray = GunFeel.kick(GunTypes.RAY_PISTOL);
        h.assertTrue(shotgun > rifle && rifle > revolver, "black powder should kick by its load: " + shotgun + ", " + rifle + ", " + revolver);
        h.assertTrue(revolver > phlogiston && phlogiston > ray && ray > 0.0F, "a ray should kick less than any powder: " + phlogiston + ", " + ray);
        h.assertTrue(revolver >= 1.0F, "a revolver's kick should be enough to see");
        for (GunStats gun : List.of(GunTypes.SHOTGUN, GunTypes.RIFLE, GunTypes.REVOLVER, GunTypes.PHLOGISTON_RIFLE, GunTypes.RAY_PISTOL)) {
            h.assertTrue(GunFeel.kick(gun) <= GunFeel.MAX_KICK, "a kick is past the cap");
        }
        h.succeed();
    }

    /** 46: the phase changed how a gun feels and nothing about what it does. These are 1.3.0's numbers. */
    static void feelLeavesTheGunsAlone(GameTestHelper h) {
        same(h, "revolver", GunTypes.REVOLVER, 7.0F, 1, 1.0F, 40.0, 12, 15);
        same(h, "rifle", GunTypes.RIFLE, 18.0F, 1, 0.0F, 100.0, 10, 50);
        same(h, "blunderbuss", GunTypes.SHOTGUN, 3.5F, 8, 7.0F, 20.0, 8, 35);
        same(h, "ray pistol", GunTypes.RAY_PISTOL, 10.0F, 1, 0.0F, 40.0, 10, 0);
        same(h, "phlogiston rifle", GunTypes.PHLOGISTON_RIFLE, 30.0F, 1, 0.0F, 80.0, 30, 0);
        h.succeed();
    }

    private static void same(GameTestHelper h, String name, GunStats gun, float damage, int pellets, float spread, double range, int cooldown, int load) {
        h.assertTrue(gun.damage() == damage && gun.pellets() == pellets && gun.spreadDegrees() == spread && gun.range() == range
                && gun.fireCooldown() == cooldown && gun.loadTicks() == load, "the " + name + "'s numbers have moved");
    }

    /** 46a: stone, metal and glass are hard (a spark, maybe a ricochet); wood, earth and hay aren't. A strike on a real block runs. */
    static void aBallKnowsWhatItHit(GameTestHelper h) {
        for (var hard : List.of(Blocks.STONE, Blocks.IRON_BLOCK, Blocks.GLASS, Blocks.COBBLESTONE, Blocks.DEEPSLATE)) {
            h.assertTrue(GunFeel.impact(hard.defaultBlockState()) == GunFeel.Impact.HARD, hard + " should be hard");
        }
        for (var soft : List.of(Blocks.OAK_PLANKS, Blocks.SAND, Blocks.DIRT, Blocks.CLAY, Blocks.HAY_BLOCK)) {
            h.assertTrue(GunFeel.impact(soft.defaultBlockState()) == GunFeel.Impact.SOFT, soft + " should be soft");
        }
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, Blocks.STONE);
        BlockPos abs = h.absolutePos(pos);
        for (int pellet = 0; pellet < 8; pellet++) {
            GunFeel.strike(h.getLevel(), new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false), pellet, h.getLevel().getRandom());
        }
        h.succeed();
    }

    /** 46a: a ball is heard going past within two blocks, but not at the muzzle, not from further off, and not on a short shot. */
    static void aNearMissIsNear(GameTestHelper h) {
        Vec3 start = Vec3.ZERO, end = new Vec3(20, 0, 0);
        Vec3 heard = GunFeel.nearMiss(start, end, new Vec3(10, 1.5, 0));
        h.assertTrue(heard != null && heard.distanceTo(new Vec3(10, 0, 0)) < 0.01, "a ball 1.5 blocks off should be heard, from where it passed");
        h.assertTrue(GunFeel.nearMiss(start, end, new Vec3(10, 2.5, 0)) == null, "a ball 2.5 blocks off shouldn't be heard");
        h.assertTrue(GunFeel.nearMiss(start, end, new Vec3(2, 0.5, 0)) == null, "nobody beside the muzzle hears a near miss");
        h.assertTrue(GunFeel.nearMiss(start, end, new Vec3(21, 0.5, 0)) != null, "a ball that stops just short of you still went past");
        h.assertTrue(GunFeel.nearMiss(start, end, new Vec3(30, 0, 0)) == null, "a ball that stopped ten blocks short didn't go past");
        h.assertTrue(GunFeel.nearMiss(start, new Vec3(3, 0, 0), new Vec3(3, 0.5, 0)) == null, "a three-block shot is all muzzle");
        h.succeed();
    }

    /** 46a: three shots close together in time and place make a haze; two, or three far apart, or three long ago, don't. */
    static void aGunfightLeavesAHaze(GameTestHelper h) {
        Vec3 here = new Vec3(0, 64, 0);
        long now = 1000;
        h.assertFalse(GunFeel.hazy(List.of(here, here), List.of(now - 10, now), here, now), "two shots shouldn't make a haze");
        h.assertTrue(GunFeel.hazy(List.of(here, here.add(3, 0, 0), here), List.of(now - 60, now - 30, now), here, now), "three shots should");
        h.assertFalse(GunFeel.hazy(List.of(here.add(20, 0, 0), here.add(-20, 0, 0), here), List.of(now - 20, now - 10, now), here, now),
                "three shots from three places shouldn't");
        h.assertFalse(GunFeel.hazy(List.of(here, here, here), List.of(now - 400, now - 300, now), here, now), "three shots over twenty seconds shouldn't");
        // And for real: three from one spot leave one haze, not three
        Vec3 spot = Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 3, 1)));
        int before = GunFeel.hazes(h.getLevel());
        for (int i = 0; i < 5; i++) {
            GunFeel.powder(h.getLevel(), spot);
        }
        h.assertTrue(GunFeel.hazes(h.getLevel()) == before + 1, "five shots from one spot should leave one haze");
        h.succeed();
    }

    /** 46b: dust goes by the ground and the weather, and a horseshoe rings only on something hard. */
    static void dustGoesByTheGround(GameTestHelper h) {
        var sand = Blocks.SAND.defaultBlockState();
        var grass = Blocks.GRASS_BLOCK.defaultBlockState();
        var dirt = Blocks.DIRT.defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        var boards = Blocks.OAK_PLANKS.defaultBlockState();
        h.assertTrue(WorldFeel.dust(sand, false) > WorldFeel.dust(dirt, false) && WorldFeel.dust(dirt, false) > WorldFeel.dust(grass, false)
                && WorldFeel.dust(grass, false) > 0, "sand should throw more dust than dirt, and dirt than grass");
        h.assertTrue(WorldFeel.dust(Blocks.RED_SAND.defaultBlockState(), false) == WorldFeel.dust(sand, false)
                && WorldFeel.dust(Blocks.DIRT_PATH.defaultBlockState(), false) == WorldFeel.dust(sand, false), "red sand and a path are as dusty as sand");
        h.assertTrue(WorldFeel.dust(stone, false) == 0 && WorldFeel.dust(boards, false) == 0, "stone and boards throw no dust");
        h.assertTrue(WorldFeel.dust(grass, true) == 0 && WorldFeel.dust(dirt, true) == 0, "rain lays the dust on grass and earth");
        h.assertTrue(WorldFeel.dust(sand, true) > 0 && WorldFeel.dust(sand, true) < WorldFeel.dust(sand, false), "and thins it on sand");
        h.assertTrue(WorldFeel.dust(Blocks.SNOW_BLOCK.defaultBlockState(), false) > 0, "snow should puff");
        h.assertTrue(WorldFeel.ground(stone).rings() && WorldFeel.ground(Blocks.BRICKS.defaultBlockState()).rings(), "a shoe should ring on stone and brick");
        h.assertFalse(WorldFeel.ground(sand).rings() || WorldFeel.ground(grass).rings() || WorldFeel.ground(boards).rings(),
                "a shoe shouldn't ring on sand, grass or boards");
        h.succeed();
    }

    /** 46b: the wind rises with speed and with height, is nothing at rest near the ground, and the deck leans no further than its cap. */
    static void windGoesBySpeedAndHeight(GameTestHelper h) {
        h.assertTrue(WorldFeel.wind(0.0, 64.0) == 0.0F, "no wind at rest near the ground");
        for (double speed = 0.0; speed < WorldFeel.WIND_FULL_SPEED; speed += 0.05) {
            h.assertTrue(WorldFeel.wind(speed + 0.05, 100.0) > WorldFeel.wind(speed, 100.0), "the wind should rise with speed at " + speed);
        }
        for (double y = WorldFeel.WIND_FROM_Y; y < WorldFeel.WIND_FULL_Y; y += 10) {
            h.assertTrue(WorldFeel.wind(0.3, y + 10) > WorldFeel.wind(0.3, y), "the wind should rise with height at " + y);
        }
        h.assertTrue(WorldFeel.wind(5.0, 400.0) == 1.0F && WorldFeel.wind(0.0, 400.0) > 0.0F, "full at speed and height; some aloft even at rest");
        h.assertTrue(WorldFeel.sway(100.0F) == WorldFeel.MAX_SWAY && WorldFeel.sway(-100.0F) == -WorldFeel.MAX_SWAY && WorldFeel.sway(0.0F) == 0.0F,
                "the deck's lean has a cap and a level");
        h.assertTrue(WorldFeel.inCloud(WorldFeel.CLOUD_Y) && !WorldFeel.inCloud(WorldFeel.CLOUD_Y + 20) && !WorldFeel.inCloud(64.0), "cloud is where the clouds are");
        h.succeed();
    }

    /** 46c: a shake falls away with distance and is nothing at its range; two at once stop at the cap; a charge is harder than a walk. */
    static void aShakeFallsWithDistance(GameTestHelper h) {
        h.assertTrue(WorldFeel.shake(0.8F, 0.0, 24.0) == 0.8F, "beside it, a shake is its whole self");
        for (double d = 0.0; d < 23.0; d += 1.0) {
            h.assertTrue(WorldFeel.shake(0.8F, d + 1.0, 24.0) < WorldFeel.shake(0.8F, d, 24.0), "a shake should fall with distance at " + d);
        }
        h.assertTrue(WorldFeel.shake(0.8F, 24.0, 24.0) == 0.0F && WorldFeel.shake(0.8F, 500.0, 24.0) == 0.0F, "nothing at its range or beyond");
        h.assertTrue(WorldFeel.add(0.8F, 0.9F) == WorldFeel.MAX_SHAKE && WorldFeel.add(0.2F, 0.3F) == 0.5F, "shakes add, up to the cap");
        for (WorldFeel.Tread tread : List.of(WorldFeel.MAMMOTH, WorldFeel.MEGATHERIUM, WorldFeel.GLYPTODON)) {
            h.assertTrue(WorldFeel.footfall(tread, WorldFeel.CHARGE, 3.0) > WorldFeel.footfall(tread, 0.05, 3.0), "a charge should land harder than a walk");
            h.assertTrue(WorldFeel.footfall(tread, 1.0, tread.range() + 1) == 0.0F, "a footfall isn't felt past its range");
            h.assertTrue(WorldFeel.footfall(tread, 1.0, 0.0) < WorldFeel.DYNAMITE.shake(), "no footfall is a stick of dynamite");
        }
        h.assertTrue(WorldFeel.footfall(WorldFeel.MAMMOTH, 0.05, 3.0) > WorldFeel.footfall(WorldFeel.GLYPTODON, 0.05, 3.0), "a mammoth outweighs a glyptodon");
        h.assertTrue(WorldFeel.blast(WorldFeel.DEATH_RAY, 20.0) > WorldFeel.blast(WorldFeel.VAULT, 20.0)
                && WorldFeel.blast(WorldFeel.VAULT, 20.0) > WorldFeel.blast(WorldFeel.DYNAMITE, 20.0), "the death ray, then a vault, then dynamite");
        h.assertTrue(WorldFeel.TREMOR < WorldFeel.DYNAMITE.shake(), "the Mothership's tremor is a tremor");
        h.succeed();
    }
}

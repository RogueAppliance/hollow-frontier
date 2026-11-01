package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.SealSocketBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.worldgen.structure.DungeonBlueprints;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * The dungeons under the sites (26): every connector in the kits finds a piece to join in the pool it opens onto, and the
 * three sites hang their shafts from their bottom layers; the vault's seal holds against a wrong answer and opens on the
 * right one; a trap's plate fires its dispenser; the hoard went down to the undercroft.
 */
final class DungeonTests {
    private DungeonTests() {}

    private record Connector(String name, String target, String pool, Direction front) {}

    private static List<Connector> connectors(GameTestHelper h, String template) {
        StructureTemplate t = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(template)).orElse(null);
        h.assertTrue(t != null, template + " template missing");
        List<Connector> out = new ArrayList<>();
        for (var info : t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)) {
            out.add(new Connector(info.nbt().getStringOr("name", ""), info.nbt().getStringOr("target", ""), info.nbt().getStringOr("pool", ""),
                    JigsawBlock.getFrontFacing(info.state())));
        }
        return out;
    }

    /**
     * Every connector in every kit that opens onto a pool finds, among that pool's pieces, one with a connector named as
     * its target and facing back at it; the pools exist; and the gate, the deep lab and the Sun Temple each have the one
     * connector down to their kit's shaft, in their bottom layer.
     */
    static void kitsJoinUp(GameTestHelper h) {
        var pools = h.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        for (DungeonBlueprints.Kit kit : DungeonBlueprints.KITS) {
            for (DungeonBlueprints.PoolSpec spec : DungeonBlueprints.pools(kit)) {
                h.assertTrue(pools.containsKey(ResourceKey.create(Registries.TEMPLATE_POOL, HollowFrontier.id(kit.id() + "/" + spec.pool()))),
                        "pool " + kit.id() + "/" + spec.pool() + " isn't registered");
                for (String template : spec.templates()) {
                    for (Connector c : connectors(h, template)) {
                        if (c.pool().equals("minecraft:empty")) {
                            continue;
                        }
                        String poolName = c.pool().substring((HollowFrontier.MODID + ":" + kit.id() + "/").length());
                        List<String> candidates = DungeonBlueprints.pools(kit).stream().filter(p -> p.pool().equals(poolName)).findFirst()
                                .map(DungeonBlueprints.PoolSpec::templates).orElse(List.of());
                        boolean joins = candidates.stream().anyMatch(other -> connectors(h, other).stream()
                                .anyMatch(o -> o.name().equals(c.target()) && (c.front().getAxis() == Direction.Axis.Y
                                        ? o.front() == c.front().getOpposite() : o.front().getAxis() != Direction.Axis.Y)));
                        h.assertTrue(joins, template + "'s connector to " + c.pool() + " finds nothing named " + c.target());
                    }
                }
            }
        }
        for (var site : List.of(new Object[] {HollowBlueprints.KINDLED_GATE, DungeonBlueprints.CATACOMBS},
                new Object[] {HollowBlueprints.GRAY_LAB_DEEP, DungeonBlueprints.GRAY_COMPLEX},
                new Object[] {ConcaviaBlueprints.SUN_TEMPLE, DungeonBlueprints.UNDERCROFT})) {
            String template = (String) site[0];
            DungeonBlueprints.Kit kit = (DungeonBlueprints.Kit) site[1];
            StructureTemplate t = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(template)).orElseThrow();
            var down = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
            h.assertValueEqual(down.size(), 1, template + "'s connectors");
            h.assertTrue(down.getFirst().pos().getY() == 0 && JigsawBlock.getFrontFacing(down.getFirst().state()) == Direction.DOWN
                    && down.getFirst().nbt().getStringOr("pool", "").equals(kit.pool("shaft")), template + "'s shaft connector");
        }
        h.assertFalse(h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(HollowBlueprints.GRAY_LAB)).orElseThrow()
                .filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW).iterator().hasNext(), "the plain lab has a dungeon");
        h.succeed();
    }

    private static StructureTemplate place(GameTestHelper h, String template) {
        StructureTemplate t = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(template)).orElseThrow();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        t.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        return t;
    }

    /**
     * The vault (its first variant): the seal stands; a lamp on an unmarked socket opens nothing, nor do lamps on only
     * some of the marked ones; lamps on all the marked ones, and none on the others, and it crumbles away. The chest
     * behind it rolls the vault's loot.
     */
    static void vaultSealOpens(GameTestHelper h) {
        StructureTemplate t = place(h, DungeonBlueprints.CATACOMBS.template("end_0"));
        var seals = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.KINDLED_SEAL.get());
        var sockets = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.SEAL_SOCKET.get());
        h.assertValueEqual(seals.size(), 9, "seal blocks");
        h.assertValueEqual(sockets.size(), 4, "sockets");
        h.assertValueEqual(LoreSiteTests.chestsRolling(t, ModChestLoot.CATACOMB_VAULT), 1L, "the vault's chest");
        BlockPos seal = h.absolutePos(seals.getFirst().pos());
        List<BlockPos> marked = new ArrayList<>(), unmarked = new ArrayList<>();
        for (var socket : sockets) {
            (socket.state().getValue(SealSocketBlock.MARKED) ? marked : unmarked).add(socket.pos());
        }
        h.assertTrue(!marked.isEmpty() && !unmarked.isEmpty(), "the mural marks some sockets and not others");
        // Wrong: a lamp on an unmarked socket, and on all the marked ones
        h.setBlock(unmarked.getFirst().above(), ModBlocks.PHLOGISTON_LAMP.get());
        marked.forEach(p -> h.setBlock(p.above(), ModBlocks.PHLOGISTON_LAMP.get()));
        h.assertTrue(h.getLevel().getBlockState(seal).is(ModBlocks.KINDLED_SEAL.get()), "the seal opened on a wrong answer");
        // Right: take the stray lamp away
        h.setBlock(unmarked.getFirst().above(), Blocks.AIR);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.getLevel().getBlockState(seal).isAir(), "the seal didn't open on the right answer");
            h.assertTrue(seals.stream().allMatch(s -> h.getLevel().getBlockState(h.absolutePos(s.pos())).isAir()), "some of the seal is left");
            h.succeed();
        });
    }

    /** A trap corridor: a mob steps on the first plate, and the dispenser beside it fires (it has fewer arrows after). */
    static void trapFires(GameTestHelper h) {
        StructureTemplate t = place(h, DungeonBlueprints.UNDERCROFT.template("path"));
        var plates = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
        var dispensers = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.DISPENSER);
        h.assertValueEqual(plates.size(), 3, "plates");
        h.assertValueEqual(dispensers.size(), 3, "dispensers");
        BlockPos plate = plates.stream().min(java.util.Comparator.comparingInt(p -> p.pos().getZ())).orElseThrow().pos();
        BlockPos dispenser = dispensers.stream().filter(d -> d.pos().getZ() == plate.getZ()).findFirst().orElseThrow().pos();
        int before = h.getBlockEntity(dispenser, DispenserBlockEntity.class).getItem(0).getCount();
        h.spawn(EntityTypes.ZOMBIE, plate);
        h.runAfterDelay(20, () -> {
            int after = h.getBlockEntity(dispenser, DispenserBlockEntity.class).getItem(0).getCount();
            h.assertTrue(after < before, "the trap didn't fire: " + before + " -> " + after);
            h.getLevel().getEntities((Entity) null, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(24), e -> !(e instanceof Player)).forEach(Entity::discard);
            h.succeed();
        });
    }

    /** The hoard is in the undercroft's end room now, not by the throne; the throne hall keeps its offerings. */
    static void hoardMovedDown(GameTestHelper h) {
        StructureTemplate temple = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(ConcaviaBlueprints.SUN_TEMPLE)).orElseThrow();
        StructureTemplate end = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(DungeonBlueprints.UNDERCROFT.template("end"))).orElseThrow();
        h.assertValueEqual(LoreSiteTests.chestsRolling(temple, ModChestLoot.SUN_TEMPLE), 0L, "hoard chests left by the throne");
        h.assertValueEqual(LoreSiteTests.chestsRolling(end, ModChestLoot.SUN_TEMPLE), 1L, "the hoard in the undercroft");
        StructureTemplate core = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(DungeonBlueprints.GRAY_COMPLEX.template("end"))).orElseThrow();
        h.assertValueEqual(LoreSiteTests.chestsRolling(core, ModChestLoot.GRAY_COMPLEX_CORE), 1L, "the data core's chest");
        StructureTemplate reactor = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(DungeonBlueprints.GRAY_COMPLEX.template("reactor"))).orElseThrow();
        h.assertValueEqual(reactor.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SPAWNER).size(), 1, "the reactor's spawner pad");
        h.succeed();
    }
}

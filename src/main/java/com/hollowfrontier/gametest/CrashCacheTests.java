package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.CargoPodBlock;
import com.hollowfrontier.content.ufo.GrayGadgetItem;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.UfoBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

// 32c: the crash site's cargo pod and the Grays' gadgets in it.
final class CrashCacheTests {
    private CrashCacheTests() {}

    /** Both crash sites share the one template: it has its sealed pod in the cockpit. */
    static void crashSiteHasACargoPod(GameTestHelper h) {
        StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(UfoBlueprints.CRASH_SITE)).orElseThrow();
        var pods = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.GRAY_CARGO_POD.get());
        h.assertValueEqual(pods.size(), 1, "cargo pods in the crash site");
        h.assertFalse(pods.get(0).state().getValue(CargoPodBlock.OPEN), "the crash site's pod is already open");
        h.succeed();
    }

    /**
     * Sealed, a pod won't break; used, it hisses open and spills salvage and at least one gadget out of its top, and stays
     * there, open and empty; used again it gives nothing more.
     */
    static void cargoPodSpillsItsLoad(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pod = new BlockPos(5, 1, 5);
        h.setBlock(pod, ModBlocks.GRAY_CARGO_POD.get());
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3), "hf-pod-opener");
        h.assertTrue(h.getBlockState(pod).getDestroyProgress(player, level, h.absolutePos(pod)) == 0.0F, "a sealed pod can be broken");
        h.useBlock(pod, player);
        h.assertTrue(h.getBlockState(pod).is(ModBlocks.GRAY_CARGO_POD.get()) && h.getBlockState(pod).getValue(CargoPodBlock.OPEN), "the pod should stay, open");
        AABB around = new AABB(h.absolutePos(pod)).inflate(3);
        List<ItemEntity> spilled = level.getEntitiesOfClass(ItemEntity.class, around);
        h.assertTrue(spilled.stream().anyMatch(e -> e.getItem().is(ModItems.ALIEN_ALLOY_PLATING.get())), "no alloy plating: " + spilled);
        h.assertTrue(spilled.stream().anyMatch(e -> e.getItem().getItem() instanceof GrayGadgetItem), "no gadget: " + spilled);
        h.assertTrue(h.getBlockState(pod).getDestroyProgress(player, level, h.absolutePos(pod)) > 0.0F, "an open pod can't be broken");
        int count = spilled.size();
        h.useBlock(pod, player);
        h.assertValueEqual(level.getEntitiesOfClass(ItemEntity.class, around).size(), count, "items after using it again");
        spilled.forEach(ItemEntity::discard);
        h.succeed();
    }

    /**
     * Each gadget's trick: the doohickey finds ore, the rock thaws you, the watch tells the time, the marble hops you up
     * with slow falling to come down, and the box, when it does open, is used up and gives up salvage.
     */
    static void gadgetsDoTheirTricks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-gadgeteer");
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertValueEqual(GrayGadgetItem.nearestOre(level, at), -1, "ore found with none about");
        h.setBlock(new BlockPos(5, 1, 2), Blocks.IRON_ORE);
        h.assertValueEqual(GrayGadgetItem.nearestOre(level, at), 3, "distance to the ore");

        ItemStack rock = new ItemStack(ModItems.SELF_WARMING_ROCK.get());
        player.setTicksFrozen(120);
        rock.inventoryTick(level, player, null);
        h.assertValueEqual(player.getTicksFrozen(), 0, "frost with the rock carried");

        for (var gadget : List.of(ModItems.HUMMING_DOOHICKEY, ModItems.BACKWARD_WATCH)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gadget.get()));
            h.assertTrue(player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND).consumesAction(), gadget.getId() + " did nothing");
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RESTLESS_MARBLE.get()));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getDeltaMovement().y > 0.5, "the marble didn't hop: " + player.getDeltaMovement());
        h.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "no slow falling after the hop");

        ItemStack box = new ItemStack(ModItems.UNOPENABLE_BOX.get(), 2);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        GrayGadgetItem.openTheBox(level, player, box);
        h.assertValueEqual(box.getCount(), 1, "boxes left after one opened");
        h.assertTrue(player.getInventory().contains(s -> s.is(ModItems.ALIEN_ALLOY_PLATING.get())), "the box gave up no salvage");
        h.succeed();
    }
}

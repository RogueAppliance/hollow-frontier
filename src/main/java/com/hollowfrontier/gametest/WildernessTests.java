package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gear.CowboySetBonus;
import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.gear.SnakeskinBoots;
import com.hollowfrontier.content.frontier.wildlife.Rattlesnake;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;

// Out on the range: dynamite and rattlesnakes.
final class WildernessTests {
    private WildernessTests() {}

    static void dynamiteBlastsBlocks(GameTestHelper h) {
        BlockPos rock = new BlockPos(5, 1, 5);
        h.setBlock(rock, Blocks.DIRT);
        h.setBlock(rock.east(), Blocks.DIRT);
        DynamiteEntity dynamite = new DynamiteEntity(ModEntities.DYNAMITE.get(), h.getLevel());
        BlockPos abs = h.absolutePos(rock.above());
        dynamite.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        h.getLevel().addFreshEntity(dynamite);
        h.runAfterDelay(DynamiteEntity.FUSE_TICKS - 10, () -> h.assertBlockPresent(Blocks.DIRT, rock));
        h.succeedWhen(() -> {
            h.assertFalse(dynamite.isAlive(), "dynamite never went off");
            h.assertBlockNotPresent(Blocks.DIRT, rock);
            h.assertBlockNotPresent(Blocks.DIRT, rock.east());
        });
    }

    static void rattlesnakeWarnsFirst(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 5));
        Rattlesnake snake = ModEntities.RATTLESNAKE.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(7, 1, 5)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.runAfterDelay(40, () -> {
            h.assertTrue(snake.isRattling(), "snake didn't rattle at a player 5 blocks off");
            h.assertTrue(snake.getTarget() == null, "snake struck without being crowded");
            h.assertTrue(player.getHealth() == player.getMaxHealth(), "snake bit from range");
            h.succeed();
        });
    }

    static void snakeskinBootsSpareWearer(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.SNAKESKIN_BOOTS.get()));
        Rattlesnake snake = ModEntities.RATTLESNAKE.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 7)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.runAfterDelay(60, () -> {
            h.assertTrue(snake.isRattling(), "snake didn't even rattle");
            h.assertTrue(snake.getTarget() == null && player.getHealth() == player.getMaxHealth(), "snake struck a snakeskin-booted player");
            h.succeed();
        });
    }

    /** Boots + 2 snakeskin → Snakeskin Boots that keep the boots' wear and name; one snakeskin isn't enough. */
    @SuppressWarnings("unchecked")
    static void snakeskinBootsRecipe(GameTestHelper h) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, HollowFrontier.id("snakeskin_boots"));
        Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) h.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        ItemStack boots = new ItemStack(ModItems.COWBOY_BOOTS.get());
        boots.setDamageValue(17);
        boots.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
        ItemStack skin = new ItemStack(ModItems.SNAKESKIN.get());
        h.assertFalse(recipe.matches(CraftingInput.of(2, 1, List.of(boots, skin)), h.getLevel()), "one snakeskin was enough");
        CraftingInput input = CraftingInput.of(3, 1, List.of(boots, skin, skin.copy()));
        h.assertTrue(recipe.matches(input, h.getLevel()), "boots + 2 snakeskin don't match");
        ItemStack result = recipe.assemble(input);
        h.assertTrue(result.is(ModItems.SNAKESKIN_BOOTS.get()) && result.getDamageValue() == 17
                && "Old Faithful".equals(result.getHoverName().getString()), "boots lost their wear or name: " + result);

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DUSTER.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CHAPS.get()));
        player.setItemSlot(EquipmentSlot.FEET, result);
        h.assertTrue(CowboySetBonus.hasFullSet(player), "snakeskin boots don't count for the cowboy set");
        h.setBlock(new BlockPos(2, 0, 2), Blocks.SAND);
        player.setOnGround(true);
        SnakeskinBoots.update(player);
        h.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), "no speed on sand");
        h.succeed();
    }

    static void rattlesnakeBitePoisons(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        ModEntities.RATTLESNAKE.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 7)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.succeedWhen(() -> h.assertTrue(player.hasEffect(MobEffects.POISON), "snake hasn't bitten the player yet"));
    }
}

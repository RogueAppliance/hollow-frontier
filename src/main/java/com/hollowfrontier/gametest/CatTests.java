package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.CatCoats;
import com.hollowfrontier.content.frontier.cat.CatHearthGoal;
import com.hollowfrontier.content.frontier.cat.TownCat;
import com.hollowfrontier.content.frontier.cat.TownCat.Accessory;
import com.hollowfrontier.content.frontier.cat.TownCat.Role;
import com.hollowfrontier.content.frontier.cat.TownCatOdds;
import com.hollowfrontier.content.frontier.cat.TownCats;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.wildlife.Rattlesnake;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.TownBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

// Town cats: blueprint spots, the sheriff's star, saving, dressing, snakes, taming and the ghost cat.
final class CatTests {
    private CatTests() {}

    private static final List<String> SQUARES = List.of("town/square_cross_bandstand", "town/square_cross_well", "town/square_tee_bandstand",
            "town/square_tee_well", "town/square_pair_bandstand", "town/square_pair_well");
    private static final List<String> TOWN_TEMPLATES = java.util.stream.Stream.concat(SQUARES.stream(), java.util.stream.Stream.of("saloon",
            "town/street_long", "town/street_short", "town/lane", "town/saloon", "town/saloon_grand", "town/general_store", "town/sheriff_office",
            "town/sheriff_office_2", "town/bank", "town/hotel", "town/hotel_grand", "town/blacksmith", "town/livery", "town/undertaker", "town/house",
            "town/adobe", "town/chapel", "town/water_tower", "town/courthouse", "town/church", "town/depot", "town/windpump", "town/corral",
            "town/boot_hill", "town/garden", "town/town_limits", "town/desert/sheriff_office", "town/mining/sheriff_office")).toList();

    private static CompoundTag template(ServerLevel level, String name) {
        StructureTemplate template = level.getStructureTemplateManager().get(HollowFrontier.id(name)).orElseThrow();
        return template.save(new CompoundTag());
    }

    // The town_cat roles of the cats a template places.
    private static List<String> catRoles(CompoundTag template) {
        List<String> roles = new ArrayList<>();
        for (Tag entry : template.getListOrEmpty("entities")) {
            CompoundTag nbt = ((CompoundTag) entry).getCompoundOrEmpty("nbt");
            if (nbt.getStringOr("id", "").equals("minecraft:cat")) {
                roles.add(nbt.getCompoundOrEmpty("neoforge:attachments").getCompoundOrEmpty(HollowFrontier.MODID + ":town_cat").getStringOr("role", "?"));
            }
        }
        return roles;
    }

    private static Cat cat(GameTestHelper h, BlockPos pos, TownCat data) {
        Cat cat = h.spawnWithNoFreeWill(EntityTypes.CAT, pos);
        TownCat.set(cat, data);
        return cat;
    }

    private static ServerPlayer holding(GameTestHelper h, BlockPos pos, ItemStack stack) {
        ServerPlayer player = TestPlayers.survival(h, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void use(ServerPlayer player, Cat cat) {
        player.interactOn(cat, InteractionHand.MAIN_HAND, cat.position());
    }

    /** The saloon and most town buildings have cat spots, the street has its fire pit, and roles are known ones. */
    static void townTemplatesHaveCats(GameTestHelper h) {
        Map<String, List<String>> spots = new TreeMap<>();
        for (String name : TOWN_TEMPLATES) {
            spots.put(name, catRoles(template(h.getLevel(), name)));
        }
        for (String name : List.of("saloon", "town/saloon", "town/general_store", "town/sheriff_office", "town/blacksmith", "town/livery",
                "town/undertaker", "town/house", "town/adobe")) {
            h.assertFalse(spots.get(name).isEmpty(), "no cat spot in " + name + ": " + spots);
        }
        h.assertTrue(spots.get("town/saloon").contains("saloon") && spots.get("town/saloon_grand").contains("saloon"), "a saloon cat is missing: " + spots);
        spots.values().stream().flatMap(List::stream).forEach(role -> h.assertTrue(
                java.util.Arrays.stream(Role.values()).anyMatch(r -> r.getSerializedName().equals(role)) && !role.equals("stray"), "bad cat role " + role));
        for (String square : SQUARES) {
            StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(square)).orElseThrow();
            boolean fire = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CAMPFIRE).stream()
                    .anyMatch(info -> info.state().getValue(CampfireBlock.LIT));
            h.assertTrue(fire, square + " has no lit fire pit");
        }
        h.succeed();
    }

    /** Only the sheriff's office has a sheriff cat spot, only a town's square has a civic lot (one), and the placed office's cat wears the star. */
    static void oneSheriffStarPerTown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (String name : TOWN_TEMPLATES) {
            long sheriffs = catRoles(template(level, name)).stream().filter("sheriff"::equals).count();
            h.assertTrue(sheriffs == (name.contains("sheriff_office") ? 1 : 0), name + " has " + sheriffs + " sheriff cat spots");
        }
        String civicPool = com.hollowfrontier.worldgen.structure.TownStyle.FRONTIER.pool(com.hollowfrontier.worldgen.structure.TownLayout.CIVIC);
        for (String piece : TOWN_TEMPLATES) {
            long civic = template(level, piece).getListOrEmpty("blocks").stream()
                    .filter(b -> ((CompoundTag) b).getCompoundOrEmpty("nbt").getStringOr("pool", "").equals(civicPool)).count();
            h.assertTrue(civic == (SQUARES.contains(piece) ? 1 : 0), piece + " has " + civic + " civic lots");
        }
        StructureTemplate office = level.getStructureTemplateManager().get(HollowFrontier.id("town/sheriff_office")).orElseThrow();
        BlockPos origin = h.absolutePos(new BlockPos(0, 60, 0));
        office.placeInWorld(level, origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), level.getRandom(), Block.UPDATE_CLIENTS);
        AABB area = new AABB(origin).inflate(16);
        try {
            List<Cat> cats = level.getEntitiesOfClass(Cat.class, area);
            List<Cat> starred = cats.stream().filter(c -> TownCat.of(c).accessory() == Accessory.SHERIFF_STAR).toList();
            h.assertTrue(starred.size() == 1, "expected one star-wearing cat, got " + starred.size() + " of " + cats.size());
            Cat sheriffsCat = starred.getFirst();
            h.assertTrue(TownCat.of(sheriffsCat).role() == Role.SHERIFF && TownCat.of(sheriffsCat).settled(), "star cat isn't the settled sheriff's cat");
            h.assertTrue(sheriffsCat.hasHome() && sheriffsCat.isPersistenceRequired(), "sheriff's cat has no home or could despawn");
        } finally {
            level.getEntitiesOfClass(Mob.class, area).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** Coat, role, accessory (with its dye) and ghostliness come back after a save and load. */
    static void catDataSurvivesSave(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cat cat = cat(h, new BlockPos(3, 1, 3), new TownCat(Role.LIVERY, TownCatOdds.bandana(DyeColor.BLUE), false, true, false));
        cat.setComponent(DataComponents.CAT_VARIANT, level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getOrThrow(CatCoats.DESERT));
        Cat ghost = cat(h, new BlockPos(6, 1, 3), new TownCat(Role.GHOST, ItemStack.EMPTY, true, true, false));
        for (Cat original : List.of(cat, ghost)) {
            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            original.saveWithoutId(out);
            Cat copy = EntityTypes.CAT.create(level, EntitySpawnReason.COMMAND);
            copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
            TownCat before = TownCat.of(original), after = TownCat.of(copy);
            h.assertTrue(after.role() == before.role() && after.ghost() == before.ghost() && after.settled(), "town cat data lost: " + after);
            h.assertTrue(after.accessory() == before.accessory() && after.bandanaColor() == before.bandanaColor(), "accessory lost: " + after.worn());
            h.assertTrue(copy.getVariant().is(original.getVariant().unwrapKey().orElseThrow()), "coat lost: " + copy.getVariant());
        }
        h.assertTrue(cat.getVariant().is(CatCoats.DESERT), "desert coat didn't apply");
        h.succeed();
    }

    /** An owner dresses their cat (swapping gives the old item back), undresses it by sneaking, and nobody else can. */
    static void accessoriesOnAndOff(GameTestHelper h) {
        Cat cat = h.spawnWithNoFreeWill(EntityTypes.CAT, new BlockPos(5, 1, 5));
        ServerPlayer owner = holding(h, new BlockPos(5, 1, 3), new ItemStack(ModItems.COWBOY_HAT.get()));
        cat.tame(owner);
        use(owner, cat);
        h.assertTrue(TownCat.of(cat).accessory() == Accessory.COWBOY_HAT && owner.getMainHandItem().isEmpty(), "hat didn't go on");
        h.assertTrue(cat.isOrderedToSit() == false, "dressing toggled sitting");

        owner.setItemInHand(InteractionHand.MAIN_HAND, TownCatOdds.bandana(DyeColor.BLUE));
        use(owner, cat);
        h.assertTrue(TownCat.of(cat).accessory() == Accessory.BANDANA, "bandana didn't replace the hat");
        h.assertTrue(owner.getInventory().countItem(ModItems.COWBOY_HAT.get()) == 1, "hat wasn't given back");

        ServerPlayer stranger = holding(h, new BlockPos(7, 1, 3), new ItemStack(ModItems.BRASS_STAR.get()));
        use(stranger, cat);
        h.assertTrue(TownCat.of(cat).accessory() == Accessory.BANDANA && !stranger.getMainHandItem().isEmpty(), "a stranger dressed my cat");

        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        owner.setShiftKeyDown(true);
        use(owner, cat);
        h.assertTrue(TownCat.of(cat).worn().isEmpty(), "sneak-use didn't take the bandana off");
        boolean returned = false;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            returned |= stack.is(ModItems.BANDANA.get()) && TownCat.STRAY.wearing(stack).bandanaColor() == (0xFF000000 | DyeColor.BLUE.getTextureDiffuseColor());
        }
        h.assertTrue(returned, "bandana (with its dye) wasn't returned");
        h.succeed();
    }

    /** A rattlesnake backs away from a cat. */
    static void rattlesnakesAvoidCats(GameTestHelper h) {
        Cat cat = h.spawnWithNoFreeWill(EntityTypes.CAT, new BlockPos(6, 1, 6));
        Rattlesnake snake = ModEntities.RATTLESNAKE.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(6, 1, 4)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.runAfterDelay(6, () -> h.assertTrue(snake.isRattling(), "snake isn't rattling at the cat"));
        h.succeedWhen(() -> h.assertTrue(snake.distanceTo(cat) > Rattlesnake.CAT_RANGE - 1.5, "snake stayed by the cat: " + snake.distanceTo(cat)));
    }

    /** Plain cats and untamed town cats still tame with raw fish, as in vanilla. */
    static void vanillaTamingStillWorks(GameTestHelper h) {
        Cat plain = h.spawnWithNoFreeWill(EntityTypes.CAT, new BlockPos(3, 1, 5));
        Cat townCat = cat(h, new BlockPos(8, 1, 5), new TownCat(Role.SALOON, ItemStack.EMPTY, false, true, false));
        for (Cat cat : List.of(plain, townCat)) {
            ServerPlayer player = holding(h, new BlockPos(cat == plain ? 3 : 8, 1, 3), new ItemStack(Items.COD, 64));
            for (int i = 0; i < 80 && !cat.isTame(); i++) {
                use(player, cat);
            }
            h.assertTrue(cat.isTame() && cat.isOwnedBy(player), "cat never tamed: " + TownCat.of(cat));
            h.assertTrue(player.getMainHandItem().getCount() < 64, "taming didn't use fish");
        }
        h.succeed();
    }

    /** The ghost cat ignores raw fish, and cooked fish too until music plays nearby; then it can be tamed. */
    static void ghostCatRitual(GameTestHelper h) {
        Cat ghost = cat(h, new BlockPos(5, 1, 5), new TownCat(Role.GHOST, ItemStack.EMPTY, true, true, false));
        ServerPlayer player = holding(h, new BlockPos(5, 1, 3), new ItemStack(Items.COD, 16));
        for (int i = 0; i < 20; i++) use(player, ghost);
        h.assertFalse(ghost.isTame(), "raw fish tamed the ghost cat");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_COD, 64));
        for (int i = 0; i < 20; i++) use(player, ghost);
        h.assertTrue(!ghost.isTame() && player.getMainHandItem().getCount() == 64, "fed the ghost cat without music");
        h.assertFalse(TownCats.musicNearby(h.getLevel(), ghost.blockPosition()), "music detected in silence");
        h.setBlock(new BlockPos(9, 1, 9), ModBlocks.SALOON_PIANO.get().defaultBlockState().setValue(SaloonPianoBlock.PLAYING, true));
        h.assertTrue(TownCats.musicNearby(h.getLevel(), ghost.blockPosition()), "playing piano not heard");
        for (int i = 0; i < 60 && !ghost.isTame(); i++) use(player, ghost);
        h.assertTrue(ghost.isTame() && TownCat.of(ghost).ghost(), "ghost cat never tamed with music (or stopped being a ghost)");
        h.succeed();
    }

    /** A brass-star cat makes a free outlaw glow, but not a jailed one. */
    /**
     * The sheriff's cat goes to its Sheriff. Two cats have consecutive entity ids, one odd and one even: goal selectors
     * try canUse on alternate ticks by id parity, which a tickCount-based throttle once missed for odd ids.
     */
    static void sheriffCatFollowsSheriff(GameTestHelper h) {
        com.hollowfrontier.content.frontier.townsfolk.Lawman sheriff = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(2, 1, 5));
        sheriff.setRank(com.hollowfrontier.content.frontier.townsfolk.Lawman.Rank.SHERIFF);
        List<Cat> cats = new ArrayList<>();
        for (BlockPos pos : List.of(new BlockPos(10, 1, 3), new BlockPos(10, 1, 8))) {
            Cat cat = EntityTypes.CAT.spawn(h.getLevel(), h.absolutePos(pos), EntitySpawnReason.SPAWN_ITEM_USE);
            TownCat.set(cat, new TownCat(Role.SHERIFF, new ItemStack(ModItems.BRASS_STAR.get()), false, true, false));
            cats.add(cat);
        }
        h.assertTrue(cats.get(0).getId() % 2 != cats.get(1).getId() % 2, "setup: both cats have the same id parity");
        h.succeedWhen(() -> {
            for (Cat cat : cats) {
                h.assertTrue(cat.distanceTo(sheriff) < 5.0, "cat " + cat.getId() + " hasn't come to the sheriff (" + cat.distanceTo(sheriff) + " away)");
            }
        });
    }

    static void starCatSpotsOutlaws(GameTestHelper h) {
        Cat cat = cat(h, new BlockPos(5, 1, 5), new TownCat(Role.SHERIFF, new ItemStack(ModItems.BRASS_STAR.get()), false, true, false));
        Outlaw free = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(8, 1, 5));
        Outlaw jailed = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(2, 1, 5));
        jailed.setJailed();
        TownCats.spotOutlaws(cat, h.getLevel());
        h.assertTrue(free.hasEffect(MobEffects.GLOWING), "free outlaw not shown up");
        h.assertFalse(jailed.hasEffect(MobEffects.GLOWING), "jailed outlaw glowing");
        h.succeed();
    }

    /** A town cat's evening fire: the nearest lit campfire in reach, passing over an unlit one and one too far above. */
    static void catFindsHearth(GameTestHelper h) {
        BlockState lit = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        h.setBlock(new BlockPos(6, 1, 5), lit.setValue(CampfireBlock.LIT, false));
        h.setBlock(new BlockPos(5, 6, 5), lit); // nearer than the real one, but 5 up (the search reaches 4)
        h.setBlock(new BlockPos(9, 1, 9), lit);
        BlockPos found = CatHearthGoal.findFire(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)));
        h.assertTrue(h.absolutePos(new BlockPos(9, 1, 9)).equals(found), "found " + found + " instead of the lit fire at " + h.absolutePos(new BlockPos(9, 1, 9)));
        h.succeed();
    }

    /** Blueprint cats roll on joining the world: the sheriff's always wears the star, the ghost is a ghost, saloons favour their coat. */
    static void catsSettleByRole(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(5, 1, 5));
        List<Cat> spawned = new ArrayList<>();
        int saloonCoats = 0, ghosts = 0, starred = 0;
        try {
            for (int i = 0; i < 180; i++) {
                Role role = i < 40 ? Role.SALOON : i < 140 ? Role.GHOST : Role.SHERIFF; // 100 ghost spots at 15%
                Cat cat = EntityTypes.CAT.create(level, EntitySpawnReason.STRUCTURE);
                TownCat.set(cat, new TownCat(role, ItemStack.EMPTY, false, false, false));
                cat.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                cat.setNoAi(true);
                if (!level.addFreshEntity(cat)) {
                    h.assertTrue(TownCatOdds.presence(role) < 1.0F, role + " spot came up empty");
                    continue;
                }
                spawned.add(cat);
                TownCat data = TownCat.of(cat);
                h.assertTrue(data.settled() && cat.hasHome(), "cat didn't settle");
                if (role == Role.SALOON && cat.getVariant().is(CatCoats.SALOON)) saloonCoats++;
                if (role == Role.GHOST && data.ghost()) ghosts++;
                if (role == Role.SHERIFF && data.accessory() == Accessory.SHERIFF_STAR) starred++;
            }
        } finally {
            spawned.forEach(Entity::discard);
        }
        h.assertTrue(saloonCoats > 0, "40 saloon cats and no saloon coat");
        h.assertTrue(ghosts > 0 && ghosts < 50, "ghost presence looks wrong: " + ghosts + " of 100");
        h.assertTrue(starred == 40, "sheriff's cats without a star: " + (40 - starred));
        h.succeed();
    }
}

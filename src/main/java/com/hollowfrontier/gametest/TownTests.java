package com.hollowfrontier.gametest;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.furniture.SeatEntity;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Routine;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.Blueprint;
import com.hollowfrontier.worldgen.structure.TownBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

// Town life: bar stool seats, the jail, lawmen and townsfolk looks.
final class TownTests {
    private TownTests() {}

    static void barStoolSeatsPlayer(GameTestHelper h) {
        BlockPos stool = new BlockPos(5, 1, 5);
        h.setBlock(stool, ModBlocks.BAR_STOOL.get());
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        h.useBlock(stool, player);
        h.assertTrue(player.getVehicle() instanceof SeatEntity, "player not seated: " + player.getVehicle());
        double seatY = player.getVehicle().getY() - h.absolutePos(stool).getY();
        h.assertTrue(Math.abs(seatY - SeatEntity.SEAT_HEIGHT) < 1e-3, "seat at the wrong height: " + seatY);
        ServerPlayer other = TestPlayers.survival(h, new BlockPos(6, 1, 3));
        h.useBlock(stool, other);
        h.assertFalse(other.isPassenger(), "two players on one stool");

        h.setBlock(stool, Blocks.AIR);
        AABB around = new AABB(h.absolutePos(stool)).inflate(2);
        h.succeedWhen(() -> {
            h.assertFalse(player.isPassenger(), "still seated after the stool was broken");
            h.assertTrue(h.getLevel().getEntitiesOfClass(SeatEntity.class, around).isEmpty(), "seat left behind");
        });
    }

    static void jailedOutlawIsHarmless(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Outlaw outlaw = ModEntities.OUTLAW.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 9)), EntitySpawnReason.SPAWN_ITEM_USE);
        outlaw.setJailed();
        h.assertTrue(outlaw.getMainHandItem().isEmpty(), "jailed outlaw still armed");
        Lawman lawman = ModEntities.LAWMAN.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(9, 1, 5)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.runAfterDelay(80, () -> {
            h.assertTrue(player.getHealth() == player.getMaxHealth(), "jailed outlaw hurt the player");
            h.assertTrue(outlaw.getTarget() == null, "jailed outlaw picked a target: " + outlaw.getTarget());
            // Neighbouring tests have outlaws of their own in range, so only this prisoner must be left alone.
            h.assertTrue(lawman.getTarget() != outlaw, "lawman went after a jailed outlaw");
            h.assertTrue(outlaw.getHealth() == outlaw.getMaxHealth(), "jailed outlaw was shot");
            h.succeed();
        });
    }

    static void lawmanShootsOutlaw(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Lawman lawman = ModEntities.LAWMAN.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.assertFalse(lawman.getMainHandItem().isEmpty(), "lawman spawned unarmed");
        Outlaw outlaw = ModEntities.OUTLAW.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 9)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.succeedWhen(() -> h.assertTrue(!outlaw.isAlive() || outlaw.getHealth() < outlaw.getMaxHealth(),
                "lawman has not hit the outlaw yet (target " + lawman.getTarget() + ")"));
    }

    static void lawmanAnswersTownsfolk(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Townsfolk folk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(5, 1, 5));
        Lawman lawman = ModEntities.LAWMAN.get().spawn(level, h.absolutePos(new BlockPos(9, 1, 9)), EntitySpawnReason.SPAWN_ITEM_USE);
        folk.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        h.assertTrue(lawman.getTarget() == player, "lawman ignored an attack on a townsfolk");
        h.assertTrue(lawman.holdsFireFor(folk) && !lawman.holdsFireFor(ModEntities.OUTLAW.get().create(level, EntitySpawnReason.COMMAND)),
                "lawman fires through the wrong people");
        h.succeed();
    }

    static void townsfolkHaveLooks(GameTestHelper h) {
        Set<Folk> seen = EnumSet.noneOf(Folk.class);
        for (int i = 0; i < 20; i++) {
            Townsfolk folk = ModEntities.TOWNSFOLK.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(1 + i % 10, 1, 1 + i / 10)), EntitySpawnReason.SPAWN_ITEM_USE);
            seen.add(folk.getFolk());
            folk.discard();
        }
        h.assertTrue(seen.size() > 1, "every townsfolk looks the same: " + seen);
        h.succeed();
    }

    /** The sheriff's office comes staffed: a sheriff and a deputy, armed, and an unarmed prisoner in a cell. */
    static void sheriffOfficeStaffed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureTemplate office = level.getStructureTemplateManager().get(HollowFrontier.id("town/sheriff_office")).orElseThrow();
        BlockPos origin = h.absolutePos(new BlockPos(0, 80, 0));
        office.placeInWorld(level, origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), level.getRandom(), Block.UPDATE_CLIENTS);
        AABB area = new AABB(origin).inflate(16);
        List<Lawman> lawmen = level.getEntitiesOfClass(Lawman.class, area);
        List<AbstractOutlaw> prisoners = level.getEntitiesOfClass(AbstractOutlaw.class, area);
        try {
            h.assertTrue(lawmen.stream().anyMatch(l -> l.getRank() == Lawman.Rank.SHERIFF), "no sheriff in the office");
            h.assertTrue(lawmen.stream().noneMatch(l -> l.getMainHandItem().isEmpty()), "unarmed lawman");
            h.assertTrue(prisoners.size() == 1 && prisoners.get(0).isJailed() && prisoners.get(0).getMainHandItem().isEmpty(),
                    "expected one unarmed jailed outlaw, got " + prisoners);
            h.assertTrue(level.getBlockState(origin.offset(2, 1, 8)).is(Blocks.IRON_DOOR), "cell door missing");
        } finally {
            level.getEntitiesOfClass(Mob.class, area).forEach(Entity::discard);
        }
        h.succeed();
    }

    /**
     * The shops come furnished (23): goods on the general store's shelves and a dummy in a cowboy hat, its name over the
     * door, the gunsmith's guns in their case and its chest filled from its own table, the doctor's and post office's
     * chests likewise. The 9-wide shops stand one in on their 12-wide lots (24a).
     */
    static void townBuildingsFurnished(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos store = h.absolutePos(new BlockPos(0, 80, 0)), gunsmith = store.offset(14, 0, 0), doctor = store.offset(26, 0, 0),
                post = store.offset(0, 0, 16);
        java.util.Map<String, BlockPos> at = java.util.Map.of("town/general_store", store, "town/gunsmith", gunsmith, "town/doctor", doctor,
                "town/post_office", post);
        at.forEach((name, origin) -> level.getStructureTemplateManager().get(HollowFrontier.id(name)).orElseThrow()
                .placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS));
        AABB area = new AABB(store).inflate(40);
        try {
            h.assertTrue(level.getBlockEntity(store.offset(1, 2, 10)) instanceof net.minecraft.world.level.block.entity.ShelfBlockEntity shelf
                    && shelf.getItems().getFirst().is(net.minecraft.world.item.Items.BREAD), "the store's shelf has no bread");
            h.assertTrue(level.getBlockEntity(store.offset(5, 3, 1)) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign
                    && sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false).get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                    && key.getKey().equals(com.hollowfrontier.worldgen.structure.TownDecor.signKey("general_store")), "the store has no sign");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, area).stream()
                    .anyMatch(stand -> stand.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.hollowfrontier.registry.ModItems.COWBOY_HAT.get())),
                    "no dummy in a cowboy hat");
            h.assertTrue(level.getBlockEntity(gunsmith.offset(2, 2, 5)) instanceof net.minecraft.world.level.block.entity.ShelfBlockEntity rack
                    && rack.getItems().getFirst().is(com.hollowfrontier.registry.ModItems.PLAINS_RIFLE.get()), "the gunsmith's case has no rifle");
            java.util.Map<BlockPos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> chests = java.util.Map.of(
                    gunsmith.offset(2, 1, 9), com.hollowfrontier.worldgen.structure.ModChestLoot.GUNSMITH,
                    doctor.offset(8, 1, 9), com.hollowfrontier.worldgen.structure.ModChestLoot.DOCTOR,
                    post.offset(6, 1, 9), com.hollowfrontier.worldgen.structure.ModChestLoot.POST_OFFICE);
            chests.forEach((pos, table) -> h.assertTrue(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest
                    && table.equals(chest.getLootTable()), "no " + table.identifier() + " chest at " + pos));
        } finally {
            level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** 24c: a chair seats you at its own height and goes on facing the way it was set; a table stands. */
    static void chairSeatsPlayer(GameTestHelper h) {
        BlockPos chair = new BlockPos(5, 1, 5);
        h.setBlock(chair, ModBlocks.SPRUCE_CHAIR.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
        h.setBlock(new BlockPos(6, 1, 5), ModBlocks.SPRUCE_TABLE.get());
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        h.useBlock(chair, player);
        h.assertTrue(player.getVehicle() instanceof SeatEntity, "not seated on the chair: " + player.getVehicle());
        double seatY = player.getVehicle().getY() - h.absolutePos(chair).getY();
        h.assertTrue(Math.abs(seatY - com.hollowfrontier.content.frontier.furniture.ChairBlock.SEAT) < 1e-3, "chair seat at the wrong height: " + seatY);
        h.assertTrue(h.getBlockState(new BlockPos(6, 1, 5)).is(ModBlocks.SPRUCE_TABLE.get()), "the table didn't stand");
        TestPlayers.leave(player);
        h.succeed();
    }

    /** 24c: a town lamp's random tick sets it lit just when the world's dark outside. */
    static void townLampFollowsTheDay(GameTestHelper h) {
        BlockPos lamp = new BlockPos(3, 1, 3);
        h.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        boolean night = com.hollowfrontier.content.frontier.furniture.TownLampBlock.shouldBeLit(h.getLevel());
        h.setBlock(lamp, ModBlocks.TOWN_LAMP.get().defaultBlockState().setValue(com.hollowfrontier.content.frontier.furniture.TownLampBlock.LIT, !night));
        BlockPos abs = h.absolutePos(lamp);
        h.getLevel().getBlockState(abs).randomTick(h.getLevel(), abs, h.getLevel().getRandom());
        BlockState after = h.getBlockState(lamp);
        h.assertTrue(after.getValue(com.hollowfrontier.content.frontier.furniture.TownLampBlock.LIT) == night, "the lamp didn't follow the day (night " + night + ")");
        h.assertTrue(after.getLightEmission() == (night ? 15 : 0), "the lamp's light doesn't match it: " + after.getLightEmission());
        h.succeed();
    }

    /** 24c: the new trades each sell their own thing: the doctor healing, the editor the Gazette, the gunsmith ammunition. */
    static void newFolkTrade(GameTestHelper h) {
        java.util.Map<Folk, net.minecraft.world.item.Item> wares = java.util.Map.of(Folk.DOCTOR, net.minecraft.world.item.Items.POTION,
                Folk.EDITOR, ModItems.FRONTIER_GAZETTE.get(), Folk.POSTMASTER, net.minecraft.world.item.Items.MAP, Folk.TEACHER,
                net.minecraft.world.item.Items.WRITABLE_BOOK, Folk.PREACHER, net.minecraft.world.item.Items.CANDLE, Folk.GUNSMITH,
                ModItems.REVOLVER_CARTRIDGE.get());
        int i = 0;
        for (var entry : wares.entrySet()) {
            Townsfolk folk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(1 + i++, 1, 1));
            folk.setFolk(entry.getKey());
            h.assertTrue(folk.getOffers().stream().anyMatch(o -> o.getResult().is(entry.getValue())), entry.getKey() + " doesn't sell " + entry.getValue());
            folk.discard();
        }
        h.succeed();
    }

    /**
     * 24c: the day's phases, and where each sends a townsperson: their workplace by day, the saloon (its barmaid) of an
     * evening, a free bed by night.
     */
    static void routineSendsFolkAbout(GameTestHelper h) {
        h.assertTrue(com.hollowfrontier.content.frontier.townsfolk.Routine.phase(1000) == com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.WORK
                && com.hollowfrontier.content.frontier.townsfolk.Routine.phase(13000) == com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.EVENING
                && com.hollowfrontier.content.frontier.townsfolk.Routine.phase(18000) == com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.NIGHT
                && com.hollowfrontier.content.frontier.townsfolk.Routine.phase(24000 + 23800) == com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.WORK,
                "the day's phases are off");
        BlockPos home = h.absolutePos(new BlockPos(2, 1, 2));
        var level = h.getLevel();
        var work = com.hollowfrontier.content.frontier.townsfolk.Routine.place(level, home, com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.WORK);
        h.assertTrue(work.pos().equals(home), "by day they should keep to work, not " + work);
        Entity barmaid = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(8, 1, 8));
        var evening = com.hollowfrontier.content.frontier.townsfolk.Routine.place(level, home, com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.EVENING);
        h.assertTrue(evening.pos().closerThan(barmaid.blockPosition(), 2), "of an evening they should make for the saloon, not " + evening);
        h.setBlock(new BlockPos(4, 1, 2), Blocks.BED.red().defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.EAST)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        var night = com.hollowfrontier.content.frontier.townsfolk.Routine.place(level, home, com.hollowfrontier.content.frontier.townsfolk.Routine.Phase.NIGHT);
        h.assertTrue(night.bed() && night.pos().equals(h.absolutePos(new BlockPos(4, 1, 2))), "by night they should go to bed, not " + night);
        barmaid.discard();
        h.succeed();
    }

    /** Every hanging lantern and town lamp in every town piece hangs from something (a playtest found them floating in barns). */
    static void townLanternsHang(GameTestHelper h) {
        List<String> floating = new java.util.ArrayList<>();
        for (Blueprint blueprint : TownBlueprints.all()) {
            for (BlockPos pos : blueprint.positions()) {
                BlockState state = blueprint.get(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.LanternBlock && state.getValue(net.minecraft.world.level.block.LanternBlock.HANGING)) {
                    BlockState over = blueprint.get(pos.above());
                    if (over == null || over.isAir()) {
                        floating.add(blueprint.name() + " " + pos.toShortString());
                    }
                }
            }
        }
        h.assertTrue(floating.isEmpty(), "lanterns hanging from nothing: " + floating);
        h.succeed();
    }

    /** A bath (24b) sobers you up, freshens you (Regeneration) and lowers the tub. */
    static void bathSobersUp(GameTestHelper h) {
        BlockPos tub = new BlockPos(2, 1, 2);
        h.setBlock(tub, TownBlueprints.water(3));
        ServerPlayer player = TestPlayers.survival(h, tub);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.hollowfrontier.registry.ModMobEffects.TIPSY, 1200, 2));
        com.hollowfrontier.content.frontier.saloon.Baths.bathe(player);
        h.assertFalse(player.hasEffect(com.hollowfrontier.registry.ModMobEffects.TIPSY), "still tipsy after a bath");
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "no freshening after a bath");
        h.assertTrue(h.getBlockState(tub).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) == 2, "the tub didn't go down");
        TestPlayers.leave(player);
        h.succeed();
    }

    /**
     * Towns have names (24a): the same key always gives the same name, and keys give many different ones. A street's
     * welcome arch placed with the name processor outside any town reads "THIS TOWN" where the name goes.
     */
    static void townNamesAndArches(GameTestHelper h) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (long key = 0; key < 60; key++) {
            String name = com.hollowfrontier.worldgen.structure.TownNames.name(1234L, key * 7919);
            h.assertTrue(name.equals(com.hollowfrontier.worldgen.structure.TownNames.name(1234L, key * 7919)), "a town's name changed: " + name);
            names.add(name);
        }
        h.assertTrue(names.size() >= 30, "too few town names: " + names);
        ServerLevel level = h.getLevel();
        StructureTemplate street = level.getStructureTemplateManager().get(HollowFrontier.id("town/street_short")).orElseThrow();
        BlockPos origin = h.absolutePos(new BlockPos(0, 140, 0));
        street.placeInWorld(level, origin, origin, new StructurePlaceSettings().setIgnoreEntities(true)
                .addProcessor(com.hollowfrontier.worldgen.structure.TownNameProcessor.INSTANCE), level.getRandom(), Block.UPDATE_CLIENTS);
        BlockPos sign = origin.offset(4, 5, 44 - 1 - 2 + 1); // the arch two in from the street's end, its welcome sign outside it
        h.assertTrue(level.getBlockEntity(sign) instanceof net.minecraft.world.level.block.entity.SignBlockEntity board
                && board.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false).get(1).getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents line
                && line.getKey().equals("sign.hollowfrontier.this_town"), "the arch's name line wasn't filled in at " + sign);
        h.succeed();
    }

    /**
     * Every town has a gunsmith, the only one who sells guns, and a post office, whose postmaster holds the Polar Survey's
     * letter (32d): each style's every square has one lot on each one's pool, and that pool, registered, draws only it.
     */
    static void everyTownHasGunsmith(GameTestHelper h) {
        var pools = h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        for (com.hollowfrontier.worldgen.structure.TownStyle style : com.hollowfrontier.worldgen.structure.TownStyle.values()) {
            for (String building : List.of(com.hollowfrontier.worldgen.structure.TownLayout.GUNSMITH, com.hollowfrontier.worldgen.structure.TownLayout.POST_OFFICE)) {
                String pool = style.pool(building);
                h.assertTrue(pools.containsKey(net.minecraft.resources.Identifier.parse(pool)), pool + " isn't registered");
                h.assertValueEqual(TownBlueprints.pools(style).get(building).keySet(), Set.of(building), pool + "'s buildings");
                for (String square : TownBlueprints.pools(style).get(com.hollowfrontier.worldgen.structure.TownLayout.SQUARES).keySet()) {
                    StructureTemplate t = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(style.template(square))).orElseThrow();
                    long lots = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW).stream()
                            .filter(info -> info.nbt().getStringOr("pool", "").equals(pool)).count();
                    h.assertValueEqual(lots, 1L, style.template(square) + "'s " + building + " lots");
                }
            }
        }
        h.succeed();
    }

    /**
     * Every door in every town building opens onto somewhere you can walk, on both sides: nothing solid in the way at
     * foot height (a carpet is fine, and a bottom slab or a stair, a step up, if nothing is over the doorway to knock
     * your head on as you step) and nothing at head height. Nothing drawn means open ground.
     */
    static void townDoorsAreClear(GameTestHelper h) {
        List<String> blocked = new java.util.ArrayList<>();
        for (Blueprint blueprint : TownBlueprints.all()) {
            for (BlockPos pos : blueprint.positions()) {
                BlockState door = blueprint.get(pos);
                if (!(door.getBlock() instanceof DoorBlock) || door.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }
                Direction facing = door.getValue(DoorBlock.FACING);
                for (Direction side : List.of(facing, facing.getOpposite())) {
                    BlockPos foot = pos.relative(side);
                    // A step right at the door lifts your head into whatever is over the doorway as you step up onto it
                    boolean stepUnderLintel = height(blueprint.get(foot)) > 0.2 && !passable(blueprint.get(pos.above(2)), false);
                    if (!passable(blueprint.get(foot), true) || !passable(blueprint.get(foot.above()), false) || stepUnderLintel) {
                        blocked.add(blueprint.name() + " " + pos.toShortString() + " " + side + (stepUnderLintel ? " (step under the lintel)" : ""));
                    }
                }
            }
        }
        h.assertTrue(blocked.isEmpty(), "blocked doors: " + blocked);
        h.succeed();
    }

    /**
     * 32a: no carpet on the tile either side of a door in any town building. Townsfolk got stuck on carpet laid up to a
     * door in a playtest; the threshold stays bare floor.
     */
    static void townDoorsHaveBareThresholds(GameTestHelper h) {
        List<String> carpeted = new java.util.ArrayList<>();
        for (Blueprint blueprint : TownBlueprints.all()) {
            for (BlockPos pos : blueprint.positions()) {
                BlockState door = blueprint.get(pos);
                if (!(door.getBlock() instanceof DoorBlock) || door.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }
                Direction facing = door.getValue(DoorBlock.FACING);
                for (Direction side : List.of(facing, facing.getOpposite())) {
                    BlockState threshold = blueprint.get(pos.relative(side));
                    if (threshold != null && threshold.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock) {
                        carpeted.add(blueprint.name() + " " + pos.toShortString() + " " + side);
                    }
                }
            }
        }
        h.assertTrue(carpeted.isEmpty(), "carpet at a door: " + carpeted);
        h.succeed();
    }

    /** 32a: the same in a shipped template (the blueprints are drawn again in datagen): the sheriff's rug stops short of his door. */
    static void sheriffRugClearsTheDoor(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 0, 5);
        place(h, "town/sheriff_office", at);
        h.assertTrue(h.getBlockState(at.offset(4, 1, 2)).getBlock() instanceof DoorBlock, "no door at the sheriff's office's 4, 1, 2");
        h.assertFalse(h.getBlockState(at.offset(4, 1, 3)).getBlock() instanceof net.minecraft.world.level.block.CarpetBlock, "carpet inside the sheriff's door");
        h.assertTrue(h.getBlockState(at.offset(4, 1, 4)).getBlock() instanceof net.minecraft.world.level.block.CarpetBlock, "the rest of the rug is gone");
        h.succeed();
    }

    /**
     * 32a: a townsperson walks through a closed door into a room, as their routine sends them, with {@code carpets} (x, z
     * pairs on the floor) laid about it. A wall runs along z 5 with an oak door at x 5; they start at z 2 and head for
     * z 10. A playtest saw folk stuck on carpet by a door, opening and shutting it over and over.
     */
    private static void folkWalkThroughDoor(GameTestHelper h, int... carpets) {
        for (int x = -3; x <= 14; x++) { // past the platform's edges, so the only way in is the door
            for (int y = 1; y <= 3; y++) {
                h.setBlock(new BlockPos(x, y, 5), Blocks.OAK_PLANKS);
            }
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        h.setBlock(new BlockPos(5, 1, 5), door);
        h.setBlock(new BlockPos(5, 2, 5), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int i = 0; i < carpets.length; i += 2) {
            h.setBlock(new BlockPos(carpets[i], 1, carpets[i + 1]), Blocks.CARPET.red());
        }
        Townsfolk folk = h.spawn(ModEntities.TOWNSFOLK.get(), new BlockPos(5, 1, 2));
        folk.goalSelector.removeAllGoals(goal -> goal instanceof Routine.RoutineGoal); // or by night they'd head back to where they were put
        BlockPos goal = h.absolutePos(new BlockPos(5, 1, 10));
        h.succeedWhen(() -> {
            if (folk.getNavigation().isDone()) {
                folk.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.5);
            }
            h.assertTrue(folk.getZ() >= goal.getZ() - 1, "stuck at " + h.relativeVec(folk.position()));
            folk.discard();
        });
    }

    /**
     * 32a: a townsperson who can't reach their place for the time of day (here they're shut in a cell well away from their
     * workplace) gives up for a while instead of setting off again every second.
     */
    static void folkGiveUpOnUnreachablePlace(GameTestHelper h) {
        BlockPos work = new BlockPos(10, 1, 10), cell = new BlockPos(1, 1, 1);
        Townsfolk folk = h.spawn(ModEntities.TOWNSFOLK.get(), work);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.absolutePos(work).equals(folk.getWorkplace()), "the workplace should be where they were put, not " + folk.getWorkplace());
            for (BlockPos pos : BlockPos.betweenClosed(cell.offset(-1, 0, -1), cell.offset(1, 2, 1))) {
                if (pos.getX() != cell.getX() || pos.getZ() != cell.getZ() || pos.getY() == cell.getY() + 2) {
                    h.setBlock(pos, Blocks.STONE);
                }
            }
            Vec3 inside = h.absoluteVec(Vec3.atBottomCenterOf(cell));
            folk.snapTo(inside.x, inside.y, inside.z, 0, 0);
            Routine.RoutineGoal routine = folk.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal())
                    .filter(Routine.RoutineGoal.class::isInstance).map(Routine.RoutineGoal.class::cast).findFirst().orElseThrow();
            h.succeedWhen(() -> {
                h.assertTrue(routine.resting(), "still trying for the workplace from the cell, at " + h.relativeVec(folk.position()));
                folk.discard();
            });
        });
    }

    static void folkPassBareDoor(GameTestHelper h) {
        folkWalkThroughDoor(h);
    }

    // Carpet on the threshold itself (z 4 or 6 at x 5) is what sticks them, which Blueprint.bareThresholds keeps out of
    // the towns; carpet diagonal to the doorway, as a rug that stops short of it leaves, is fine.
    static void folkPassDoorBesideCarpet(GameTestHelper h) {
        folkWalkThroughDoor(h, 4, 6, 6, 6, 4, 4, 6, 4);
    }

    private static double height(BlockState state) {
        if (state == null) {
            return 0;
        }
        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        return shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y);
    }

    /**
     * Each staircase to an upper floor is walked up, as a player holding forward would (stepping, never jumping): onto
     * it straight from the floor, and off the top onto the floor above.
     */
    static void townStairsClimb(GameTestHelper h) {
        record Flight(String building, BlockPos at, Vec3 from, Direction dir, double floor) {}
        List<Flight> flights = List.of(
                new Flight("town/hotel", new BlockPos(1, 0, 1), new Vec3(9.5, 1, 3.5), Direction.SOUTH, 6),
                new Flight("town/hotel_grand", new BlockPos(20, 0, 1), new Vec3(11.5, 1, 11.5), Direction.NORTH, 6),
                new Flight("town/hotel_grand", new BlockPos(20, 0, 1), new Vec3(10.5, 6, 3.5), Direction.SOUTH, 10),
                new Flight("town/saloon_grand", new BlockPos(1, 0, 22), new Vec3(13.5, 1, 3.5), Direction.SOUTH, 6),
                new Flight("town/courthouse", new BlockPos(20, 0, 22), new Vec3(15.5, 2, 14.5), Direction.NORTH, 7));
        flights.stream().map(f -> java.util.Map.entry(f.building(), f.at())).distinct().forEach(e -> place(h, e.getKey(), e.getValue()));
        List<String> jumps = new java.util.ArrayList<>();
        for (Flight f : flights) {
            Vec3 end = walk(h, f.at(), f.from(), f.dir(), 7);
            if (end.y < f.floor() - 0.01) {
                jumps.add(f.building() + " from " + f.from() + ": stopped at " + end);
            }
        }
        h.assertTrue(jumps.isEmpty(), "stairs that need a jump: " + jumps);
        h.succeed();
    }

    /** Through the depot from the street, out of the back door and up onto the platform, both doors open. */
    static void depotWalkThrough(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 0, 5);
        place(h, "town/depot", at);
        for (int z : new int[] {2, 8}) {
            BlockPos door = h.absolutePos(at.offset(12, 1, z));
            BlockState state = h.getLevel().getBlockState(door);
            h.assertTrue(state.getBlock() instanceof DoorBlock, "no door at the depot's z " + z);
            ((DoorBlock) state.getBlock()).setOpen(null, h.getLevel(), state, door, true);
        }
        Vec3 end = walk(h, at, new Vec3(12.5, 1, 0.5), Direction.SOUTH, 11);
        h.assertTrue(end.z >= 11 && end.y >= 2 - 0.01, "stuck in the depot at " + end + " (template coordinates)");
        h.succeed();
    }

    private static void place(GameTestHelper h, String name, BlockPos at) {
        StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name)).orElseThrow();
        BlockPos origin = h.absolutePos(at);
        template.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
    }

    /**
     * Walks a fake player from {@code from} (feet, in the coordinates of a template placed at {@code at}) {@code blocks}
     * along {@code dir} the way the game moves a player: settle onto the ground, then a small step forward (climbing
     * what the player's step height allows), over and over. Returns where they end up, in the same coordinates.
     */
    private static Vec3 walk(GameTestHelper h, BlockPos at, Vec3 from, Direction dir, double blocks) {
        Vec3 origin = Vec3.atLowerCornerOf(h.absolutePos(at));
        ServerPlayer player = TestPlayers.survival(h, at);
        player.snapTo(origin.x + from.x, origin.y + from.y, origin.z + from.z, dir.toYRot(), 0.0F);
        double stride = 0.1;
        for (int i = 0; i < blocks / stride; i++) {
            player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -0.5, 0));
            player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(dir.getStepX() * stride, 0, dir.getStepZ() * stride));
        }
        Vec3 end = player.position().subtract(origin);
        TestPlayers.leave(player);
        return end;
    }

    private static boolean passable(BlockState state, boolean stepOk) {
        if (state == null) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (shape.isEmpty()) {
            return true;
        }
        return stepOk && (shape.max(Direction.Axis.Y) <= 0.5
                || state.getBlock() instanceof StairBlock && state.getValue(StairBlock.HALF) == Half.BOTTOM);
    }
}

package com.hollowfrontier.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

// Fake players for game tests. Mirrors GameTestHelper.makeMockServerPlayerInLevel but lets us pick survival.
final class TestPlayers {
    private TestPlayers() {}

    static ServerPlayer survival(GameTestHelper helper, BlockPos relativePos) {
        return create(helper, relativePos, GameType.SURVIVAL, "hf-test-player");
    }

    /** A survival player with a name of its own (where who did it matters: a poster TAKEN by someone). */
    static ServerPlayer survival(GameTestHelper helper, BlockPos relativePos, String name) {
        return create(helper, relativePos, GameType.SURVIVAL, name);
    }

    static ServerPlayer creative(GameTestHelper helper, BlockPos relativePos) {
        return create(helper, relativePos, GameType.CREATIVE, "hf-test-player");
    }

    /**
     * Takes fake players back out of the world. A survival player left standing around can keep another test's boss
     * awake (archon_resets), so every fake player also leaves by itself when its test passes or fails.
     */
    static void leave(ServerPlayer... players) {
        for (ServerPlayer player : players) {
            var list = player.level().getServer().getPlayerList();
            if (list.getPlayer(player.getUUID()) == player) {
                list.remove(player);
            }
        }
    }

    // Sends the player away once its test is over, however it ended.
    private static void leaveWithTest(GameTestHelper helper, ServerPlayer player) {
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {}

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                leave(player);
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                leave(player);
            }

            @Override
            public void testAddedForRerun(GameTestInfo info, GameTestInfo rerun, GameTestRunner runner) {}
        });
    }

    private static ServerPlayer create(GameTestHelper helper, BlockPos relativePos, GameType mode, String name) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return mode;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection); // so mods that sync on login (Curios) can send to it
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        BlockPos abs = helper.absolutePos(relativePos);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0F, 0.0F);
        leaveWithTest(helper, player);
        return player;
    }
}

package com.hollowfrontier.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.AirshipSighting;
import com.hollowfrontier.content.frontier.saloon.Drink;
import com.hollowfrontier.content.frontier.saloon.Slur;
import com.hollowfrontier.network.PhantomSightingPayload;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * How the drink takes the local player (21b), all on this client:
 * <ul>
 * <li><b>Slurring</b> ({@link Slur}) from Tipsy II: what NPCs say in chat, and the Gazette they open.</li>
 * <li><b>Hiccups</b> from Tipsy II, about once every {@link #HICCUP_EVERY} ticks.</li>
 * <li><b>The moonshine's sighting</b> ({@link PhantomSightingPayload}): an airship sighting (11a) in this sky only, gone
 *     again after its run. Nobody else sees it, and it's never logged.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class TipsyClient {
    private TipsyClient() {}

    public static final int HICCUP_EVERY = 400;
    /** Half the phantom's run: shorter than a real sighting's, so it starts inside the chunks this client has loaded (one outside them wouldn't tick, so wouldn't move). */
    public static final double PHANTOM_HALF_RUN = 48.0;
    /** Client-only entity ids (well clear of the server's), and how long each phantom has left. */
    private static int nextId = -1_000_000;
    private static final Map<Entity, Integer> PHANTOMS = new LinkedHashMap<>();

    private static int tipsy() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : Drink.tipsyLevel(player);
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent.System event) {
        int level = tipsy();
        if (event.isOverlay() || level < Slur.FROM_LEVEL || !(event.getMessage().getContents() instanceof TranslatableContents said)
                || !said.getKey().equals("message.hollowfrontier.npc.say") || said.getArgs().length < 2) {
            return;
        }
        Object[] args = said.getArgs();
        Component line = args[1] instanceof Component c ? c : Component.literal(String.valueOf(args[1]));
        event.setMessage(Component.translatable(said.getKey(), args[0], Slur.slur(line, level)).withStyle(event.getMessage().getStyle()));
    }

    /** A slurred Gazette, so it isn't slurred twice. */
    private static final class SlurredBook extends BookViewScreen {
        SlurredBook(BookAccess book) {
            super(book);
        }
    }

    @SubscribeEvent
    static void onOpen(ScreenEvent.Opening event) {
        int level = tipsy();
        LocalPlayer player = Minecraft.getInstance().player;
        if (level < Slur.FROM_LEVEL || player == null || !(event.getNewScreen() instanceof BookViewScreen) || event.getNewScreen() instanceof SlurredBook) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModItems.FRONTIER_GAZETTE.get())) {
                BookViewScreen.BookAccess book = BookViewScreen.BookAccess.fromItem(held);
                if (book != null) {
                    event.setNewScreen(new SlurredBook(new BookViewScreen.BookAccess(book.pages().stream().map(p -> Slur.slur(p, level)).toList())));
                }
                return;
            }
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && !mc.isPaused() && tipsy() >= Slur.FROM_LEVEL && mc.player.getRandom().nextInt(HICCUP_EVERY) == 0) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS,
                    0.6F, 1.8F + mc.player.getRandom().nextFloat() * 0.2F, false);
        }
        for (Iterator<Map.Entry<Entity, Integer>> it = PHANTOMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Integer> phantom = it.next();
            int left = phantom.getValue() - 1;
            if (left <= 0 || phantom.getKey().level() != mc.level) {
                if (phantom.getKey().level() instanceof ClientLevel level) {
                    level.removeEntity(phantom.getKey().getId(), Entity.RemovalReason.DISCARDED);
                }
                it.remove();
            } else {
                phantom.setValue(left);
            }
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PhantomSightingPayload.TYPE, (payload, context) -> context.enqueueWork(() -> phantom(payload.angle())));
    }

    /** An airship sighting in this sky only, crossing high over the player on {@code angle}. */
    static void phantom(float angle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        AirshipSighting sighting = ModEntities.AIRSHIP_SIGHTING.get().create(mc.level, EntitySpawnReason.EVENT);
        if (sighting == null) {
            return;
        }
        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 over = mc.player.position();
        sighting.setId(nextId--);
        sighting.setPos(new Vec3(over.x, Math.min(over.y + 50, mc.level.getMaxY() - 8), over.z).subtract(direction.scale(PHANTOM_HALF_RUN)));
        sighting.launch(direction, 0);
        mc.level.addEntity(sighting);
        mc.level.playLocalSound(over.x, over.y + 20, over.z, ModSounds.SIGHTING_HUM.get(), SoundSource.AMBIENT, 2.0F, 0.6F, false);
        PHANTOMS.put(sighting, (int) (2 * PHANTOM_HALF_RUN / AirshipSighting.SPEED));
    }
}

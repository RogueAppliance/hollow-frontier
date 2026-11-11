package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.season.TownTree;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * A town's December tree (35b, {@code town_tree}, made by tools/models/TreeModels.java): spruce boughs in tiers over a
 * tub, tin stars and candles. The candles' flames are the {@code flames} bone, drawn only while the tree is lit (after
 * dark), and they're all the glow mask holds, so the tree glows only then.
 */
public class TownTreeRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<TownTree, R> {
    static final DataTicket<Boolean> LIT = DataTicket.create("hollowfrontier_town_tree_lit", Boolean.class);

    public TownTreeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id("town_tree")));
        withRenderLayer(AutoGlowingGeoLayer::new);
        this.shadowRadius = 1.6F;
    }

    @Override
    public void captureDefaultRenderState(TownTree tree, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(tree, relatedObject, state, partialTick);
        state.addGeckolibData(LIT, tree.isLit());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        boolean lit = info.renderState().getOrDefaultGeckolibData(LIT, false);
        snapshots.ifPresent("flames", bone -> bone.skipRender(!lit));
    }
}

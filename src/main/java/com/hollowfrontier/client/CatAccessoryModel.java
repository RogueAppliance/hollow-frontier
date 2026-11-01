package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.TownCat.Accessory;

import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * What a grown cat wears, as real geometry parented to the vanilla cat's bones so it follows walking, sitting and lying:
 * a tiny cowboy hat lying flat on the head with the ears poking through its brim, a bandana knotted round the neck with a
 * kerchief over the chest, and a brass star on a leather collar (all on the body). The parts sit at the vanilla cat's
 * pivots and get its 0.8 scale, and setupAnim is the vanilla cat's. Texture layout (32x32), shared by the three accessory
 * textures: hat crown 2x2x3 @0,0 | brim 8x1x7 @0,6 (ear holes) | band/collar 4x2x6 @0,14 | kerchief 3x1x3 @20,14 | star 4x1x4 @0,24.
 * On the chest plates (kerchief, star) texture row 0 is the top, toward the head.
 * <p>
 * One model instance per accessory, each showing only its own parts: rendering is deferred, so a shared instance whose
 * part visibility changed between cats would draw the last cat's choice on all of them.
 */
public class CatAccessoryModel extends AdultCatModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(HollowFrontier.id("cat"), "accessories");

    public CatAccessoryModel(ModelPart root, Accessory shown) {
        super(root);
        head.getChild("hat").visible = shown == Accessory.COWBOY_HAT;
        body.getChild("bandana").visible = shown == Accessory.BANDANA;
        body.getChild("kerchief").visible = shown == Accessory.BANDANA;
        body.getChild("collar").visible = shown == Accessory.SHERIFF_STAR;
        body.getChild("star").visible = shown == Accessory.SHERIFF_STAR;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Same bones and rest poses as AdultFelineModel.createBodyMesh, without its cubes.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, -9.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 12.0F, -10.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        root.addOrReplaceChild("tail1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 15.0F, 8.0F, 0.9F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0F, 20.0F, 14.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(), PartPose.offset(1.1F, 18.0F, 5.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offset(-1.1F, 18.0F, 5.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create(), PartPose.offset(1.2F, 14.1F, -5.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create(), PartPose.offset(-1.2F, 14.1F, -5.0F));

        // Hat: the head is x -2.5..2.5, y -2..2 (top at -2), z -3..2; the ears are x ±1..2, y -3..-2, z 0..2. The brim (a
        // 1px box thinned to half) lies flat on the head, and the ears poke up through holes in its texture: it spans
        // x -4..4, z -4..3 so each ear covers exactly one brim pixel column and two rows. The crown sits in the middle of the
        // head between the ears (x -1..1), so both show beside it from the front.
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -4.25F, -2.0F, 2.0F, 2.0F, 3.0F)
                        .texOffs(0, 6).addBox(-4.0F, -2.5F, -4.0F, 8.0F, 1.0F, 7.0F, new CubeDeformation(0.0F, -0.25F, 0.0F)),
                PartPose.ZERO);
        // Body space: length along y from the chest (y 3) to the tail, belly at z -8, back at z -2.
        body.addOrReplaceChild("bandana", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-2.0F, 3.5F, -8.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
        body.addOrReplaceChild("kerchief", CubeListBuilder.create()
                .texOffs(20, 14).addBox(-1.5F, 2.2F, -8.4F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F, -0.3F, 0.0F)), PartPose.ZERO);
        // The star rides on a leather collar (the bandana band's shape, in the star texture) so the sheriff's cat reads from
        // any side. Its plate spans the chest below the chin (it must stay below the head, which overlaps the top of the
        // chest) and hangs a pixel past the belly line so it still shows when the cat sits.
        body.addOrReplaceChild("collar", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-2.0F, 3.5F, -8.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
        body.addOrReplaceChild("star", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.0F, 2.2F, -9.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F, -0.35F, 0.0F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).apply(AdultCatModel.CAT_TRANSFORMER);
    }
}

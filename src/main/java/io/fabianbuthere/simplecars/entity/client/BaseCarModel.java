// Made with Blockbench 4.12.5
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

package io.fabianbuthere.simplecars.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class BaseCarModel<T extends Entity> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	private final ModelPart chassis;

	public BaseCarModel(ModelPart root) {
		this.chassis = root.getChild("Chasis");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Chasis = partdefinition.addOrReplaceChild("Chasis", CubeListBuilder.create().texOffs(-20, -20).addBox(-13.0F, -7.0F, -17.0F, 4.0F, 9.0F, 22.0F, new CubeDeformation(0.0F))
				.texOffs(-9, -9).addBox(-13.0F, -7.0F, 32.0F, 4.0F, 9.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(-24, -19).addBox(-9.0F, -10.0F, -16.0F, 12.0F, 11.0F, 21.0F, new CubeDeformation(0.0F))
				.texOffs(-22, -20).addBox(3.0F, -7.0F, -17.0F, 4.0F, 9.0F, 22.0F, new CubeDeformation(0.0F))
				.texOffs(-45, -35).addBox(-11.0F, -11.0F, 5.0F, 16.0F, 13.0F, 37.0F, new CubeDeformation(0.0F))
				.texOffs(-7, 0).addBox(-11.0F, -19.0F, 40.0F, 16.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(-7, 0).addBox(-11.0F, -19.0F, 8.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(-38, -31).addBox(-11.0F, -20.0F, 8.0F, 16.0F, 1.0F, 33.0F, new CubeDeformation(0.0F))
				.texOffs(-25, -25).mirror().addBox(-13.0F, 0.0F, 5.0F, 2.0F, 2.0F, 27.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(-25, -25).addBox(5.0F, 0.0F, 5.0F, 2.0F, 2.0F, 27.0F, new CubeDeformation(0.0F))
				.texOffs(0, 1).addBox(-11.0F, -18.0F, 41.0F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 1).addBox(3.0F, -18.0F, 41.0F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(4.0F, -18.0F, 40.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(-11.0F, -18.0F, 40.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-13.0F, -6.0F, 5.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-13.0F, -6.0F, 30.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(-13.0F, -6.0F, 43.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-13.0F, -3.0F, 7.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-13.0F, -3.0F, 28.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(-13.0F, -3.0F, 44.0F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(5.0F, -6.0F, 5.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(5.0F, -3.0F, 7.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(5.0F, -3.0F, 28.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(5.0F, -6.0F, 30.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(-9, -9).addBox(3.0F, -7.0F, 32.0F, 4.0F, 9.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(3.0F, -3.0F, 44.0F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(1, 1).addBox(3.0F, -6.0F, 43.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(-22, -19).addBox(-7.0F, -11.0F, -16.0F, 8.0F, 1.0F, 21.0F, new CubeDeformation(0.0F))
				.texOffs(-11, -11).addBox(3.0F, -9.0F, -12.0F, 4.0F, 2.0F, 13.0F, new CubeDeformation(0.0F))
				.texOffs(-11, -11).addBox(-13.0F, -9.0F, -12.0F, 4.0F, 2.0F, 13.0F, new CubeDeformation(0.0F))
				.texOffs(-1, -1).addBox(3.0F, -8.0F, -15.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(-1, -1).addBox(-13.0F, -8.0F, -15.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(-1, -1).addBox(-13.0F, -8.0F, 1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(-1, -1).addBox(3.0F, -8.0F, 1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(-3, -3).addBox(-13.0F, -2.0F, 9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(-3, -3).addBox(5.0F, -2.0F, 9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(-12, -4).addBox(-9.0F, -7.0F, 42.0F, 12.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(-8, 0).addBox(-9.0F, -9.0F, 44.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(-30, -30).addBox(3.0F, -19.0F, 8.0F, 2.0F, 1.0F, 32.0F, new CubeDeformation(0.0F))
				.texOffs(-30, -30).addBox(-11.0F, -19.0F, 8.0F, 2.0F, 1.0F, 32.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 20.0F, -16.0F));

		PartDefinition cube_r1 = Chasis.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(-8, 0).mirror().addBox(-3.0F, -2.2F, -0.67F, 12.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.0F, -7.0F, 43.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r2 = Chasis.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(-8, 0).mirror().addBox(-3.0F, -2.1F, -1.3F, 12.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.0F, -7.0F, 47.0F, 0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r3 = Chasis.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(1, 1).addBox(-1.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(14.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-7, 1).addBox(1.0F, -6.0F, -1.0F, 12.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -11.0F, 7.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r4 = Chasis.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(1, 1).addBox(-1.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(13.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(14.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -11.0F, 6.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r5 = Chasis.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(-6, 1).addBox(-8.0F, -1.9F, -0.67F, 16.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -17.0F, 8.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r6 = Chasis.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(-7, 0).addBox(-8.0F, -1.0F, -1.0F, 16.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -18.0F, 8.0F, -0.2182F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// TODO: Animations
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		chassis.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return chassis;
	}
}
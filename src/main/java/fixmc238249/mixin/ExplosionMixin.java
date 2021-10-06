package fixmc238249.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private World world;
    @Shadow @Final private float power;

    private final BlockPos posOrigin = new BlockPos(x, y, z);
    private Float[][][] blastResNearOrigin = new Float[5][5][5];
    private Boolean[][][] isPosNearOriginAir = new Boolean[5][5][5];

    @Inject(
            method = "collectBlocksAndDamageEntities",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/explosion/Explosion;z:D"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fixMc238249(CallbackInfo ci, Set<BlockPos> toExplode, int i, int k, int l, int j, double xoffset, double yoffset, double zoffset, double g, float rayStrength, double x, double y, double z) {
        rayStrength = -rayStrength; // had to hackfix yeetage of vanilla for loop
        while (rayStrength > 0.0F) {
            BlockPos blockPos = new BlockPos(x, y, z);

            int xindex = posOrigin.getX() - blockPos.getX() + 2;
            int yindex = posOrigin.getY() - blockPos.getY() + 2;
            int zindex = posOrigin.getZ() - blockPos.getZ() + 2;

            float blastResOfBlock;
            boolean isBlockAir;

            if (!isWithinBounds(xindex, yindex, zindex) || !isPositionCached(xindex, yindex, zindex)) {
                BlockState blockState = this.world.getBlockState(blockPos);
                FluidState fluidState = this.world.getFluidState(blockPos);
                blastResOfBlock = (Math.max(blockState.getBlock().getBlastResistance(), fluidState.getBlastResistance()) + 0.3F) * 0.3F;
                isBlockAir = blockState.isAir() && fluidState.isEmpty();
                if (isWithinBounds(xindex, yindex, zindex)) {
                    this.blastResNearOrigin[xindex][yindex][zindex] = blastResOfBlock;
                    isPosNearOriginAir[xindex][yindex][zindex] = isBlockAir;
                }
            } else {
                blastResOfBlock = blastResNearOrigin[xindex][yindex][zindex];
                isBlockAir = isPosNearOriginAir[xindex][yindex][zindex];
            }
            if (!isBlockAir) {
                rayStrength -= blastResOfBlock;
            }

            if (rayStrength > 0.0F) {
                toExplode.add(blockPos);
            }

            rayStrength -= 0.225F;

            x += xoffset * 0.3D;
            y += yoffset * 0.3D;
            z += zoffset * 0.3D;
        }
    }

    @Redirect(
            method = "collectBlocksAndDamageEntities",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/explosion/Explosion;power:F"
            )
    )
    private float removeVanillaForLoop() {
        return -this.power;
    }

    private boolean isWithinBounds(int xindex, int yindex, int zindex) {
        return xindex >= 0 && xindex <= 4 && yindex >= 0 && yindex <= 4 && zindex >= 0 && zindex <= 4;
    }

    private boolean isPositionCached(int xindex, int yindex, int zindex) {
        return blastResNearOrigin[xindex][yindex][zindex] != null;
    }
}

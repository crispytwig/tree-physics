package com.farcr.treephysics.collision_callback;

import com.farcr.treephysics.api.TreeUtil;
import com.farcr.treephysics.api.manager.ServerTreeManager;
import com.farcr.treephysics.client.TreeManager;
import com.farcr.treephysics.index.TreePhysicsTags;
import com.farcr.treephysics.particle.collision_dust.CollisionDustParticleOptions;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class LogCallback extends FragileBlockCallback {

    private static final Vector3dc[] OFFSETS = new Vector3d[] {
            new Vector3d(0, -1, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(-1, 0, 0),
            new Vector3d(0, 0, 1),
            new Vector3d(0, 0, -1),
            new Vector3d(0, 1, 0),
    };

    private static final Vector3d offsetPos = new Vector3d();
    private static final BlockPos.MutableBlockPos offsetBlockPos = new BlockPos.MutableBlockPos();

    public static final LogCallback INSTANCE = new LogCallback();

    @Override
    public double getTriggerVelocity() {
        return 2;
    }

    @Override
    public boolean shouldTriggerFor(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    @Override
    public CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d hitPos) {
        TreeManager treeManager = TreeManager.get(level);
        SubLevel subLevel = treeManager.getTree(pos);
        if(subLevel != null) {
            subLevel.logicalPose().transformPosition(hitPos);

            BlockState dustState = null;
            float dist = 0.3f;

            for (Vector3dc offset : OFFSETS) {
                offset.mul(dist, offsetPos);
                offsetPos.add(hitPos);
                BlockPos.MutableBlockPos blockPos = offsetBlockPos.set(offsetPos.x, offsetPos.y, offsetPos.z);
                BlockState blockState = level.getBlockState(blockPos);
                if(!blockState.isAir() && blockState.isSolid()) {
                    dustState = blockState;
                    break;
                }
            }

            if(dustState != null && dustState.is(TreePhysicsTags.PRODUCES_DUST_ON_IMPACT)) {
                level.sendParticles(new CollisionDustParticleOptions(dustState), hitPos.x, hitPos.y, hitPos.z, 6, 0, 0, 0, 1);
            }

            double uprightness = TreeUtil.getUprightness(subLevel);
            if(uprightness < 0.75) {
                ((ServerTreeManager) treeManager).startBreakingLeaves(subLevel);
            }
        }

        return CollisionResult.NONE;
    }
}

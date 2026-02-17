package dev.byt3.bloodmagic.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import dev.byt3.bloodmagic.components.BloodLinkMaster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BloodManaSystems {
    public static void registerSystems(BloodMagicPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new BloodManaSetSystem());
        plugin.getEntityStoreRegistry().registerSystem(new BloodManaValidateSystem());
    }

    public static class BloodManaSetSystem extends EntityTickingSystem<EntityStore> {
        private float tickTimer = 0f;
        private static final float TICK_INTERVAL = .5f; // set stat value every Interval seconds

        @Override
        public void tick(float dt, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            tickTimer += dt;
            if (tickTimer < TICK_INTERVAL) {
                return;
            }
            tickTimer = 0f;
            BloodLinkMaster master = archetypeChunk.getComponent(i, BloodLinkMaster.getComponentType());
            if (master == null) return;
            EntityStatMap statMap = archetypeChunk.getComponent(i, EntityStatMap.getComponentType());
            if (statMap == null) return;
            statMap.setStatValue(EntityStatType.getAssetMap().getIndex("BloodMana"), master.getTotalLinkedHealth());
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return BloodLinkMaster.getComponentType();
        }
    }

    public static class BloodManaValidateSystem extends EntityTickingSystem<EntityStore> {
        private float tickTimer = 0f;
        private static final float TICK_INTERVAL = 2f; // validate every Interval seconds

        @Override
        public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            tickTimer += v;
            if (tickTimer < TICK_INTERVAL) {
                return;
            }
            tickTimer = 0f;
            BloodLinkMaster master = archetypeChunk.getComponent(i, BloodLinkMaster.getComponentType());
            if (master == null) return;
            if (!master.isCacheValid(commandBuffer)) {
                boolean isPlayerInCreative = BloodMagicPlugin.isPlayerInCreative(archetypeChunk.getReferenceTo(i), commandBuffer);
                master.revalidateCache(commandBuffer, !isPlayerInCreative);
            }
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return BloodLinkMaster.getComponentType();
        }
    }

}


package dev.byt3.bloodmagic.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.codec.BloodLinkSource;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import dev.byt3.bloodmagic.components.BloodLink;
import dev.byt3.bloodmagic.components.BloodLinkMaster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BloodLinkDamageSystem extends DamageEventSystem {

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        if (damage.getSource() instanceof BloodLinkSource) {
            // This is already-split damage being applied to a linked entity.
            // Do NOT intercept — leave the damage amount untouched so the engine applies it normally.
            return;
        }

        BloodLinkMaster master = archetypeChunk.getComponent(i, BloodLinkMaster.getComponentType());
        boolean playerInCreative;
        if (master != null) {
            // The damaged entity IS the master — split damage across the network
            playerInCreative = BloodMagicPlugin.isPlayerInCreative(archetypeChunk.getReferenceTo(i), commandBuffer);
            master.applyDamage(damage, commandBuffer, !playerInCreative);
            damage.setCancelled(true);
            return;
        }

        BloodLink link = archetypeChunk.getComponent(i, BloodLink.getComponentType());
        if (link == null) {
            // Entity matched the query but has neither component (shouldn't happen)
            return;
        }

        // The damaged entity is a linked entity — find the master and split damage
        Ref<EntityStore> masterRef = commandBuffer.getExternalData().getRefFromUUID(link.getMasterUUID());
        if (masterRef == null) {
            HytaleLogger.getLogger().atWarning().log("BloodLinkDamageSystem: Master entity reference not found for linked entity, cannot apply linked damage.");
            return;
        }
        master = store.getComponent(masterRef, BloodLinkMaster.getComponentType());
        if (master == null) {
            HytaleLogger.getLogger().atWarning().log("BloodLinkDamageSystem: Master entity has no BloodLinkMaster component.");
            return;
        }
        playerInCreative = BloodMagicPlugin.isPlayerInCreative(masterRef, commandBuffer);
        master.applyDamage(damage, commandBuffer, !playerInCreative);
        damage.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(BloodLink.getComponentType(), BloodLinkMaster.getComponentType());
    }
}

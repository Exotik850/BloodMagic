package dev.byt3.bloodmagic.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import dev.byt3.bloodmagic.codec.BloodMagicConfig;
import dev.byt3.bloodmagic.components.BloodLink;
import dev.byt3.bloodmagic.components.BloodLinkMaster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BloodLinkDeathSystem extends DeathSystems.OnDeathSystem {

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        BloodMagicConfig config = BloodMagicPlugin.get().config.get();
        BloodLinkMaster master = commandBuffer.getComponent(ref, BloodLinkMaster.getComponentType());
        if (master != null) {
            if (config.killLinkedEntities) {
                // The entity that died is a master — kill all linked entities as well
                master.killLinkedEntities(commandBuffer);
                return;
            }
        }

        if (!config.killMasterOnLinkedEntityDeath) {
            // If we're not configured to kill the master when a linked entity dies, we can stop here.
            return;
        }

        BloodLink link = commandBuffer.getComponent(ref, BloodLink.getComponentType());
        if (link == null) return;
        // The entity that died is a linked entity — kill the master as well
        Ref<EntityStore> parentRef = commandBuffer.getExternalData().getRefFromUUID(link.getMasterUUID());
        if (parentRef == null) return;
        Damage lethalDamage = BloodLinkMaster.getDamageForLethalKill(ref);
        DeathComponent.tryAddComponent(commandBuffer, parentRef, lethalDamage);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(BloodLink.getComponentType(), BloodLinkMaster.getComponentType());
    }
}

package dev.byt3.bloodmagic.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.components.BloodLink;
import dev.byt3.bloodmagic.components.BloodLinkMaster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BloodLinkDeathSystem extends DeathSystems.OnDeathSystem {
    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        //
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(BloodLink.getComponentType(), BloodLinkMaster.getComponentType());
    }
}

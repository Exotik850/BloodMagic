package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class BloodLinkMaster implements Component<EntityStore> {
    Ref<EntityStore> masterEntity; // The entity that is the master of this link
    Set<Ref<EntityStore>> linkedEntities; // Set of entities linked to this master
    @Nullable
    Store<EntityStore> store;

    public BloodLinkMaster(Ref<EntityStore> masterEntity, Set<Ref<EntityStore>> linkedEntities) {
        this.linkedEntities = linkedEntities;
        this.masterEntity = masterEntity;
    }

    public void addLinkedEntity(Ref<EntityStore> entity) {
        getStore().putComponent(entity, BloodLink.getComponentType(), new BloodLink(masterEntity));
        linkedEntities.add(entity);
    }

    @Nonnull
    public Store<EntityStore> getStore() {
        if (store == null && linkedEntities.isEmpty()) {
            throw new IllegalStateException("BloodLinkMaster has no linked entities to derive store from.");
        }
        if (store == null) {
            // Assuming all linked entities share the same store, we can take the first one
            Ref<EntityStore> firstLinkedEntity = linkedEntities.iterator().next();
            store = firstLinkedEntity.getStore();
        }
        return store;
    }

    public static ComponentType<EntityStore, BloodLinkMaster> getComponentType() {
        return BloodMagicPlugin.get().bloodLinkMasterComponentType;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new BloodLinkMaster(this.masterEntity, this.linkedEntities);
    }
}

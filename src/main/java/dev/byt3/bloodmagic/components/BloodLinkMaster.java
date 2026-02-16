package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BloodLinkMaster implements Component<EntityStore> {
    public static final BuilderCodec<BloodLinkMaster> CODEC = BuilderCodec.builder(BloodLinkMaster.class, BloodLinkMaster::new)
            .append(new KeyedCodec<>("Owner", Codec.UUID_BINARY), (master, owner) -> master.masterEntity = owner, (master) -> master.masterEntity)
            .add()
            .append(new KeyedCodec<>("LinkedEntities", new ArrayCodec<>(Codec.UUID_BINARY, UUID[]::new)),
                    (master, linkedEntities) -> {
                        master.linkedEntities.clear();
                        master.linkedEntities.addAll(List.of(linkedEntities));
                    },
                    master -> master.linkedEntities.toArray(UUID[]::new))
            .add()
            .build();

    private UUID masterEntity; // The entity that is the master of this link
    private Set<UUID> linkedEntities; // Set of entities linked to this master

    private BloodLinkMaster() {}

    public BloodLinkMaster(UUID masterEntity, Set<UUID> linkedEntities) {
        this.linkedEntities = linkedEntities;
        this.masterEntity = masterEntity;
    }

    public void addLinkedEntity(Ref<EntityStore> entity, ComponentAccessor<EntityStore> store) {
        UUIDComponent uuidComponent = entity.getStore().getComponent(entity, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            throw new IllegalStateException("Entity does not have a UUIDComponent, cannot link to BloodLinkMaster.");
        }
        store.putComponent(entity, BloodLink.getComponentType(), new BloodLink(masterEntity));
        linkedEntities.add(uuidComponent.getUuid());
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

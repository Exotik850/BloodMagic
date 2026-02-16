package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
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
    private Set<UUID> linkedEntities = new HashSet<>(); // Set of entities linked to this master

    @Nullable
    private Ref<EntityStore> entityRef;

    private BloodLinkMaster() {
    }

    private Ref<EntityStore> getRef(ComponentAccessor<EntityStore> store) {
        if (entityRef == null) {
            entityRef = store.getExternalData().getRefFromUUID(masterEntity);
        }
        return entityRef;
    }

    public BloodLinkMaster(UUID masterEntity, Set<UUID> linkedEntities) {
        this.linkedEntities = linkedEntities;
        this.masterEntity = masterEntity;
    }

    public void addLinkedEntity(Ref<EntityStore> entity, ComponentAccessor<EntityStore> store) {
        UUIDComponent uuidComponent = store.getComponent(entity, UUIDComponent.getComponentType());
        EntityStatMap statMap = store.getComponent(entity, EntityStatMap.getComponentType());
        EntityStatMap masterMap = store.getComponent(getRef(store), EntityStatMap.getComponentType());
        if (uuidComponent == null || statMap == null || masterMap == null) {
            HytaleLogger.getLogger().atWarning().log("Tried to link entity with No UUID or StatMap: " + entity.getIndex());
            return;
        }

        EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthValue == null) {
            HytaleLogger.getLogger().atWarning().log("Tried to link entity with No Health stat: " + entity.getIndex());
            return;
        }

        int bloodManaIdx = EntityStatType.getAssetMap().getIndex("BloodMana");
        Modifier linkedModifer = new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, healthValue.get());
        masterMap.putModifier(bloodManaIdx, uuidComponent.getUuid().toString(), linkedModifer);

        store.putComponent(entity, BloodLink.getComponentType(), new BloodLink(masterEntity));
        linkedEntities.add(uuidComponent.getUuid());

        HytaleLogger.getLogger().atInfo().log("Linked entity " + entity.getIndex() + " to master " + getRef(store).getIndex());
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

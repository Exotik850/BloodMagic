package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.codec.BloodLinkSource;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import it.unimi.dsi.fastutil.Pair;

import javax.annotation.Nullable;
import java.util.*;

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
    private List<Pair<EntityStatMap, Ref<EntityStore>>> cachedLinkedRefs = new ArrayList<>(); // Cached refs of linked entities for quick access

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
        masterMap.addStatValue(bloodManaIdx, healthValue.get());

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

    public void removeEntity(Ref<EntityStore> target, ComponentAccessor<EntityStore> store) {
        UUIDComponent uuidComponent = store.getComponent(target, UUIDComponent.getComponentType());
        EntityStatMap statMap = store.getComponent(target, EntityStatMap.getComponentType());
        EntityStatMap masterMap = store.getComponent(getRef(store), EntityStatMap.getComponentType());
        if (uuidComponent == null || statMap == null || masterMap == null) {
            HytaleLogger.getLogger().atWarning().log("Tried to unlink entity with No UUID or StatMap: " + target.getIndex());
            return;
        }

        if (!linkedEntities.contains(uuidComponent.getUuid())) {
            HytaleLogger.getLogger().atWarning().log("Tried to unlink entity that is not linked: " + target.getIndex());
            return;
        }

        int bloodManaIdx = EntityStatType.getAssetMap().getIndex("BloodMana");
        masterMap.removeModifier(bloodManaIdx, uuidComponent.getUuid().toString());

        store.removeComponent(target, BloodLink.getComponentType());
        linkedEntities.remove(uuidComponent.getUuid());

        HytaleLogger.getLogger().atInfo().log("Unlinked entity " + target.getIndex() + " from master " + getRef(store).getIndex());
    }

    /**
     * Applies damage split across the blood link network, including the master entity.
     *
     * @param damage The damage to split
     * @param store  The component accessor
     */
    public void applyDamage(Damage damage, ComponentAccessor<EntityStore> store) {
        applyDamage(damage, store, true);
    }

    /**
     * Applies damage split across the blood link network.
     *
     * @param damage        The damage to split
     * @param store         The component accessor
     * @param includeMaster Whether to include the master entity in the damage calculation
     */
    public void applyDamage(Damage damage, ComponentAccessor<EntityStore> store, boolean includeMaster) {
        // Split the damage based on the percentage of health each linked entity has compared to the total health of all linked entities
        float totalHealth = 0;

        EntityStatMap masterStatMap = store.getComponent(getRef(store), EntityStatMap.getComponentType());
        EntityStatValue masterHealthValue = null;

        if (includeMaster) {
            if (masterStatMap == null) {
                HytaleLogger.getLogger().atWarning().log("Master entity has no EntityStatMap, cannot apply linked damage.");
                return;
            }
            masterHealthValue = masterStatMap.get(DefaultEntityStatTypes.getHealth());
            if (masterHealthValue == null) {
                HytaleLogger.getLogger().atWarning().log("Master entity has no Health stat, cannot apply linked damage.");
                return;
            }
            totalHealth += masterHealthValue.get();
        }

        EntityStore entityStore = store.getExternalData();
        Set<UUID> toRemove = new HashSet<>();

        for (UUID linkedEntity : linkedEntities) {
            Ref<EntityStore> entityRef = entityStore.getRefFromUUID(linkedEntity);
            if (entityRef == null) {
                HytaleLogger.getLogger().atWarning().log("Linked entity with UUID " + linkedEntity + " not found, skipping damage application.");
                toRemove.add(linkedEntity);
                continue;
            }
            EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMap == null) continue;
            EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
            if (healthValue == null) continue;
            totalHealth += healthValue.get();
            cachedLinkedRefs.add(Pair.of(statMap, entityRef));
        }

        for (UUID remove : toRemove) {
            linkedEntities.remove(remove);
        }

        if (totalHealth <= 0) {
            HytaleLogger.getLogger().atWarning().log("Total health of blood link network is 0, cannot split damage.");
            return;
        }

        float totalDamage = damage.getAmount();
        BloodLinkSource source = new BloodLinkSource(getRef(store), damage.getSource());

        // Apply proportional damage to each linked entity
        for (var pair : cachedLinkedRefs) {
            EntityStatMap statMap = pair.left();
            Ref<EntityStore> ref = pair.right();
            EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
            if (healthValue == null) continue;
            float healthPercent = healthValue.get() / totalHealth;
            float damageToApply = totalDamage * healthPercent;
            // Directly apply damage via invoke so the engine processes it
            // but the damage system will recognize it and not re-split
            Damage splitDamage = new Damage(source, damage.getDamageCauseIndex(), damageToApply);
            store.invoke(ref, splitDamage);
            HytaleLogger.getLogger().atInfo().log("Applying %f damage to linked entity %d (Health: %f, Percent: %f%%)", damageToApply, ref.getIndex(), healthValue.get(), healthPercent * 100);
        }

        // Apply proportional damage to the master entity if included
        if (includeMaster) {
            float masterHealthPercent = masterHealthValue.get() / totalHealth;
            float masterDamageToApply = totalDamage * masterHealthPercent;
            Damage masterDamage = new Damage(source, damage.getDamageCauseIndex(), masterDamageToApply);
            store.invoke(getRef(store), masterDamage);
            HytaleLogger.getLogger().atInfo().log("Applying %f damage to master entity %d (Health: %f, Percent: %f%%)", masterDamageToApply, getRef(store).getIndex(), masterHealthValue.get(), masterHealthPercent * 100);
        }

        cachedLinkedRefs.clear();
    }
}

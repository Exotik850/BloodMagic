package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import dev.byt3.bloodmagic.codec.BloodLinkSource;
import dev.byt3.bloodmagic.codec.BloodMagicConfig;
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
    private final List<Pair<EntityStatMap, Ref<EntityStore>>> cachedLinkedRefs = new ArrayList<>(); // Cached refs of linked entities for quick access

    @Nullable
    private Ref<EntityStore> entityRef;

    private BloodLinkMaster() {
    }

    public void clearLinks(ComponentAccessor<EntityStore> store) {
        for (UUID linkedEntity : linkedEntities) {
            Ref<EntityStore> entityRef = store.getExternalData().getRefFromUUID(linkedEntity);
            if (entityRef != null) {
                store.removeComponent(entityRef, BloodLink.getComponentType());
            }
        }
        linkedEntities.clear();
        cachedLinkedRefs.clear();
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

    public Set<UUID> getLinkedEntities() {
        return Collections.unmodifiableSet(linkedEntities);
    }

    public void addLinkedEntity(Ref<EntityStore> entity, ComponentAccessor<EntityStore> store) {
        UUIDComponent uuidComponent = store.getComponent(entity, UUIDComponent.getComponentType());
        store.putComponent(entity, BloodLink.getComponentType(), new BloodLink(masterEntity));
        linkedEntities.add(uuidComponent.getUuid());
        HytaleLogger.getLogger().atInfo().log("Linked entity " + entity.getIndex() + " to master " + getRef(store).getIndex());
        if (!cachedLinkedRefs.isEmpty()) {
            cachedLinkedRefs.add(Pair.of(store.getComponent(entity, EntityStatMap.getComponentType()), entity));
        }
    }

    public boolean isCacheValid(ComponentAccessor<EntityStore> store) {
        return cachedLinkedRefs.size() == linkedEntities.size() && cachedLinkedRefs.stream().allMatch(pair -> {
            UUIDComponent uuidComponent = store.getComponent(pair.right(), UUIDComponent.getComponentType());
            return uuidComponent != null && linkedEntities.contains(uuidComponent.getUuid());
        });
    }

    public float getTotalLinkedHealth() {
        float totalHealth = 0;
        for (var pair : cachedLinkedRefs) {
            EntityStatValue healthValue = pair.left().get(DefaultEntityStatTypes.getHealth());
            if (healthValue != null) {
                totalHealth += healthValue.get();
            }
        }
        return totalHealth;
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

        if (!linkedEntities.contains(uuidComponent.getUuid())) {
            HytaleLogger.getLogger().atWarning().log("Tried to unlink entity that is not linked: " + target.getIndex());
            return;
        }

        store.removeComponent(target, BloodLink.getComponentType());
        linkedEntities.remove(uuidComponent.getUuid());
        if (!cachedLinkedRefs.isEmpty()) {
            cachedLinkedRefs.removeIf(pair -> pair.right().equals(target));
        }

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
        BloodMagicConfig config = BloodMagicPlugin.get().config.get();

        // Check if damage type is blacklisted
        if (config.blacklistedDamageTypes.contains(damage.getDamageCauseIndex())) {
            return;
        }

        // Check split threshold
        // TODO: This threshold check should be against total health, as per config documentation
        if (damage.getAmount() < config.splitThreshold) {
            return;
        }

        // Apply damage reduction multiplier
        float modifiedDamage = (float) (damage.getAmount() * config.damageReductionMultiplier);

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

        for (var pair : cachedLinkedRefs) {
            Ref<EntityStore> ref = pair.right();

            // TODO Check distance

            EntityStatValue healthValue = pair.left().get(DefaultEntityStatTypes.getHealth());
            if (healthValue == null) continue;
            totalHealth += healthValue.get();
        }

        if (totalHealth <= 0) {
            HytaleLogger.getLogger().atWarning().log("Total health of blood link network is 0, cannot split damage.");
            return;
        }

        float totalDamage = modifiedDamage;
        BloodLinkSource source = new BloodLinkSource(getRef(store), damage.getSource());

        // TODO: Implement other SplitModes (PERCENTAGE_COUNT, SEQUENTIAL, FLAT) based on config.damageSplitMode
        // Currently only implementing PERCENTAGE_TOTAL

        // Apply proportional damage to each linked entity
        for (var pair : cachedLinkedRefs) {
            EntityStatMap statMap = pair.left();
            Ref<EntityStore> ref = pair.right();

            // TODO Check distance again (or optimize to not calculate twice?

            EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
            if (healthValue == null) continue;

            float damageToApply;
            if (config.damageSplitMode == BloodMagicConfig.DamageSplitMode.PERCENTAGE_TOTAL) {
                float healthPercent = healthValue.get() / totalHealth;
                damageToApply = totalDamage * healthPercent;
            } else {
                // Fallback or TODO for other modes
                // For now defaulting to PERCENTAGE_TOTAL logic as placeholder for other modes to ensure compilation/running
                float healthPercent = healthValue.get() / totalHealth;
                damageToApply = totalDamage * healthPercent;
            }

            if (config.preventLethalDamageToLinked) {
                float currentHealth = healthValue.get();
                if (damageToApply >= currentHealth) {
                    damageToApply = Math.max(0, currentHealth - 1); // Reduce damage to leave at least 1 health
                }
            }

            // Directly apply damage via invoke so the engine processes it
            // but the damage system will recognize it and not re-split
            Damage splitDamage = new Damage(source, damage.getDamageCauseIndex(), damageToApply);
            store.invoke(ref, splitDamage);

            if (config.showDamageNumbers) {
                // TODO: Show floating damage numbers
            }

            if (config.debugMode) {
                HytaleLogger.getLogger().atInfo().log("Applying %f damage to linked entity %d (Health: %f)", damageToApply, ref.getIndex(), healthValue.get());
            }
        }

        // Apply proportional damage to the master entity if included
        if (includeMaster) {
            float masterDamageToApply;
            if (config.damageSplitMode == BloodMagicConfig.DamageSplitMode.PERCENTAGE_TOTAL) {
                float masterHealthPercent = masterHealthValue.get() / totalHealth;
                masterDamageToApply = totalDamage * masterHealthPercent;
            } else {
                // Fallback
                float masterHealthPercent = masterHealthValue.get() / totalHealth;
                masterDamageToApply = totalDamage * masterHealthPercent;
            }

            masterDamageToApply *= (float) config.masterDamageMultiplier;

            Damage masterDamage = new Damage(source, damage.getDamageCauseIndex(), masterDamageToApply);
            store.invoke(getRef(store), masterDamage);

            if (config.showDamageNumbers) {
                // TODO: Show floating damage numbers
            }

            if (config.debugMode) {
                HytaleLogger.getLogger().atInfo().log("Applying %f damage to master entity %d (Health: %f)", masterDamageToApply, getRef(store).getIndex(), masterHealthValue.get());
            }
        }
    }

    public void revalidateCache(ComponentAccessor<EntityStore> store, boolean includeMaster) {
        cachedLinkedRefs.clear();
        for (UUID linkedEntity : linkedEntities) {
            Ref<EntityStore> entityRef = store.getExternalData().getRefFromUUID(linkedEntity);
            if (entityRef == null) {
                HytaleLogger.getLogger().atWarning().log("Linked entity with UUID " + linkedEntity + " not found, skipping damage application.");
                continue;
            }
            EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMap == null) continue;
            cachedLinkedRefs.add(Pair.of(statMap, entityRef));
        }

        if (includeMaster) {
            EntityStatMap masterStatMap = store.getComponent(getRef(store), EntityStatMap.getComponentType());
            if (masterStatMap != null) {
                cachedLinkedRefs.add(Pair.of(masterStatMap, getRef(store)));
            }
        }
    }

    public static Damage getDamageForLethalKill(Ref<EntityStore> ref) {
        return new Damage(new BloodLinkSource(ref, null), DamageCause.getAssetMap().getIndex("BloodLinkSever"), Float.MAX_VALUE);
    }

    public void killLinkedEntities(CommandBuffer<EntityStore> store) {
        Damage lethalDamage = getDamageForLethalKill(getRef(store));
        if (isCacheValid(store)) {
            for (var pair : cachedLinkedRefs) {
                DeathComponent.tryAddComponent(store, pair.right(), lethalDamage);
            }
            return;
        }

        for (UUID linkedEntity : linkedEntities) {
            Ref<EntityStore> entityRef = store.getExternalData().getRefFromUUID(linkedEntity);
            if (entityRef == null) {
                HytaleLogger.getLogger().atWarning().log("Linked entity with UUID " + linkedEntity + " not found, cannot apply lethal damage.");
                continue;
            }
            DeathComponent.tryAddComponent(store, entityRef, lethalDamage);
        }

    }
}

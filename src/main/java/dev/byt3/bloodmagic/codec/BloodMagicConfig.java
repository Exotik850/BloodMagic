package dev.byt3.bloodmagic.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.validator.ArrayValidator;
import com.hypixel.hytale.server.npc.validators.NPCRoleValidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BloodMagicConfig {

    public static BuilderCodec<BloodMagicConfig> CODEC = BuilderCodec.builder(BloodMagicConfig.class, BloodMagicConfig::new)
            .append(new KeyedCodec<>("SplitMode", new EnumCodec<>(DamageSplitMode.class)),
                    (config, mode) -> config.damageSplitMode = mode,
                    config -> config.damageSplitMode)
            .documentation("Determines how damage is split across linked entities when a linked entity takes damage.")
            .add()
            .append(new KeyedCodec<>("OnlySplitWhenMasterDamaged", Codec.BOOLEAN),
                    (config, value) -> config.onlySplitWhenMasterDamaged = value,
                    config -> config.onlySplitWhenMasterDamaged)
            .documentation("If true, damage will only be split across the network if the master entity is the one taking damage. If false, damage to any linked entity will be split across the network.")
            .add()
            .append(new KeyedCodec<>("BlacklistedEntityTypes", Codec.STRING_ARRAY),
                    (config, list) -> {
                        config.blacklistedEntityTypes.clear();
                        config.blacklistedEntityTypes.addAll(List.of(list));
                    },
                    config -> config.blacklistedEntityTypes.toArray(String[]::new)
            )
            .documentation("List of entity groups (by name) that should be excluded from being able to link.")
            .addValidatorLate(() -> new ArrayValidator<>(NPCRoleValidator.INSTANCE).late())
            .add()
            .append(new KeyedCodec<>("KillLinkedEntities", Codec.BOOLEAN),
                    (config, value) -> config.killLinkedEntities = value,
                    config -> config.killLinkedEntities)
            .documentation("If true, all linked entities will be killed when the master entity dies.")
            .add()
            .append(new KeyedCodec<>("UnlinkOnMasterDying", Codec.BOOLEAN),
                    (config, value) -> config.unlinkOnMasterDying = value,
                    config -> config.unlinkOnMasterDying)
            .documentation("If true, all linked entities will be unlinked from the master when it dies")
            .add()
            .append(new KeyedCodec<>("CanLinkPlayers", Codec.BOOLEAN),
                    (config, value) -> config.canLinkPlayers = value,
                    config -> config.canLinkPlayers)
            .documentation("If true, Players are also able to be linked. (Use at your own risk!)")
            .add()
            .append(new KeyedCodec<>("SplitThreshold", Codec.FLOAT),
                    (config, value) -> config.splitThreshold = value,
                    config -> config.splitThreshold)
            .documentation("Minimum damage amount to bother splitting (as a percentage of total linked health)")
            .add()
            .build();

    public DamageSplitMode damageSplitMode = DamageSplitMode.PERCENTAGE_TOTAL;
    public boolean onlySplitWhenMasterDamaged = false;
    public boolean killLinkedEntities = false;
    public boolean unlinkOnMasterDying = true;
    public boolean canLinkPlayers = false;
    public float splitThreshold = 0.01f; //
    public Set<String> blacklistedEntityTypes = new HashSet<>();

    public static enum DamageSplitMode {
        PERCENTAGE_TOTAL, // Scale damage to each entity based on the contribution of their max health to the total max health of the network
        PERCENTAGE_COUNT, // Split damage by count of linked entities
        SEQUENTIAL, // Apply full damage to one entity at a time
        FLAT // Apply full damage to each entity (can result in more total damage than the original amount)
    }
}

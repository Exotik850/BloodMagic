package dev.byt3.bloodmagic.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.validator.ArrayValidator;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.npc.validators.NPCRoleValidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BloodMagicConfig {

    public static BuilderCodec<BloodMagicConfig> CODEC = BuilderCodec.builder(BloodMagicConfig.class, BloodMagicConfig::new)
            // ==================== DAMAGE DISTRIBUTION ====================
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
            .append(new KeyedCodec<>("SplitThreshold", Codec.DOUBLE),
                    (config, value) -> config.splitThreshold = value,
                    config -> config.splitThreshold)
            .documentation("Minimum damage amount to bother splitting (as a percentage of total linked health). Prevents micro-damage spam.")
            .add()
            .append(new KeyedCodec<>("DamageReductionMultiplier", Codec.DOUBLE),
                    (config, value) -> config.damageReductionMultiplier = value,
                    config -> config.damageReductionMultiplier)
            .documentation("Multiplier applied to total damage before splitting (1.0 = no change, 0.8 = 20% reduction, 1.2 = 20% increase). Allows balancing the link as a buff or debuff.")
            .add()
            .append(new KeyedCodec<>("MasterDamageMultiplier", Codec.DOUBLE),
                    (config, value) -> config.masterDamageMultiplier = value,
                    config -> config.masterDamageMultiplier)
            .documentation("Multiplier for damage applied specifically to the master entity during splits. Allows the master to take more/less damage than linked entities.")
            .add()
            .append(new KeyedCodec<>("BlacklistedDamageTypes", Codec.STRING_ARRAY),
                    (config, list) -> {
                        config.blacklistedDamageTypes.clear();
                        config.blacklistedDamageTypes.addAll(List.of(list));
                    },
                    config -> config.blacklistedDamageTypes.toArray(String[]::new))
            .addValidatorLate(() -> DamageCause.VALIDATOR_CACHE.getArrayValidator().late())
            .documentation("List of damage type names that should NOT be split across the network (e.g., 'fall', 'drown', 'void').")
            .add()

            // ==================== LINKING LIMITS ====================
            .append(new KeyedCodec<>("MaxLinkedEntities", Codec.INTEGER),
                    (config, value) -> config.maxLinkedEntities = value,
                    config -> config.maxLinkedEntities)
            .documentation("Maximum number of entities that can be linked to a single master. Set to -1 for unlimited.")
            .add()
            .append(new KeyedCodec<>("MaxLinkDistance", Codec.DOUBLE),
                    (config, value) -> config.maxLinkDistance = value,
                    config -> config.maxLinkDistance)
            .documentation("Maximum distance (in blocks) between master and linked entities. Entities beyond this range may be auto-unlinked. Set to -1 for unlimited range.")
            .add()
            .append(new KeyedCodec<>("RequireLineOfSight", Codec.BOOLEAN),
                    (config, value) -> config.requireLineOfSight = value,
                    config -> config.requireLineOfSight)
            .documentation("If true, damage sharing requires line of sight between master and linked entities.")
            .add()

            // ==================== ENTITY RESTRICTIONS ====================
            .append(new KeyedCodec<>("BlacklistedNPCRoles", Codec.STRING_ARRAY),
                    (config, list) -> {
                        config.blacklistedNPCRoles.clear();
                        config.blacklistedNPCRoles.addAll(List.of(list));
                    },
                    config -> config.blacklistedNPCRoles.toArray(String[]::new)
            )
            .documentation("List of entity groups (by name) that should be excluded from being able to link.")
            .addValidatorLate(() -> new ArrayValidator<>(NPCRoleValidator.INSTANCE).late())
            .add()
            .append(new KeyedCodec<>("WhitelistedNPCRoles", Codec.STRING_ARRAY),
                    (config, list) -> {
                        config.whitelistedNPCRoles.clear();
                        config.whitelistedNPCRoles.addAll(List.of(list));
                    },
                    config -> config.whitelistedNPCRoles.toArray(String[]::new))
            .documentation("If non-empty, ONLY these entity types can be linked. Takes priority over blacklist.")
            .addValidatorLate(() -> new ArrayValidator<>(NPCRoleValidator.INSTANCE).late())
            .add()
            .append(new KeyedCodec<>("CanLinkPlayers", Codec.BOOLEAN),
                    (config, value) -> config.canLinkPlayers = value,
                    config -> config.canLinkPlayers)
            .documentation("If true, Players are also able to be linked. (Use at your own risk!)")
            .add()
            .append(new KeyedCodec<>("CanLinkBosses", Codec.BOOLEAN),
                    (config, value) -> config.canLinkBosses = value,
                    config -> config.canLinkBosses)
            .documentation("If true, boss-type entities can be linked. Disabled by default to prevent exploits.")
            .add()
            .append(new KeyedCodec<>("AllowCrossTeamLinking", Codec.BOOLEAN),
                    (config, value) -> config.allowCrossTeamLinking = value,
                    config -> config.allowCrossTeamLinking)
            .documentation("If true, entities from different teams/factions can be linked together.")
            .add()

            // ==================== DEATH & LIFECYCLE ====================
            .append(new KeyedCodec<>("KillLinkedEntities", Codec.BOOLEAN),
                    (config, value) -> config.killLinkedEntities = value,
                    config -> config.killLinkedEntities)
            .documentation("If true, all linked entities will be killed when the master entity dies.")
            .add()
            .append(new KeyedCodec<>("UnlinkOnMasterDying", Codec.BOOLEAN),
                    (config, value) -> config.unlinkOnMasterDying = value,
                    config -> config.unlinkOnMasterDying)
            .documentation("If true, all linked entities will be unlinked from the master when it dies.")
            .add()
            .append(new KeyedCodec<>("TransferMasterOnDeath", Codec.BOOLEAN),
                    (config, value) -> config.transferMasterOnDeath = value,
                    config -> config.transferMasterOnDeath)
            .documentation("If true, when the master dies, the link network transfers to the linked entity with the highest health instead of dissolving.")
            .add()
            .append(new KeyedCodec<>("PreventLethalDamageToLinked", Codec.BOOLEAN),
                    (config, value) -> config.preventLethalDamageToLinked = value,
                    config -> config.preventLethalDamageToLinked)
            .documentation("If true, linked entities cannot die from split damage - they'll be left at 1 HP instead. Only the master can die from split damage.")
            .add()
            .append(new KeyedCodec<>("KillMasterOnChildDeath", Codec.BOOLEAN),
                    (config, value) -> config.killMasterOnLinkedEntityDeath = value,
                    config -> config.killMasterOnLinkedEntityDeath)
            .documentation("If true, the master entity will be killed when any linked entity dies.")
            .add()
            .append(new KeyedCodec<>("LinkDurationSeconds", Codec.DOUBLE),
                    (config, value) -> config.linkDurationSeconds = value,
                    config -> config.linkDurationSeconds)
            .documentation("How long links last in seconds. Set to -1 for permanent links (until manually broken or entity death).")
            .add()

            // ==================== HEALING & REGENERATION ====================
            .append(new KeyedCodec<>("ShareHealing", Codec.BOOLEAN),
                    (config, value) -> config.shareHealing = value,
                    config -> config.shareHealing)
            .documentation("If true, healing received by any linked entity is also distributed across the network (using the same split mode as damage).")
            .add()
            .append(new KeyedCodec<>("HealingSplitEfficiency", Codec.DOUBLE),
                    (config, value) -> config.healingSplitEfficiency = value,
                    config -> config.healingSplitEfficiency)
            .documentation("Efficiency multiplier for shared healing (1.0 = full healing shared, 0.5 = 50% healing shared).")
            .add()

            // ==================== VISUAL FEEDBACK ====================
            .append(new KeyedCodec<>("ShowLinkParticles", Codec.BOOLEAN),
                    (config, value) -> config.showLinkParticles = value,
                    config -> config.showLinkParticles)
            .documentation("If true, visual particle effects are shown between linked entities.")
            .add()
            .append(new KeyedCodec<>("ShowDamageNumbers", Codec.BOOLEAN),
                    (config, value) -> config.showDamageNumbers = value,
                    config -> config.showDamageNumbers)
            .documentation("If true, floating damage numbers are shown on all entities receiving split damage.")
            .add()
            .append(new KeyedCodec<>("LinkParticleColor", Codec.STRING),
                    (config, value) -> config.linkParticleColor = value,
                    config -> config.linkParticleColor)
            .documentation("Hex color code for link particles (e.g., '#FF0000' for red). Default is blood red.")
            .add()
            .append(new KeyedCodec<>("PlayLinkSound", Codec.BOOLEAN),
                    (config, value) -> config.playLinkSound = value,
                    config -> config.playLinkSound)
            .documentation("If true, plays a sound effect when entities are linked/unlinked.")
            .add()
            .append(new KeyedCodec<>("LinkSoundEffect", Codec.STRING),
                    (config, value) -> config.linkSoundEffect = value,
                    config -> config.linkSoundEffect)
            .documentation("Sound effect asset name to play when linking. Empty string for default.")
            .add()

            // ==================== DEBUG & LOGGING ====================
            .append(new KeyedCodec<>("DebugMode", Codec.BOOLEAN),
                    (config, value) -> config.debugMode = value,
                    config -> config.debugMode)
            .documentation("If true, enables verbose logging for debugging blood link calculations and events.")
            .add()
            .build();

    // ==================== DAMAGE DISTRIBUTION ====================
    public DamageSplitMode damageSplitMode = DamageSplitMode.PERCENTAGE_TOTAL;
    public boolean onlySplitWhenMasterDamaged = false;
    public double splitThreshold = 0.01;
    public double damageReductionMultiplier = 1.0;
    public double masterDamageMultiplier = 1.0;
    public Set<String> blacklistedDamageTypes = new HashSet<>();

    // ==================== LINKING LIMITS ====================
    public Integer maxLinkedEntities = -1;
    public Double maxLinkDistance = null;
    public boolean requireLineOfSight = false;

    // ==================== ENTITY RESTRICTIONS ====================
    public Set<String> blacklistedNPCRoles = new HashSet<>();
    public Set<String> whitelistedNPCRoles = new HashSet<>();
    public boolean canLinkPlayers = false;
    public boolean canLinkBosses = false;
    public boolean allowCrossTeamLinking = true;

    // ==================== DEATH & LIFECYCLE ====================
    public boolean killLinkedEntities = false;
    public boolean killMasterOnLinkedEntityDeath = false;
    public boolean unlinkOnMasterDying = true;
    public boolean transferMasterOnDeath = false;
    public boolean preventLethalDamageToLinked = false;
    public Double linkDurationSeconds = -1.0;

    // ==================== HEALING & REGENERATION ====================
    public boolean shareHealing = false;
    public double healingSplitEfficiency = 1.0;

    // ==================== VISUAL FEEDBACK ====================
    public boolean showLinkParticles = true;
    public boolean showDamageNumbers = true;
    public String linkParticleColor = "#8B0000";
    public boolean playLinkSound = true;
    public String linkSoundEffect = "";

    // ==================== DEBUG & LOGGING ====================
    public boolean debugMode = false;

    public static enum DamageSplitMode {
        PERCENTAGE_TOTAL, // Scale damage to each entity based on the contribution of their max health to the total max health of the network
        PERCENTAGE_COUNT, // Split damage by count of linked entities
        SEQUENTIAL, // Apply full damage to one entity at a time
        FLAT // Apply full damage to each entity (can result in more total damage than the original amount)
    }
}

package dev.byt3.bloodmagic;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import dev.byt3.bloodmagic.codec.BloodMagicConfig;
import dev.byt3.bloodmagic.components.BloodLink;
import dev.byt3.bloodmagic.components.BloodLinkMaster;
import dev.byt3.bloodmagic.interactions.ActivateBloodLinkInteraction;
import dev.byt3.bloodmagic.systems.BloodLinkDamageSystem;
import dev.byt3.bloodmagic.systems.BloodLinkDeathSystem;
import dev.byt3.bloodmagic.systems.BloodManaSystems;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BloodMagicPlugin extends JavaPlugin {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public ComponentType<EntityStore, BloodLink> bloodLinkComponentType;
    public ComponentType<EntityStore, BloodLinkMaster> bloodLinkMasterComponentType;

    static BloodMagicPlugin instance;
    public Config<BloodMagicConfig> config = this.withConfig("BloodMagicConfig", BloodMagicConfig.CODEC);

    public BloodMagicPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static BloodMagicPlugin get() {
        return instance;
    }

    public static boolean isPlayerInCreative(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        if (store.getExternalData().getWorld().getGameplayConfig().getCombatConfig().isPlayerIncomingDamageDisabled()) {
            // If player damage is disabled, then the player is effectively in creative mode for the purposes of Blood Magic.
            return true;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        return player != null && player.getGameMode().equals(GameMode.Creative);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Starting Blood Magic plugin setup...");
        BloodMagicConfig configInstance = config.get();
        bloodLinkComponentType = this.getEntityStoreRegistry().registerComponent(BloodLink.class, "BloodLink", BloodLink.CODEC);
        bloodLinkMasterComponentType = this.getEntityStoreRegistry().registerComponent(BloodLinkMaster.class, "BloodLinkMaster", BloodLinkMaster.CODEC);
        this.getEntityStoreRegistry().registerSystem(new BloodLinkDamageSystem());
        if (configInstance.killLinkedEntities || configInstance.killMasterOnLinkedEntityDeath) {
            LOGGER.atInfo().log("Registering BloodLinkDeathSystem due to config settings...");
            this.getEntityStoreRegistry().registerSystem(new BloodLinkDeathSystem());
        }
        BloodManaSystems.registerSystems(this);
        this.getCodecRegistry(Interaction.CODEC).register("ActivateBloodLink", ActivateBloodLinkInteraction.class, ActivateBloodLinkInteraction.CODEC);
        if (EntityStatType.getAssetMap().getIndex("BloodMana") == Integer.MAX_VALUE) {
            LOGGER.atSevere().log("BloodMana stat not found in asset map! Blood Magic will not function correctly.");
        }
        config.save();
    }

}

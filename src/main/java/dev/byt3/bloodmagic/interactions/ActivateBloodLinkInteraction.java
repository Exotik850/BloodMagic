package dev.byt3.bloodmagic.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;
import dev.byt3.bloodmagic.codec.BloodMagicConfig;
import dev.byt3.bloodmagic.components.BloodLinkMaster;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.HashSet;

public class ActivateBloodLinkInteraction extends SimpleInstantInteraction {
    public static BuilderCodec<ActivateBloodLinkInteraction> CODEC = BuilderCodec.builder(ActivateBloodLinkInteraction.class, ActivateBloodLinkInteraction::new, SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("Unlink", Codec.BOOLEAN), (interaction, unlink) -> interaction.unlink = unlink, interaction -> interaction.unlink)
            .documentation("Whether this interaction should unlink the entities instead of linking them. If true, the target entity will be removed from the master entity's BloodLinkMaster component.")
            .add()
            .build();

    private boolean unlink = false;

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext ctx, @NonNullDecl CooldownHandler cooldownHandler) {
        Ref<EntityStore> master = ctx.getOwningEntity();
        ComponentAccessor<EntityStore> store = ctx.getCommandBuffer();

        BloodMagicConfig config = BloodMagicPlugin.get().config.get();

        if (store == null) {
            HytaleLogger.getLogger().atWarning().log("ActivateBloodLinkInteraction: No component store available for interaction.");
            return;
        }
        Ref<EntityStore> targetEntity = ctx.getTargetEntity();
        if (targetEntity == null) {
            HytaleLogger.getLogger().atWarning().log("ActivateBloodLinkInteraction: No target entity found for interaction.");
            return;
        }

        // Check Entity Type Limits
        // TODO: Get entity type string to check against blacklist/whitelist
        // String entityType = ???;
        // if (config.whitelistedEntityTypes.size() > 0 && !config.whitelistedEntityTypes.contains(entityType)) { return; }
        // if (config.blacklistedEntityTypes.contains(entityType)) { return; }

        if (!config.canLinkPlayers) {
            if (store.getComponent(targetEntity, Player.getComponentType()) != null) {
                HytaleLogger.getLogger().atInfo().log("ActivateBloodLinkInteraction: Target entity is a player and player linking is disabled.");
                return;
            }
        }

        // TODO: Check if target is a boss and config.canLinkBosses is false

        // TODO: Check teams and config.allowCrossTeamLinking

        if (BloodMagicPlugin.isPlayerInCreative(targetEntity, store)) {
            HytaleLogger.getLogger().atInfo().log("ActivateBloodLinkInteraction: Target entity is a player in creative mode, skipping link.");
            return;
        }
        UUIDComponent uuidTarget = store.getComponent(targetEntity, UUIDComponent.getComponentType());
        if (uuidTarget == null) {
            HytaleLogger.getLogger().atWarning().log("ActivateBloodLinkInteraction: Target entity does not have a UUIDComponent, cannot link.");
            return;
        }
        UUIDComponent uuidMaster = store.getComponent(master, UUIDComponent.getComponentType());
        if (uuidMaster == null) {
            HytaleLogger.getLogger().atWarning().log("ActivateBloodLinkInteraction: Master entity does not have a UUIDComponent, cannot link.");
            return;
        }
        BloodLinkMaster masterComponent = store.getComponent(master, BloodLinkMaster.getComponentType());
        if (masterComponent == null) {
            masterComponent = new BloodLinkMaster(uuidMaster.getUuid(), new HashSet<>());
            store.putComponent(master, BloodLinkMaster.getComponentType(), masterComponent);
        }
        if (unlink) {
            masterComponent.removeEntity(targetEntity, store);
        } else {
             // Check max links
            if (config.maxLinkedEntities != -1 && masterComponent.getLinkedEntities().size() >= config.maxLinkedEntities) {
                 HytaleLogger.getLogger().atInfo().log("ActivateBloodLinkInteraction: Master has reached max linked entities.");
                 return;
            }
            masterComponent.addLinkedEntity(targetEntity, store);
        }
    }
}

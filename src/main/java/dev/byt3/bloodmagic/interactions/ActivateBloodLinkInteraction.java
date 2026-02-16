package dev.byt3.bloodmagic.interactions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.components.BloodLinkMaster;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;

public class ActivateBloodLinkInteraction extends SimpleInstantInteraction {
    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext ctx, @NonNullDecl CooldownHandler cooldownHandler) {
        Ref<EntityStore> master = ctx.getOwningEntity();
        Store<EntityStore> store = master.getStore();
        Ref<EntityStore> targetEntity = ctx.getTargetEntity();
        if (targetEntity == null) {
            HytaleLogger.getLogger().atWarning().log("ActivateBloodLinkInteraction: No target entity found for interaction.");
            return;
        }
        BloodLinkMaster masterComponent = store.getComponent(master, BloodLinkMaster.getComponentType());
        if (masterComponent == null) {
            masterComponent = new BloodLinkMaster(master, Set.of());
            store.putComponent(master, BloodLinkMaster.getComponentType(), masterComponent);
        }
        masterComponent.addLinkedEntity(targetEntity);
    }
}

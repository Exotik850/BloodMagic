package dev.byt3.bloodmagic.codec;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class BloodLinkSource extends Damage.EntitySource {
    private final Damage.Source originalSource;

    public BloodLinkSource(@Nonnull Ref<EntityStore> sourceRef, Damage.Source originalSource) {
        super(sourceRef);
        this.originalSource = originalSource;
    }

    @Nonnull
    @Override
    public Message getDeathMessage(@Nonnull Damage info, @Nonnull Ref<EntityStore> targetRef, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        return super.getDeathMessage(info, targetRef, componentAccessor).insert(Message.raw(" (linked damage)"));
    }

    public Damage.Source getOriginalSource() {
        return originalSource;
    }
}

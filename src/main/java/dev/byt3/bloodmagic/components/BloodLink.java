package dev.byt3.bloodmagic.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.BloodMagicPlugin;

import javax.annotation.Nullable;

public class BloodLink implements Component<EntityStore> {

    public static BuilderCodec<BloodLink> CODEC = BuilderCodec.builder(BloodLink.class, BloodLink::new)
            .build();

    Ref<EntityStore> linkedEntity;

    private BloodLink() {}

    public BloodLink(Ref<EntityStore> linkedEntity) {
        this.linkedEntity = linkedEntity;
    }

    public static ComponentType<EntityStore, BloodLink> getComponentType() {
        return BloodMagicPlugin.get().bloodLinkComponentType;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new BloodLink(this.linkedEntity);
    }
}

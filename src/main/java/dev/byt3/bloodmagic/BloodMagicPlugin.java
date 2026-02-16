package dev.byt3.bloodmagic;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.byt3.bloodmagic.components.BloodLink;
import dev.byt3.bloodmagic.components.BloodLinkMaster;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BloodMagicPlugin extends JavaPlugin {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public ComponentType<EntityStore, BloodLink> bloodLinkComponentType;
    public ComponentType<EntityStore, BloodLinkMaster> bloodLinkMasterComponentType;

    static BloodMagicPlugin instance;

    public BloodMagicPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static BloodMagicPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Starting Blood Magic plugin setup...");
        bloodLinkComponentType = this.getEntityStoreRegistry().registerComponent(BloodLink.class, "BloodLink", BloodLink.CODEC);
    }

}

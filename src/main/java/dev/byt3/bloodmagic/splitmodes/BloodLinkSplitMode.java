package dev.byt3.bloodmagic.splitmodes;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

public interface BloodLinkSplitMode {

    class SplitContext {
        float damageDealt;
        Damage originalDamage;
        float totalLinkedHealth;
        int entityIdx;

        public SplitContext(Damage originalDamage, float totalLinkedHealth) {
            this.damageDealt = 0.0f;
            this.originalDamage = originalDamage;
            this.totalLinkedHealth = totalLinkedHealth;
            this.entityIdx = 0;
        }

        public void addDamageForEntity(float damage) {
            this.damageDealt += damage;
            this.entityIdx += 1;
        }
    }
}

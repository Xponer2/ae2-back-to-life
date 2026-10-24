package appeng.core.definitions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

/**
 * AE2's damage types (death message key {@code death.attack.<id>}, like on modern Minecraft).
 */
public final class AEDamageTypes {
    private AEDamageTypes() {
    }

    public static final String MATTER_CANNON = "matter_cannon";

    public static DamageSource matterCannon(@Nullable Entity shooter) {
        if (shooter == null) {
            return new DamageSource(MATTER_CANNON).setProjectile();
        }
        return new EntityDamageSource(MATTER_CANNON, shooter).setProjectile();
    }
}

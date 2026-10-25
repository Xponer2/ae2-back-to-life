package appeng.integration.modules.igtooltip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import appeng.api.integrations.igtooltip.BaseClassRegistration;
import appeng.api.parts.IPartHost;

public class BaseClassRegistrationImpl implements BaseClassRegistration {
    private static final Logger LOG = LogManager.getLogger(BaseClassRegistrationImpl.class);

    private final List<BaseClass> baseClasses = new ArrayList<>();

    private final Set<BaseClass> partHostClasses = new HashSet<>();

    @Override
    public void addBaseBlockEntity(Class<? extends TileEntity> blockEntityClass, Class<? extends Block> blockClass) {
        var defaultClass = new BaseClass(blockEntityClass, blockClass);

        // If any superclass is already in the list, don't add it
        for (var registeredClass : baseClasses) {
            if (registeredClass.isSuperclassOf(defaultClass)) {
                LOG.info("Not registering {}, because superclass {} is already registered.",
                        defaultClass, registeredClass);
                return;
            }
        }

        // Remove any subclasses of this class
        baseClasses.removeIf(otherClass -> {
            if (defaultClass.isSuperclassOf(otherClass)) {
                LOG.info("Replacing default server-data registration for {} with superclass {}.",
                        defaultClass, otherClass);
                return true;
            }
            return false;
        });

        baseClasses.add(defaultClass);
    }

    @Override
    public <T extends TileEntity & IPartHost> void addPartHost(Class<T> blockEntityClass,
            Class<? extends Block> blockClass) {
        partHostClasses.add(new BaseClass(blockEntityClass, blockClass));
    }

    public List<BaseClass> getBaseClasses() {
        return baseClasses;
    }

    public Set<BaseClass> getPartHostClasses() {
        return partHostClasses;
    }

    public static final class BaseClass {
        private final Class<? extends TileEntity> blockEntity;
        private final Class<? extends Block> block;

        public BaseClass(Class<? extends TileEntity> blockEntity, Class<? extends Block> block) {
            this.blockEntity = blockEntity;
            this.block = block;
        }

        public Class<? extends TileEntity> blockEntity() {
            return blockEntity;
        }

        public Class<? extends Block> block() {
            return block;
        }

        public boolean isSuperclassOf(BaseClass other) {
            return blockEntity.isAssignableFrom(other.blockEntity);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BaseClass)) {
                return false;
            }
            BaseClass that = (BaseClass) o;
            return blockEntity.equals(that.blockEntity) && block.equals(that.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockEntity, block);
        }

        @Override
        public String toString() {
            return "BaseClass[blockEntity=" + blockEntity + ", block=" + block + "]";
        }
    }
}

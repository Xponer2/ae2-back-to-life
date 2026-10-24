package appeng.crafting.execution;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.crafting.UnsuitableCpus;
import appeng.api.stacks.GenericStack;

public final class CraftingSubmitResult implements ICraftingSubmitResult {
    private final ICraftingLink link;
    private final CraftingSubmitErrorCode errorCode;
    private final Object errorDetail;

    public CraftingSubmitResult(@Nullable ICraftingLink link, @Nullable CraftingSubmitErrorCode errorCode, @Nullable Object errorDetail) {
        this.link = link;
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
    }

    @Nullable public ICraftingLink link() {
        return link;
    }

    @Nullable public CraftingSubmitErrorCode errorCode() {
        return errorCode;
    }

    @Nullable public Object errorDetail() {
        return errorDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftingSubmitResult)) return false;
        CraftingSubmitResult that = (CraftingSubmitResult) o;
        return java.util.Objects.equals(link, that.link)
                && java.util.Objects.equals(errorCode, that.errorCode)
                && java.util.Objects.equals(errorDetail, that.errorDetail);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(link, errorCode, errorDetail);
    }

    @Override
    public String toString() {
        return "CraftingSubmitResult[" + "link=" + link + ", " + "errorCode=" + errorCode + ", " + "errorDetail=" + errorDetail + "]";
    }

    public static ICraftingSubmitResult successful(@Nullable ICraftingLink link) {
        return new CraftingSubmitResult(link, null, null);
    }

    public static final ICraftingSubmitResult NO_CPU_FOUND = simpleError(CraftingSubmitErrorCode.NO_CPU_FOUND);
    public static final ICraftingSubmitResult INCOMPLETE_PLAN = simpleError(CraftingSubmitErrorCode.INCOMPLETE_PLAN);
    public static final ICraftingSubmitResult CPU_BUSY = simpleError(CraftingSubmitErrorCode.CPU_BUSY);

    public static final ICraftingSubmitResult CPU_OFFLINE = simpleError(CraftingSubmitErrorCode.CPU_OFFLINE);

    public static final ICraftingSubmitResult CPU_TOO_SMALL = simpleError(CraftingSubmitErrorCode.CPU_TOO_SMALL);

    public static ICraftingSubmitResult simpleError(CraftingSubmitErrorCode code) {
        return new CraftingSubmitResult(null, code, null);
    }

    public static ICraftingSubmitResult missingIngredient(GenericStack missingIngredient) {
        return new CraftingSubmitResult(null, CraftingSubmitErrorCode.MISSING_INGREDIENT, missingIngredient);
    }

    public static ICraftingSubmitResult noSuitableCpu(UnsuitableCpus unsuitableCpus) {
        return new CraftingSubmitResult(null, CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND, unsuitableCpus);
    }
}

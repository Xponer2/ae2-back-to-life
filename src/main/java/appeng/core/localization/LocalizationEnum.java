package appeng.core.localization;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public interface LocalizationEnum {

    String getEnglishText();

    String getTranslationKey();

    default ITextComponent text() {
        return new TextComponentTranslation(getTranslationKey());
    }

    default ITextComponent text(Object... args) {
        return new TextComponentTranslation(getTranslationKey(), args);
    }

    default ITextComponent withSuffix(String text) {
        return text().createCopy().appendText(text);
    }

    default ITextComponent withSuffix(ITextComponent text) {
        return text().createCopy().appendSibling(text);
    }

}

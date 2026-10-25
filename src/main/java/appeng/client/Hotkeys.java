package appeng.client;

import java.util.HashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;

import appeng.hotkeys.HotkeyActions;

/**
 * client side component of {@link HotkeyActions}
 */
public class Hotkeys {

    private static final HashMap<String, Hotkey> HOTKEYS = new HashMap<>();

    private static boolean finalized;

    private static Hotkey createHotkey(String id) {
        if (finalized) {
            throw new IllegalStateException("Hotkey registration already finalized!");
        }
        return new Hotkey(id, new KeyBinding("key.ae2." + id, Keyboard.KEY_NONE, "key.ae2.category"));
    }

    private static void registerHotkey(Hotkey hotkey) {
        HOTKEYS.put(hotkey.name(), hotkey);
    }

    public static void finalizeRegistration(Consumer<KeyBinding> register) {
        for (var value : HOTKEYS.values()) {
            register.accept(value.mapping());
        }
        finalized = true;
    }

    public static void registerHotkey(String id) {
        registerHotkey(createHotkey(id));
    }

    public static void checkHotkeys() {
        HOTKEYS.forEach((name, hotkey) -> hotkey.check());
    }

    @Nullable
    public static Hotkey getHotkeyMapping(@Nullable String id) {
        return HOTKEYS.get(id);
    }
}

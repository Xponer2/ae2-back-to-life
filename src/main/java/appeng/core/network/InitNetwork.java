package appeng.core.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.AssemblerAnimationPacket;
import appeng.core.network.clientbound.BlockTransitionEffectPacket;
import appeng.core.network.clientbound.ClearPatternAccessTerminalPacket;
import appeng.core.network.clientbound.CompassResponsePacket;
import appeng.core.network.clientbound.CraftConfirmPlanPacket;
import appeng.core.network.clientbound.CraftingJobStatusPacket;
import appeng.core.network.clientbound.CraftingStatusPacket;
import appeng.core.network.clientbound.ExportedGridContent;
import appeng.core.network.clientbound.GuiDataSyncPacket;
import appeng.core.network.clientbound.ItemTransitionEffectPacket;
import appeng.core.network.clientbound.LightningPacket;
import appeng.core.network.clientbound.MEInventoryUpdatePacket;
import appeng.core.network.clientbound.MatterCannonPacket;
import appeng.core.network.clientbound.MockExplosionPacket;
import appeng.core.network.clientbound.NetworkStatusPacket;
import appeng.core.network.clientbound.OpenMenuPacket;
import appeng.core.network.clientbound.PatternAccessTerminalPacket;
import appeng.core.network.clientbound.SetLinkStatusPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.core.network.serverbound.ColorApplicatorSelectColorPacket;
import appeng.core.network.serverbound.ConfigButtonPacket;
import appeng.core.network.serverbound.ConfirmAutoCraftPacket;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.core.network.serverbound.GuiActionPacket;
import appeng.core.network.serverbound.HotkeyPacket;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.core.network.serverbound.MEInteractionPacket;
import appeng.core.network.serverbound.MouseWheelPacket;
import appeng.core.network.serverbound.PartLeftClickPacket;
import appeng.core.network.serverbound.QuickMovePatternPacket;
import appeng.core.network.serverbound.RequestClosestMeteoritePacket;
import appeng.core.network.serverbound.SelectKeyTypePacket;
import appeng.core.network.serverbound.SwapSlotsPacket;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.core.network.serverbound.UpdateHoldingCtrlPacket;

/**
 * All AE2 packets go through one FML event driven channel ("ae2"). A packet is a var-int id (the registration order
 * below, same list as AE2 for 1.21) followed by the data written by its {@code STREAM_CODEC}. Packets are decoded on
 * the network thread and handled on the main thread of the receiving side.
 */
public final class InitNetwork {
    public static final String CHANNEL = AppEng.MOD_ID;

    private static final List<Registration<?>> BY_ID = new ArrayList<>();
    private static final Map<CustomAppEngPayload.Type<?>, Registration<?>> BY_TYPE = new HashMap<>();

    private static FMLEventChannel channel;

    private InitNetwork() {
    }

    private static final class Registration<T extends CustomAppEngPayload> {
        final int id;
        final CustomAppEngPayload.Type<T> type;
        final StreamCodec<RegistryFriendlyByteBuf, T> codec;
        final boolean toClient;
        final boolean toServer;

        Registration(int id, CustomAppEngPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec,
                boolean toClient, boolean toServer) {
            this.id = id;
            this.type = type;
            this.codec = codec;
            this.toClient = toClient;
            this.toServer = toServer;
        }

        @SuppressWarnings("unchecked")
        void encode(RegistryFriendlyByteBuf buf, CustomAppEngPayload payload) {
            codec.encode(buf, (T) payload);
        }
    }

    /**
     * Registers all packets and the channel. Must be called once during pre-init on both sides.
     */
    public static void init() {
        if (channel != null) {
            return;
        }

        // Clientbound
        clientbound(AssemblerAnimationPacket.TYPE, AssemblerAnimationPacket.STREAM_CODEC);
        clientbound(BlockTransitionEffectPacket.TYPE, BlockTransitionEffectPacket.STREAM_CODEC);
        clientbound(ClearPatternAccessTerminalPacket.TYPE, ClearPatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(CompassResponsePacket.TYPE, CompassResponsePacket.STREAM_CODEC);
        clientbound(CraftConfirmPlanPacket.TYPE, CraftConfirmPlanPacket.STREAM_CODEC);
        clientbound(CraftingJobStatusPacket.TYPE, CraftingJobStatusPacket.STREAM_CODEC);
        clientbound(CraftingStatusPacket.TYPE, CraftingStatusPacket.STREAM_CODEC);
        clientbound(GuiDataSyncPacket.TYPE, GuiDataSyncPacket.STREAM_CODEC);
        clientbound(ItemTransitionEffectPacket.TYPE, ItemTransitionEffectPacket.STREAM_CODEC);
        clientbound(LightningPacket.TYPE, LightningPacket.STREAM_CODEC);
        clientbound(MatterCannonPacket.TYPE, MatterCannonPacket.STREAM_CODEC);
        clientbound(MEInventoryUpdatePacket.TYPE, MEInventoryUpdatePacket.STREAM_CODEC);
        clientbound(MockExplosionPacket.TYPE, MockExplosionPacket.STREAM_CODEC);
        clientbound(NetworkStatusPacket.TYPE, NetworkStatusPacket.STREAM_CODEC);
        clientbound(PatternAccessTerminalPacket.TYPE, PatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(SetLinkStatusPacket.TYPE, SetLinkStatusPacket.STREAM_CODEC);
        clientbound(ExportedGridContent.TYPE, ExportedGridContent.STREAM_CODEC);

        // Serverbound
        serverbound(ColorApplicatorSelectColorPacket.TYPE, ColorApplicatorSelectColorPacket.STREAM_CODEC);
        serverbound(RequestClosestMeteoritePacket.TYPE, RequestClosestMeteoritePacket.STREAM_CODEC);
        serverbound(ConfigButtonPacket.TYPE, ConfigButtonPacket.STREAM_CODEC);
        serverbound(ConfirmAutoCraftPacket.TYPE, ConfirmAutoCraftPacket.STREAM_CODEC);
        serverbound(FillCraftingGridFromRecipePacket.TYPE, FillCraftingGridFromRecipePacket.STREAM_CODEC);
        serverbound(GuiActionPacket.TYPE, GuiActionPacket.STREAM_CODEC);
        serverbound(HotkeyPacket.TYPE, HotkeyPacket.STREAM_CODEC);
        serverbound(InventoryActionPacket.TYPE, InventoryActionPacket.STREAM_CODEC);
        serverbound(MEInteractionPacket.TYPE, MEInteractionPacket.STREAM_CODEC);
        serverbound(MouseWheelPacket.TYPE, MouseWheelPacket.STREAM_CODEC);
        serverbound(PartLeftClickPacket.TYPE, PartLeftClickPacket.STREAM_CODEC);
        serverbound(QuickMovePatternPacket.TYPE, QuickMovePatternPacket.STREAM_CODEC);
        serverbound(SelectKeyTypePacket.TYPE, SelectKeyTypePacket.STREAM_CODEC);
        serverbound(SwapSlotsPacket.TYPE, SwapSlotsPacket.STREAM_CODEC);
        serverbound(SwitchGuisPacket.TYPE, SwitchGuisPacket.STREAM_CODEC);
        serverbound(UpdateHoldingCtrlPacket.TYPE, UpdateHoldingCtrlPacket.STREAM_CODEC);

        // Bidirectional
        register(ConfigValuePacket.TYPE, ConfigValuePacket.STREAM_CODEC, true, true);

        // 1.12.2 only (vanilla opens menus in 1.21), registered last to keep the 1.21 ids of the others
        clientbound(OpenMenuPacket.TYPE, OpenMenuPacket.STREAM_CODEC);

        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
        channel.register(new ServerHandler());
        if (FMLCommonHandler.instance().getSide().isClient()) {
            channel.register(new ClientNetworkHandler());
        }
    }

    private static <T extends ClientboundPacket> void clientbound(CustomAppEngPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        register(type, codec, true, false);
    }

    private static <T extends ServerboundPacket> void serverbound(CustomAppEngPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        register(type, codec, false, true);
    }

    private static <T extends CustomAppEngPayload> void register(CustomAppEngPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, boolean toClient, boolean toServer) {
        if (BY_TYPE.containsKey(type)) {
            throw new IllegalStateException("Packet type registered twice: " + type);
        }
        var registration = new Registration<>(BY_ID.size(), type, codec, toClient, toServer);
        BY_ID.add(registration);
        BY_TYPE.put(type, registration);
    }

    static FMLEventChannel channel() {
        if (channel == null) {
            throw new IllegalStateException("InitNetwork.init() has not been called");
        }
        return channel;
    }

    /**
     * Serializes a packet (id + data). Every send wraps the bytes in a new {@link FMLProxyPacket}.
     */
    static byte[] encode(CustomAppEngPayload payload, boolean toClient) {
        var registration = BY_TYPE.get(payload.type());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered packet " + payload.getClass().getName());
        }
        if (toClient ? !registration.toClient : !registration.toServer) {
            throw new IllegalArgumentException("Packet " + registration.type.id() + " cannot be sent to the "
                    + (toClient ? "client" : "server"));
        }
        var buf = RegistryFriendlyByteBuf.create();
        buf.writeVarInt(registration.id);
        registration.encode(buf, payload);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    static FMLProxyPacket toProxyPacket(byte[] bytes) {
        return new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(bytes)), CHANNEL);
    }

    /**
     * Reads a received packet, null if it is unknown or was sent in the wrong direction.
     */
    @Nullable
    static CustomAppEngPayload decode(FMLProxyPacket packet, boolean onClient) {
        ByteBuf payload = packet.payload();
        var buf = RegistryFriendlyByteBuf.wrap(payload);
        int id = buf.readVarInt();
        if (id < 0 || id >= BY_ID.size()) {
            AELog.warn("Received unknown AE2 packet id %d", id);
            return null;
        }
        var registration = BY_ID.get(id);
        if (onClient ? !registration.toClient : !registration.toServer) {
            AELog.warn("Received AE2 packet %s on the wrong side", registration.type.id());
            return null;
        }
        var result = registration.codec.decode(buf);
        if (buf.isReadable()) {
            AELog.warn("AE2 packet %s has %d unread bytes", registration.type.id(), buf.readableBytes());
        }
        return result;
    }

    public static final class ServerHandler {
        @SubscribeEvent
        public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
            EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
            CustomAppEngPayload packet;
            try {
                packet = decode(event.getPacket(), false);
            } catch (RuntimeException e) {
                AELog.warn(e, "Failed to decode an AE2 packet from " + player.getName());
                return;
            }
            if (packet instanceof ServerboundPacket serverboundPacket) {
                player.getServerWorld().addScheduledTask(() -> serverboundPacket.handleOnServer(player));
            }
        }
    }
}

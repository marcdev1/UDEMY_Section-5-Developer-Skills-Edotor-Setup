package dev.blockov.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Paquet serveur -> client unique : un objet JSON avec un champ "t" (type).
 * Le client le redistribue vers CData (comme dans le mod d'origine, qui lit du Gson).
 */
public record BlockovPayload(String json) implements CustomPacketPayload {
    public static final Type<BlockovPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("blockov", "data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockovPayload> CODEC =
            ByteBufCodecs.stringUtf8(1 << 18).map(BlockovPayload::new, BlockovPayload::json).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

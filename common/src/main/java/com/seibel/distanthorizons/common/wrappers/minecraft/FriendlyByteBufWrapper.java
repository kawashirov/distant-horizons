package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.Charset;

public class FriendlyByteBufWrapper implements IFriendlyByteBuf
{
    private final FriendlyByteBuf buf;

    public FriendlyByteBufWrapper(FriendlyByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public short readShort()
    {
        return buf.readShort();
    }

    public CharSequence readCharSequence(int length, Charset charset)
    {
        return buf.readCharSequence(length, charset);
    }
}

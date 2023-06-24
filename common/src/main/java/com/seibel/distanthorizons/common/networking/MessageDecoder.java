package com.seibel.distanthorizons.common.networking;

import com.seibel.distanthorizons.common.networking.messages.MessageRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    private MessageRegistry messageRegistry;

    public MessageDecoder(MessageRegistry messageRegistry) {
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(messageRegistry.createMessage(in.readShort()).decode(in));
    }
}

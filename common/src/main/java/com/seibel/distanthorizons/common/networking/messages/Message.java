package com.seibel.distanthorizons.common.networking.messages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract class Message {
    public Message() { }

    public void encode(ByteBuf out) {
        throw new UnsupportedOperationException();
    }
    public Message decode(ByteBuf in) {
        throw new UnsupportedOperationException();
    }

    protected void handle_Server(ChannelHandlerContext ctx) {
        throw new UnsupportedOperationException();
    }
    protected void handle_Client(ChannelHandlerContext ctx) {
        throw new UnsupportedOperationException();
    }
}


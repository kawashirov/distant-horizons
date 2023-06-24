package com.seibel.distanthorizons.common.networking;

import com.seibel.distanthorizons.common.networking.messages.MessageHandler;
import com.seibel.distanthorizons.common.networking.messages.MessageHandlerSide;
import com.seibel.distanthorizons.common.networking.messages.MessageRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class LodServer {
    // TODO move to config of some sort
    static final int PORT = 25049;

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(getInitializer());

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {

    }

    private ChannelInitializer<SocketChannel> getInitializer() {
        return new ChannelInitializer<>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                var messageRegistry = new MessageRegistry();

                // Encoding
                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2));
                pipeline.addLast(new MessageDecoder(messageRegistry));
                // TODO packet encoder

                pipeline.addLast(new MessageHandler(MessageHandlerSide.SERVER));
            }
        };
    }
}

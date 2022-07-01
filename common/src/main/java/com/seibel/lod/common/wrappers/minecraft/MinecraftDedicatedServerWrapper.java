package com.seibel.lod.common.wrappers.minecraft;

import com.seibel.lod.core.api.internal.a7.SharedApi;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import net.minecraft.server.dedicated.DedicatedServer;

import java.io.File;

public class MinecraftDedicatedServerWrapper implements IMinecraftSharedWrapper {
    public static final MinecraftDedicatedServerWrapper INSTANCE = new MinecraftDedicatedServerWrapper();
    private MinecraftDedicatedServerWrapper() {}
    public DedicatedServer dedicatedServer = null;
    @Override
    public boolean isDedicatedServer() {
        return true;
    }
    @Override
    public File getInstallationDirectory() {
        if (dedicatedServer == null)
            throw new IllegalStateException("Trying to get Installation Direction before Dedicated server complete initialization!");
        return dedicatedServer.getServerDirectory();
    }
}

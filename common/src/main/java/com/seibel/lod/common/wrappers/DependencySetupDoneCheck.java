package com.seibel.lod.common.wrappers;

import java.util.function.Supplier;

public class DependencySetupDoneCheck
{
	public static boolean isDone = false;
	public static Supplier<Boolean> getIsCurrentThreadDistantGeneratorThread = (() -> {return false;});
	
}

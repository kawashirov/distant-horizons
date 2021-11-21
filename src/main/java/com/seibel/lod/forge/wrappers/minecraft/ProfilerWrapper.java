package com.seibel.lod.forge.wrappers.minecraft;

import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;

import net.minecraft.profiler.IProfiler;

/**
 * 
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class ProfilerWrapper implements IProfilerWrapper
{
	public IProfiler profiler;
	
	public ProfilerWrapper(IProfiler newProfiler)
	{
		profiler = newProfiler;
	}
	
	
	/** starts a new section inside the currently running section */
	@Override
	public void push(String newSection)
	{
		profiler.push(newSection);
	}
	
	/** ends the currently running section and starts a new one */
	@Override
	public void popPush(String newSection)
	{
		profiler.popPush(newSection);
	}
	
	/** ends the currently running section */
	@Override
	public void pop()
	{
		profiler.pop();
	}
}

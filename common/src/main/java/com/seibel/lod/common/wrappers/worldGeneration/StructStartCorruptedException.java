package com.seibel.lod.common.wrappers.worldGeneration;

public class StructStartCorruptedException extends RuntimeException {
	private static final long serialVersionUID = -8987434342051563358L;

	public StructStartCorruptedException(ArrayIndexOutOfBoundsException e) {
		super("StructStartCorruptedException");
		super.initCause(e);
		fillInStackTrace();
	}
}
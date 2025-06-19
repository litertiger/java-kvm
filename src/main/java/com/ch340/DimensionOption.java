package com.ch340;
import java.awt.*;

public class DimensionOption {
	private final Dimension dimension;
	public DimensionOption(Dimension dimension) {
		this.dimension = dimension;
	}
	public Dimension get() {
		return dimension;
	}
	@Override
	public String toString() {
		return "(" + dimension.width + ", " + dimension.height + ")";
	}
}
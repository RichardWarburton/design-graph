package designgrapher.rendering;

import java.io.File;

import designgrapher.ClassHierachy;

public interface Renderer {

	public abstract void render(final File toFile, final ClassHierachy hierachy);

}
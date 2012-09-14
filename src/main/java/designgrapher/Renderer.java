package designgrapher;

import java.io.File;


public interface Renderer {

	public abstract void render(final File toFile, final ClassHierachy hierachy);

}
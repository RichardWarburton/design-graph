package designgrapher;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import designgrapher.rendering.DotRenderer;
import designgrapher.rendering.Renderer;

public class Main {

	public static void main(final String[] args) {
		final List<File> classFiles = listClasses(new File(args[0]));
		final ClassHierachyAnalysis analysis = new ClassHierachyAnalysis();
		analysis.addAllClasses(classFiles);
		analysis.analyse();

		final Renderer renderer = new DotRenderer();
		renderer.render(new File("output.dot"), analysis.getClassHierachy());
	}

	private static List<File> listClasses(final File dir) {
		final List<File> files = new ArrayList<>();
		listClassesRec(dir, files);
		return files;
	}

	private static void listClassesRec(final File dir, final List<File> files) {
		if (dir.isDirectory()) {
			for (final File subdir : dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(final File file) {
					return file.isDirectory();
				}
			})) {
				listClassesRec(subdir, files);
			}
			for (final File file : dir.listFiles()) {
				if (file.isFile() && file.getAbsolutePath().endsWith(".class")) {
					files.add(file);
				}
			}
		}
	}
}

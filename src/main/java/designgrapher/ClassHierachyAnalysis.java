package designgrapher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ClassHierachyAnalysis {

	private static final int FLAGS = 0;

	private final Map<String, ClassNode> classesByName = new HashMap<>();
	private final Map<ClassNode, ClassNode> classesToParent = new HashMap<>(); // CHILD -> PARENT
	private final Multimap<String, ClassNode> classesByPackage = HashMultimap.create(); // PACKAGE -> CLASS

	public void addAllClasses(final List<File> classFiles) {
		for (final File classFile : classFiles) {
			final ClassNode node = loadClass(classFile);
			if (!isInnerClass(node)) {
				classesByName.put(node.name, node);
			}
		}
	}

	private boolean isInnerClass(ClassNode node) {
		// TODO; strengthen this 
		return node.name.contains("$");
	}

	private ClassNode loadClass(final File classFile) {
		try (final FileInputStream in = new FileInputStream(classFile)) {
			final ClassReader reader = new ClassReader(in);
			final ClassNode cls = new ClassNode();
			reader.accept(cls, FLAGS);
			return cls;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void analyse() {
		buildParentLookup();
		buildPackageLookup();
	}

	private void buildPackageLookup() {
		for (final ClassNode cls : classesByName.values()) {
			final int pkgIndex = cls.name.lastIndexOf('/');
			final String packageName = cls.name.substring(0, pkgIndex);
			classesByPackage.put(packageName, cls);
		}
	}

	private void buildParentLookup() {
		for (final ClassNode cls : classesByName.values()) {
			final ClassNode parent = classesByName.get(cls.superName);
			if (parent != null) {
				classesToParent.put(cls, parent);
			}
		}
	}

	public ClassHierachy getClassHierachy() {
		return new ClassHierachy(classesToParent, classesByPackage);
	}

}

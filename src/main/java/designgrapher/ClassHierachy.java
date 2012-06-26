package designgrapher;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ClassHierachy {

	public final Map<ClassNode, ClassNode> classesToParent;
	public final Multimap<String, ClassNode> classesByPackage;

	public ClassHierachy(final Map<ClassNode, ClassNode> classesToParent, final Multimap<String, ClassNode> classesByPackage) {
		this.classesToParent = new HashMap<>(classesToParent);
		this.classesByPackage = HashMultimap.create(classesByPackage);
	}

}

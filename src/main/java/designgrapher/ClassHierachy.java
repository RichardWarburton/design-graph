package designgrapher;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ClassHierachy {

	private static final Comparator<ClassNode> compareClassByName = new Comparator<ClassNode>() {
		@Override
		public int compare(final ClassNode left, final ClassNode right) {
			return left.name.compareTo(right.name);
		}
	};
	
	public final Map<ClassNode, ClassNode> classesToParent;
	public final Multimap<String, ClassNode> classesByPackage;
	public final SortedMap<String, SortedSet<ClassNode>> classesByPackageSorted;

	public ClassHierachy(final Map<ClassNode, ClassNode> classesToParent, final Multimap<String, ClassNode> classesByPackage) {
		this.classesToParent = new HashMap<>(classesToParent);
		this.classesByPackage = HashMultimap.create(classesByPackage);
		classesByPackageSorted = new TreeMap<>();
		makeSortedPackageLookup();
	}
	
	public void makeSortedPackageLookup() {
		for (final Entry<String, Collection<ClassNode>> e : classesByPackage.asMap().entrySet()) {
			final SortedSet<ClassNode> nodes = new TreeSet<>(compareClassByName);
			nodes.addAll(e.getValue());
			classesByPackageSorted.put(e.getKey(), nodes);
		}
	}

}

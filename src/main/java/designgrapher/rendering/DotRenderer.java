package designgrapher.rendering;

import static com.google.common.collect.Collections2.transform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.stringtemplate.v4.ST;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import designgrapher.ClassHierachy;


// TODO: refactor this class entirely and model rendering properly
@SuppressWarnings("unchecked")
public class DotRenderer implements Renderer {

	private ClassHierachy hierachy;

	@Override
	public void render(final File toFile, final ClassHierachy hierachy) {
		this.hierachy = hierachy;
		final SortedMap<String, SortedSet<ClassNode>> packageLookup = makeSortedPackageLookup(hierachy);
		final Collection<String> packages = transform(packageLookup.entrySet(), makePackage);
		final Collection<String> relationShips = renderRelationShips(packageLookup);
		final String entireGraph = renderGraph(Iterables.concat(packages, relationShips));
		try {
			Files.write(entireGraph, toFile, Charset.defaultCharset());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private SortedMap<String, SortedSet<ClassNode>> makeSortedPackageLookup(final ClassHierachy hierachy) {
		final SortedMap<String, SortedSet<ClassNode>> sorted = new TreeMap<>();
		for (final Entry<String, Collection<ClassNode>> e : hierachy.classesByPackage.asMap().entrySet()) {
			final SortedSet<ClassNode> nodes = new TreeSet<>(compareClassByName);
			nodes.addAll(e.getValue());
			sorted.put(e.getKey(), nodes);
		}
		return sorted;
	}

	private Collection<String> renderRelationShips(final SortedMap<String, SortedSet<ClassNode>> sortedClasses) {
		final Collection<String> relationships = new ArrayList<>();
		// sigh
		int packageIndex = 0;
		for (final SortedSet<ClassNode> pkg : sortedClasses.values()) {
			int classIndex = 0;
			for (final ClassNode cls : pkg) {
				final ClassNode parent = hierachy.classesToParent.get(cls);
				String classReference = makeClassReference(packageIndex, classIndex);
				if (parent != null) {
					String parentReference = makeClassReference(find(sortedClasses, parent));
					makeExtends(relationships, classReference, parentReference);
				}
				for (Object inter : cls.interfaces) {
					Entry<Integer, Integer> interfaceIndex = find(sortedClasses, (String) inter);
					if (interfaceIndex == null)
						continue;
					
					makeImplements(relationships, classReference, makeClassReference(interfaceIndex));
				}
				renderLocalInformation(classReference, cls.methods, sortedClasses, relationships);
				classIndex++;
			}
			packageIndex++;
		}
		return relationships;
	}

	private void renderLocalInformation(String classReference, List<MethodNode> methods, SortedMap<String, SortedSet<ClassNode>> sortedClasses, Collection<String> relationships) {
		Set<String> classes = Sets.newHashSet();
		Set<String> called = Sets.newHashSet();
		for (MethodNode method : methods) {
			for (AbstractInsnNode instruction : method.instructions.toArray()) {
				if (instruction instanceof MethodInsnNode) {
					MethodInsnNode methodCall = (MethodInsnNode) instruction;
					called.add(methodCall.owner);
				} else if (instruction instanceof LdcInsnNode) {
					LdcInsnNode ldc = (LdcInsnNode) instruction;
					if (ldc.cst instanceof Type) {
						Type typeLiteral = (Type) ldc.cst;
						classes.add(typeLiteral.getInternalName());
					}
				}
			}
		}
		for (String calledClass : called) {
			Entry<Integer, Integer> calledIndex = find(sortedClasses, calledClass);
			if (calledIndex == null)
				continue;
			
			String calledReference = makeClassReference(calledIndex);
			makeCall(relationships, classReference, calledReference);
		}
		
		classes.removeAll(called);
		for (String cls : classes) {
			Entry<Integer, Integer> clsIndex = find(sortedClasses, cls);
			if (clsIndex == null)
				continue;
			
			String calledReference = makeClassReference(clsIndex);
			makeClassLiteral(relationships, classReference, calledReference);
		}
	}

	private void makeClassLiteral(final Collection<String> relationships, String classReference, String calledReference) {
		makeRelationship(relationships, classReference, calledReference, "black");
	}
	
	private void makeCall(final Collection<String> relationships, String classReference, String calledReference) {
		makeRelationship(relationships, classReference, calledReference, "grey");
	}

	private void makeExtends(final Collection<String> relationships, String classReference, String parentReference) {
		makeRelationship(relationships, classReference, parentReference, "blue");
	}

	private void makeImplements(final Collection<String> relationships, String classReference, String interfaceReference) {
		makeRelationship(relationships, classReference, interfaceReference, "green");
	}

	private void makeRelationship(final Collection<String> relationships, String classReference, String parentReference, String colour) {
		relationships.add(classReference + " -> " + parentReference + " [color=" + colour + "];\n");
	}
	
	private String makeClassReference(Entry<Integer, Integer> index) {
		return makeClassReference(index.getKey(), index.getValue());
	}

	private String makeClassReference(final int packageIndex, final int classIndex) {
		final ST relation = new ST("cls_<packageIndex>_<classIndex>");
		relation.add("packageIndex", packageIndex);
		relation.add("classIndex", classIndex);
		return relation.render();
	}
	
	private Entry<Integer, Integer> find(final SortedMap<String, SortedSet<ClassNode>> sorted, final String toFind) {
		return find(sorted, new Predicate<ClassNode>() {
			@Override
			public boolean apply(ClassNode input) {
				return input.name.equals(toFind);
			}
		});
	}
	
	private Entry<Integer, Integer> find(final SortedMap<String, SortedSet<ClassNode>> sorted, final ClassNode toFind) {
		return find(sorted, new Predicate<ClassNode>() {
			@Override
			public boolean apply(ClassNode input) {
				return input == toFind;
			}
		});
	}

	private Entry<Integer, Integer> find(final SortedMap<String, SortedSet<ClassNode>> sorted, final Predicate<ClassNode> toFind) {
		int packageIndex = 0;
		for (final SortedSet<ClassNode> pkg : sorted.values()) {
			int classIndex = 0;
			for (final ClassNode cls : pkg) {
				if (toFind.apply(cls)) {
					return Maps.immutableEntry(packageIndex, classIndex);
				}
				classIndex++;
			}
			packageIndex++;
		}
		return null;
	}

	private String renderGraph(final Iterable<String> children) {
		final ST graph = new ST("digraph G { \n fontname = \"Helvetica\" \n fontsize = 8\n<inner>\n }");
		graph.add("inner", Joiner.on('\n').join(children));
		return graph.render();
	}

	private final Function<Entry<String, SortedSet<ClassNode>>, String> makePackage = new Function<Entry<String, SortedSet<ClassNode>>, String>() {

		private int packageIndex = 0;

		@Override
		public String apply(final Entry<String, SortedSet<ClassNode>> pkg) {
			final String packageName = pkg.getKey();
			final List<ClassNode> classes = new ArrayList<>(pkg.getValue());
			final String classString = renderClassString(classes);
			final ST cluster = makeCluster(classString, packageName);
			return cluster.render();
		}

		private ST makeCluster(final String classString, final String packageName) {
			final ST cluster = new ST("subgraph cluster_<packageIndex> { node [style=filled]; <classes> label = \"<name>\"; color=blue;fontsize=15 }");
			cluster.add("packageIndex", packageIndex++);
			cluster.add("classes", classString);
			cluster.add("name", toDottedName(packageName));
			return cluster;
		}

		private String renderClassString(final List<ClassNode> classes) {
			final StringBuilder classString = new StringBuilder();
			for (int classIndex = 0; classIndex < classes.size(); classIndex++) {
				final ST classNode = new ST("cls_<packageIndex>_<classIndex> [label=\"<name>\"];\n");
				classNode.add("packageIndex", packageIndex);
				classNode.add("classIndex", classIndex);
				classNode.add("name", getName(classes.get(classIndex)));
				classString.append(classNode.render());
			}
			return classString.toString();
		}
	};

	private final Comparator<ClassNode> compareClassByName = new Comparator<ClassNode>() {
		@Override
		public int compare(final ClassNode left, final ClassNode right) {
			return left.name.compareTo(right.name);
		}
	};
	
	private String toDottedName(String internal) {
		return internal.replace('/', '.');
	}

	private String getName(final ClassNode cls) {
		int lastSlash = cls.name.lastIndexOf('/');
		if (lastSlash == -1)
			return cls.name;
		
		return cls.name.substring(lastSlash + 1);
	}

}

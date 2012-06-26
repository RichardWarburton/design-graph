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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.tree.ClassNode;
import org.stringtemplate.v4.ST;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import designgrapher.ClassHierachy;

public class DotRenderer implements Renderer {

	private ClassHierachy hierachy;

	/**
	 * @see designgrapher.rendering.Renderer#render(java.io.File, ClassHierachy)
	 */
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

	private Collection<String> renderRelationShips(final SortedMap<String, SortedSet<ClassNode>> sorted) {
		final Collection<String> relationships = new ArrayList<>();
		// sigh
		int packageIndex = 0;
		for (final SortedSet<ClassNode> pkg : sorted.values()) {
			int classIndex = 0;
			for (final ClassNode cls : pkg) {
				final ClassNode parent = hierachy.classesToParent.get(cls);
				if (parent != null) {
					final Entry<Integer, Integer> parentIndex = find(sorted, parent);
					relationships.add(refClass(packageIndex, classIndex) + " -> " + refClass(parentIndex.getKey(), parentIndex.getValue()) + ";\n");
				}
				classIndex++;
			}
			packageIndex++;
		}
		return relationships;
	}

	private String refClass(final int packageIndex, final int classIndex) {
		final ST relation = new ST("cls_<packageIndex>_<classIndex>");
		relation.add("packageIndex", packageIndex);
		relation.add("classIndex", classIndex);
		return relation.render();
	}

	private Entry<Integer, Integer> find(final SortedMap<String, SortedSet<ClassNode>> sorted, final ClassNode toFind) {
		int packageIndex = 0;
		for (final SortedSet<ClassNode> pkg : sorted.values()) {
			int classIndex = 0;
			for (final ClassNode cls : pkg) {
				if (cls == toFind) {
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
			final ST cluster = new ST("subgraph cluster_<packageIndex> { node [style=filled]; <classes> label = \"<name>\"; color=blue }");
			cluster.add("packageIndex", packageIndex++);
			cluster.add("classes", classString);
			cluster.add("name", packageName);
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

	private String getName(final ClassNode cls) {
		return cls.name.replace('/', '.');
	}

}

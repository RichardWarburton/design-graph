package designgrapher;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.concat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;

import org.objectweb.asm.tree.ClassNode;
import org.stringtemplate.v4.ST;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class DotRenderer {

	public void render(final File toFile, final ClassHierachy hierachy) {
		final Collection<String> packages = transform(hierachy.classesByPackageSorted.entrySet(), makePackage);
		final String entireGraph = renderGraph(concat(packages, Relationships.of(hierachy)));
		try {
			Files.write(entireGraph, toFile, Charset.defaultCharset());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
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
				classNode.add("name", getShortName(classes.get(classIndex)));
				classString.append(classNode.render());
			}
			return classString.toString();
		}
	};
	
	private String toDottedName(String internal) {
		return internal.replace('/', '.');
	}

	private String getShortName(final ClassNode cls) {
		int lastSlash = cls.name.lastIndexOf('/');
		if (lastSlash == -1)
			return cls.name;
		
		return cls.name.substring(lastSlash + 1);
	}

}

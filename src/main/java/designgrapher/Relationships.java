package designgrapher;

import static com.google.common.collect.Sets.difference;
import static designgrapher.RelationshipType.CALLS;
import static designgrapher.RelationshipType.EXTENDS;
import static designgrapher.RelationshipType.IMPLEMENTS;
import static designgrapher.RelationshipType.REFERS_TO_LITERAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Sets;

public class Relationships {

	public static List<String> of(ClassHierachy hierachy) {
		return new Relationships(hierachy).asList();
	}

	private final List<String> toRender;
	private final ClassHierachy hierachy;

	private Relationships(ClassHierachy hierachy) {
		this.hierachy = hierachy;
		toRender = new ArrayList<>();
		buildRelationShips();
	}

	// ASM Sucks
	@SuppressWarnings("unchecked")
	private void buildRelationShips() {
		int packageIndex = 0;
		for (final SortedSet<ClassNode> pkg : hierachy.classesByPackageSorted.values()) {
			int classIndex = 0;
			for (final ClassNode cls : pkg) {
				final ClassNode parent = hierachy.classesToParent.get(cls);
				ClassIdentifier id = new ClassIdentifier(packageIndex, classIndex);

				ClassIdentifier parentId = hierachy.findByNode(parent);
				newRelationShip(id, parentId, EXTENDS);

				List<String> interfaces = new ArrayList<>(cls.interfaces);
				newRelationShips(interfaces, id, IMPLEMENTS);

				analyseMethods(id, cls.methods);
				classIndex++;
			}
			packageIndex++;
		}
	}

	private void analyseMethods(ClassIdentifier cls, List<MethodNode> methods) {
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
		newRelationShips(called, cls, CALLS);
		newRelationShips(difference(classes, called), cls, REFERS_TO_LITERAL);
	}

	private void newRelationShips(Collection<String> from, ClassIdentifier to,
			RelationshipType type) {
		for (String className : from) {
			newRelationShip(hierachy.findByName(className), to, type);
		}
	}

	private void newRelationShip(ClassIdentifier from, ClassIdentifier to,
			RelationshipType type) {
		if (from.isKnown() && to.isKnown())
			toRender.add(from + " -> " + to + " [color=" + type.colour + "];\n");
	}

	private List<String> asList() {
		return toRender;
	}

}

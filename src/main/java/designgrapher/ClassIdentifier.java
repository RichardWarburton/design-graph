package designgrapher;

import org.stringtemplate.v4.ST;

/**
 * Identifies a class in a graph, since class names aren't valid dot node identifiers 'cls_<packageIndex>_<classIndex>' is used to identify classes.
 */
public class ClassIdentifier {

	private final int packageIndex;
	private final int classIndex;
	private final boolean known;

	public ClassIdentifier(int packageIndex, int classIndex) {
		known = true;
		this.packageIndex = packageIndex;
		this.classIndex = classIndex;
	}

	public ClassIdentifier() {
		known = false;
		packageIndex = -1;
		classIndex = -1;
	}

	@Override
	public String toString() {
		final ST relation = new ST("cls_<packageIndex>_<classIndex>");
		relation.add("packageIndex", packageIndex);
		relation.add("classIndex", classIndex);
		return relation.render();
	}

	public boolean isKnown() {
		return known;
	}

}

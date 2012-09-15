package designgrapher;

enum RelationshipType {

	IMPLEMENTS("green"),
	EXTENDS("blue"),
	CALLS("grey"),
	REFERS_TO_LITERAL("black");
	
	private RelationshipType(final String colour) {
		this.colour = colour;
	}
	
	final String colour;

}

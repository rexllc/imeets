package gq.fora.app.models.story.reactions;

public class ReactionType {
	public String reaction_type;
	public String reactor_id;
	
	public String getType() {
		return this.reaction_type;
	}
	
	public String getReactorId() {
		return this.reactor_id;
	}
}
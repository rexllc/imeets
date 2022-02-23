package gq.fora.app.models;

public class RoomParticipant {
	
	public String participantId;
	public String creatorId;
	
	public RoomParticipant(String userId, String creatorId) {
		this.participantId = userId;
		this.creatorId = creatorId;
	}
	
}
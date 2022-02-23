package gq.fora.app.models;

public class RoomBuilder {
	
	public String creator_id;
	public String room_id;
	public String room_name;
	public long timestamp;
	public boolean isRoomActive;
	
	public RoomBuilder() {
		//Default
	}
	
	public RoomBuilder(String userId, String roomName, String roomId) {
		this.creator_id = userId;
		this.room_id = roomId;
		this.room_name = roomName;
		this.timestamp = System.currentTimeMillis();
		this.isRoomActive = true;
	}
}
package gq.fora.app.models;

public class Room {
	
	public String creator_id;
	public String room_id;
	public String room_name;
	public long timestamp;
	public boolean isRoomActive;
	
	public String getCreatorId() {
		return this.creator_id;
	}
	
	public String getRoomId() {
		return this.room_id;
	}
	
	public String getRoomName() {
		return this.room_name;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public boolean isRoomActive() {
		return this.isRoomActive = true;
	}
}
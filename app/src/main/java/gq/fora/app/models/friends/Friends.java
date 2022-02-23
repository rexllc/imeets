package gq.fora.app.models.friends;

public class Friends {

    public boolean isFriends;
    public String friendsId;
    public String userId;
    public String status;
    public String receiverId;
    public String senderId;

    public boolean isFriends() {
        return this.isFriends == true;
    }

    public String getFriendsId() {
        return this.friendsId;
    }

    public String getStatus() {
        return this.status;
    }

    public String getUserId() {
        return this.userId;
    }
}

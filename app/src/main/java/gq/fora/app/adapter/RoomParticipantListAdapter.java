package gq.fora.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import de.hdodenhof.circleimageview.CircleImageView;
import gq.fora.app.R;
import gq.fora.app.models.Participant;
import gq.fora.app.models.list.viewmodel.User;
import java.util.ArrayList;

public class RoomParticipantListAdapter extends RecyclerView.Adapter {

    private ArrayList<Participant> data;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");

    public RoomParticipantListAdapter(ArrayList<Participant> arr) {
        data = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View userView =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.users, parent, false);
        // Return a new holder instance
        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        userView.setLayoutParams(lp);
        return new UserViewHolder(userView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Participant user = data.get((int) position);
        ((UserViewHolder) holder).bind(user);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView avatar, online;
        private TextView display_name;

        UserViewHolder(View view) {
            super(view);
            avatar = (CircleImageView) view.findViewById(R.id.avatar);
            online = (CircleImageView) view.findViewById(R.id.online);
            display_name = (TextView) view.findViewById(R.id.display_name);
        }

        void bind(Participant data) {

            users.child(data.getParticipantId())
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    User user = _dataSnapshot.getValue(User.class);

                                    display_name.setText(user.displayName);
                                    Glide.with(avatar)
                                            .load(user.userPhoto)
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(avatar);
											
									if (user.isOnline) {
										online.setVisibility(View.VISIBLE);
									} else {
										online.setVisibility(View.GONE);
									}
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            display_name.setTextColor(0xFFE0E0E0);
        }
    }
}

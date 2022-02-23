package gq.fora.app.adapter;

import android.content.Context;
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
import gq.fora.app.models.friends.Friends;
import gq.fora.app.models.list.viewmodel.User;

import java.util.ArrayList;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.ViewHolder> {

    private FirebaseDatabase firebase;
    private DatabaseReference users;
	private Context context;
    private ArrayList<Friends> peoples;

    public PeopleAdapter(Context context) {
        this.context = context;
        peoples = new ArrayList<>();
		firebase = FirebaseDatabase.getInstance();
        users = firebase.getReference("users");
    }

    public void setItems(ArrayList<Friends> friendsArrayList) {
        this.peoples = friendsArrayList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view =
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.user_list, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Friends friends = peoples.get(position);
        users.child(friends.getFriendsId())
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                if (user != null) {
                                    Glide.with(context).load(user.userPhoto).into(holder.avatar);
									holder.display_name.setText(user.displayName);
									if (user.isOnline) {
										holder.status.setText("Online");
									} else {
										holder.status.setText("Offline");
									}
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
    }

    @Override
    public int getItemCount() {
        return peoples.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView avatar;
        private TextView display_name;
        private TextView status;

        public ViewHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.avatar);
            display_name = view.findViewById(R.id.display_name);
            status = view.findViewById(R.id.username);
        }
    }
}

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
import gq.fora.app.models.CallType;
import gq.fora.app.models.list.viewmodel.User;

import java.util.ArrayList;
import java.util.HashMap;

public class CallsAdapter extends RecyclerView.Adapter<CallsAdapter.ViewHolder> {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference users;
    private Context context;
    private ArrayList<HashMap<String, Object>> chatItem;

    public CallsAdapter(Context context) {
        this.context = context;
        firebaseDatabase = FirebaseDatabase.getInstance();
        users = firebaseDatabase.getReference("users");
        chatItem = new ArrayList<>();
    }

    public void setItems(ArrayList<HashMap<String, Object>> chatItemArrayList) {
        this.chatItem = chatItemArrayList;
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
        if (chatItem.get(position).get("type").toString().equals("50")) {
            users.child(chatItem.get(position).get("chat_id").toString())
                    .addValueEventListener(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (user != null) {
                                        Glide.with(context)
                                                .load(user.userPhoto)
                                                .into(holder.avatar);
                                        holder.display_name.setText(user.displayName);
                                        if (chatItem.contains("call_type")) {
                                            switch (Integer.parseInt(
                                                    chatItem.get(position)
                                                            .get("call_type")
                                                            .toString())) {
                                                case CallType.FAILURE:
                                                    holder.status.setText("Missed call.");
                                                    break;
                                                case CallType.CANCELLED:
                                                    holder.status.setText("Missed call.");
                                                    break;
                                                case CallType.HUNG_UP:
                                                    holder.status.setText("Outgoing.");
                                                    break;
                                                case CallType.NO_ANSWER:
                                                    holder.status.setText("Missed call.");
                                                    break;
                                                case CallType.TIMEOUT:
                                                    holder.status.setText("Missed call.");
                                                    break;
                                                case CallType.DENIED:
                                                    holder.status.setText("No Answer.");
                                                    break;
                                                case CallType.OTHER_DEVICE_ANSWERED:
                                                    holder.status.setText(
                                                            "Anwsered, but in other device.");
                                                    break;
                                            }
                                        } else {
											holder.status.setText("Called.");
										}
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {}
                            });
        }
    }

    @Override
    public int getItemCount() {
        return chatItem.size();
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

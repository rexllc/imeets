package gq.fora.app.adapter;

import android.content.Context;
import android.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import de.hdodenhof.circleimageview.CircleImageView;
import gq.fora.app.R;
import gq.fora.app.models.ChatItem;
import gq.fora.app.models.UserConfig;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ChatViewAdapter extends RecyclerView.Adapter<ChatViewAdapter.ViewHolder> {

    private String TAG = "FORA";
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");

    private ArrayList<ChatItem> item;
	private Calendar cal = Calendar.getInstance();

    public ChatViewAdapter(ArrayList<ChatItem> arr) {
        item = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the custom layout
        View chatListView = inflater.inflate(R.layout.chat_view, parent, false);
        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(chatListView);
        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chatListView.setLayoutParams(lp);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ChatItem data = item.get(position);

        holder.msg_1().setText(data.getContent());
        holder.msg_2().setText(data.getContent());

        String userId = data.getAuthorId();

        if (userId.equals(UserConfig.getInstance().getUid())) {
            holder.msg_1.setVisibility(View.GONE);
            holder.msg_2.setVisibility(View.VISIBLE);
        } else {
            holder.msg_1.setVisibility(View.VISIBLE);
            holder.msg_2.setVisibility(View.GONE);
        }

        users.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot _dataSnapshot) {

                        try {
                            GenericTypeIndicator<HashMap<String, Object>> _ind =
                                    new GenericTypeIndicator<HashMap<String, Object>>() {};
                            for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                                HashMap<String, Object> _map = _data.getValue(_ind);

                                if (_map.containsKey("userId")) {
                                    if (_map.get("userId").toString().equals(userId)) {
                                        Glide.with(holder.itemView)
                                                .load(_map.get("user_photo"))
                                                .into(holder.avatar);
                                    }
                                }
                            }
                        } catch (Exception _e) {
                            _e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError _databaseError) {}
                });
			cal.setTimeInMillis(data.getTimestamp());
		holder.time_1.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
		holder.time_2.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
    }

    @Override
    public int getItemCount() {
        return item.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView avatar;
        private TextView msg_1, msg_2, time_1, time_2;
        private LinearLayout user_1, user_2;

        public ViewHolder(View view) {
            super(view);
            avatar = (CircleImageView) view.findViewById(R.id.avatar);
            msg_1 = (TextView) view.findViewById(R.id.msg_1);
            msg_2 = (TextView) view.findViewById(R.id.msg_2);
            time_1 = (TextView) view.findViewById(R.id.time_1);
            time_2 = (TextView) view.findViewById(R.id.time_2);
            user_1 = (LinearLayout) view.findViewById(R.id.user_1);
            user_2 = (LinearLayout) view.findViewById(R.id.user_2);
        }

        public TextView msg_1() {
            return msg_1;
        }

        public TextView msg_2() {
            return msg_2;
        }

        public CircleImageView getPhoto() {
            return avatar;
        }

        public LinearLayout user_1() {
            return user_1;
        }

        public LinearLayout user_2() {
            return user_2;
        }
    }
}

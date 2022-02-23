package gq.fora.app.adapter;

import android.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.vanniktech.emoji.EmojiTextView;
import de.hdodenhof.circleimageview.CircleImageView;
import gq.fora.app.R;
import gq.fora.app.models.list.viewmodel.User;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class UserListAdapter extends RecyclerView.Adapter {

    private String TAG = "FORA";
    private List<User> item;
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_NONE = 0;

    public UserListAdapter(List<User> arr) {
        item = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View userView =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list, parent, false);
        // Return a new holder instance
        return new UserViewHolder(userView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
		User items = item.get(position);
        ((UserViewHolder) holder).bind(items);
    }

    @Override
    public int getItemCount() {
        return item.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView avatar;
        private TextView display_name;
        private EmojiTextView username;

        public UserViewHolder(View view) {
            super(view);
            avatar = (CircleImageView) view.findViewById(R.id.avatar);
            username = (EmojiTextView) view.findViewById(R.id.username);
            display_name = (TextView) view.findViewById(R.id.display_name);
        }

        void bind(User user) {
            display_name.setText(user.displayName);
            username.setText("@" + user.username);
            Glide.with(avatar).load(user.userPhoto).into(avatar);
        }
    }
}

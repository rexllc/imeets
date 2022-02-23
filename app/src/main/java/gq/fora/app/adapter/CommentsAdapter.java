package gq.fora.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanniktech.emoji.EmojiTextView;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.models.ui.Comments;

import java.util.ArrayList;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private ArrayList<Comments> list = new ArrayList<>();
    private Context context;

    private EmojiTextView text;
    private CircleImageView avatar;
    private TextView display_name;

    public CommentsAdapter(Context ctx) {
        context = ctx;
    }

    public void setItems(ArrayList<Comments> data) {
        list.addAll(data);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.comment_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        holder.itemView.setLayoutParams(lp);
        ((ViewHolder) holder).bind(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.text);
            avatar = view.findViewById(R.id.avatar);
            display_name = view.findViewById(R.id.display_name);
        }

        public void bind(int position) {
            Comments comments = list.get(position);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(comments.userId)
                    .addSnapshotListener(eventListener);

            text.setText(comments.text);
        }
    }

    private EventListener<DocumentSnapshot> eventListener =
            (value, error) -> {
                if (error != null) return;
                display_name.setText(value.getString("displayName"));
                Glide.with(avatar.getContext())
                        .load(value.getString("userPhoto"))
                        .skipMemoryCache(true)
                        .thumbnail(0.1f)
                        .circleCrop()
                        .into(avatar);
            };
}

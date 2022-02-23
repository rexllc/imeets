package gq.fora.app.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.initializeApp;
import gq.fora.app.models.list.Story;

import java.util.ArrayList;

public class StoryListAdapter extends RecyclerView.Adapter {

    private String TAG = "FORA";
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private ArrayList<Story> item;
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_NONE = 2;
    private SharedPreferences sharedPreferences;

    public StoryListAdapter(ArrayList<Story> arr) {
        item = arr;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_USER;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View userView;
        // Inflate the custom layout
        if (viewType == VIEW_TYPE_USER) {
            userView =
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.users, parent, false);
            // Return a new holder instance
            return new UserViewHolder(userView);
        }

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parent.setLayoutParams(lp);

        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Story data = item.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER:
                ((UserViewHolder) holder).bind(data);
                break;
            case VIEW_TYPE_NONE:
        }
    }

    @Override
    public int getItemCount() {
        return item.size();
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

        void bind(Story data) {
            sharedPreferences =
                    initializeApp.context.getSharedPreferences("themes", Context.MODE_PRIVATE);

            database.collection("users")
                    .document(data.userId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    display_name.setText(value.getString("displayName"));
                                    Glide.with(avatar.getContext())
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(avatar);

                                    if (value.getBoolean("isOnline")) {
                                        online.setVisibility(View.VISIBLE);
                                    } else {
                                        online.setVisibility(View.GONE);
                                    }
                                }
                            });

            if (sharedPreferences.getString("dark_mode", "").equals("true")) {
                online.setCircleBackgroundColor(0xFF212121);
                display_name.setTextColor(0xFFFFFFFF);
                online.setBorderColor(0xFF212121);
            } else {
                online.setCircleBackgroundColor(0xFFFFFFFF);
                display_name.setTextColor(0xFF000000);
                online.setBorderColor(0xFFFFFFFF);
            }
        }
    }
}

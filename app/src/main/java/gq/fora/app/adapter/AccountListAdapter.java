package gq.fora.app.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.ViewHolder> {

    private ArrayList<HashMap<String, Object>> data;
    private ArrayList<HashMap<String, Object>> accountList = new ArrayList<>();
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private Activity context;

    public AccountListAdapter(@NonNull Activity ctx) {
        context = ctx;
    }

    public void setItems(ArrayList<HashMap<String, Object>> arr) {
        data = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.account_list, parent, false);
        ViewHolder holder = new ViewHolder(view);
        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String path = FileUtils.getPackageDataDir(context) + "/user/accounts.json";
        String path2 = FileUtils.getPackageDataDir(context) + "/user";
        users.child(data.get((int) position).get("userId").toString())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot arg0) {
                                User user = arg0.getValue(User.class);
                                holder.displayName.setText(user.displayName);
                                Glide.with(holder.itemView)
                                        .load(user.userPhoto)
                                        .into(holder.avatar);
                            }

                            @Override
                            public void onCancelled(DatabaseError arg0) {}
                        });

        if (data.get((int) position)
                .get("userId")
                .toString()
                .equals(UserConfig.getInstance().getUid())) {
            holder.status.setText("Signed in");
            holder.itemView.setEnabled(false);
            holder.options.setEnabled(false);
        } else {
            holder.status.setText("Not signed in");
            holder.itemView.setEnabled(true);
            holder.options.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView displayName, status;
        CircleImageView avatar;
        ImageView options;

        public ViewHolder(View view) {
            super(view);

            displayName = view.findViewById(R.id.display_name);
            avatar = view.findViewById(R.id.avatar);
            status = view.findViewById(R.id.status);
            options = view.findViewById(R.id.options);
        }
    }
}

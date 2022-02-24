package gq.fora.app.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.view.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.vanniktech.emoji.EmojiTextView;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.initializeApp;
import gq.fora.app.models.ChatItem;
import gq.fora.app.models.Conversation;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

public class ChatListAdapter extends RecyclerView.Adapter {

    private String TAG = "FORA";
    private static final int SENDER = 1;
    private static final int RECEIVER = 2;
    private ArrayList<Conversation> item;
    private ChatListAdapter adapter;
    private ChatItem chat;
    private User user;
    private Calendar timeToday = Calendar.getInstance();
    private Calendar afterDay = Calendar.getInstance();
    private SharedPreferences sharedPreferences;

    private CircleImageView avatar, avatar2, sent;
    private TextView display_name, time;
    private EmojiTextView last_message;

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference users = database.collection("users");
    private CollectionReference messages = database.collection("messages");

    /*Native Ads*/

    private NativeAdView nativeAd;

    private ItemClickListener itemClickListener;

    public ChatListAdapter() {}

    public void setItems(ArrayList<Conversation> arr) {
        item = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the custom layout
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list, parent, false);
        // Return a new holder instance
        if (viewType == RECEIVER) return new ReceiverViewHolder(view);
        return new SenderViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Conversation data = item.get(position);
        if (data.senderId.equals(UserConfig.getInstance().getUid())) return RECEIVER;
        return SENDER;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        holder.itemView.setLayoutParams(lp);

        sharedPreferences =
                initializeApp.context.getSharedPreferences("themes", Context.MODE_PRIVATE);

        Conversation data = item.get(position);

        time.setText(DateUtils.getRelativeTimeSpanString(time.getContext(), data.chatTime));
        time.setTextColor(0xFF8D8E90);

        database.collection("messages")
                .document(UserConfig.getInstance().getUid())
                .collection(data.chatId)
                .orderBy("chatTime")
                .addSnapshotListener(eventListener);

        if (data.senderId.equals(UserConfig.getInstance().getUid())) {
            ((ReceiverViewHolder) holder).bind(data);
        } else {
            ((SenderViewHolder) holder).bind(data);
        }

        sent.setColorFilter(0xFF8D8E90);

        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            display_name.setTextColor(0xFFFFFFFF);
            sent.setCircleBackgroundColor(0xFF212121);
            avatar2.setCircleBackgroundColor(0xFF212121);
            time.setTextColor(0xFFFFFFFF);
            last_message.setTextColor(0xFFFFFFFF);
            sent.setBorderColor(0xFF212121);
        } else {
            display_name.setTextColor(0xFF000000);
            sent.setCircleBackgroundColor(0xFFFFFFFF);
            avatar2.setCircleBackgroundColor(0xFFFFFFFF);
            time.setTextColor(0xFF8D8E90);
            last_message.setTextColor(0xFF8D8E90);
            sent.setBorderColor(0xFFFFFFFF);
        }

        /*Initialize Native Advanced*/

        AdLoader.Builder builder =
                new AdLoader.Builder(
                        holder.itemView.getContext(), "ca-app-pub-1252287720252625/2199568605");
        builder.forNativeAd(
                new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        // Show the ad.
                        FrameLayout frameLayout = holder.itemView.findViewById(R.id.ad_placeholder);
                        NativeAdView adView =
                                (NativeAdView)
                                        LayoutInflater.from(holder.itemView.getContext())
                                                .inflate(R.layout.ad_unified, null);
                        _populateNativeAd(nativeAd, adView);

                        if (position != 4) {
                            // REMOVE
                            frameLayout.removeAllViews();
                        }

                        frameLayout.addView(adView);
                    }
                });
        AdLoader adLoader =
                builder.withAdListener(
                                new AdListener() {
                                    @Override
                                    public void onAdFailedToLoad(LoadAdError adError) {
                                        // Handle the failure by logging, altering the UI, and so
                                        // on.
                                    }
                                })
                        .withNativeAdOptions(
                                new NativeAdOptions.Builder()
                                        // Methods in the NativeAdOptions.Builder class can be
                                        // used here to specify individual options settings.
                                        .build())
                        .build();

        Utils.rippleRoundStroke(holder.itemView, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");

        holder.itemView.setOnClickListener(
                (v) -> {
                    itemClickListener.onItemClick(holder.itemView, position);
                });

        holder.itemView.setOnLongClickListener(
                (v) -> {
                    itemClickListener.onItemLongClick(holder.itemView, position);
                    return false;
                });
    }

    @Override
    public int getItemCount() {
        return item.size();
    }

    public class ReceiverViewHolder extends RecyclerView.ViewHolder {

        public ReceiverViewHolder(View view) {
            super(view);
            avatar = (CircleImageView) view.findViewById(R.id.avatar);
            display_name = (TextView) view.findViewById(R.id.display_name);
            last_message = (EmojiTextView) view.findViewById(R.id.last_message);
            time = (TextView) view.findViewById(R.id.time);
            avatar2 = (CircleImageView) view.findViewById(R.id.avatar2);
            sent = (CircleImageView) view.findViewById(R.id.sent);
        }

        public void bind(Conversation data) {
            database.collection("users")
                    .document(data.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    display_name.setText(value.getString("displayName"));
                                    Glide.with(avatar)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(avatar);
                                }
                            });
        }
    }

    public void _populateNativeAd(final NativeAd _nativeAd, final NativeAdView _adView) {
        _adView.setHeadlineView(_adView.findViewById(R.id.ad_headline));
        _adView.setIconView(_adView.findViewById(R.id.ad_app_icon));
        _adView.setPriceView(_adView.findViewById(R.id.ad_price));
        ((TextView) _adView.getHeadlineView()).setText(_nativeAd.getHeadline());
        if (_nativeAd.getIcon() == null) {
            _adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) _adView.getIconView()).setImageDrawable(_nativeAd.getIcon().getDrawable());
            _adView.getIconView().setVisibility(View.VISIBLE);
        }
        if (_nativeAd.getPrice() == null) {
            _adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            _adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) _adView.getPriceView()).setText(_nativeAd.getPrice());
        }
        _adView.setNativeAd(_nativeAd);
    }

    public class SenderViewHolder extends RecyclerView.ViewHolder {
        public SenderViewHolder(View view) {
            super(view);
            avatar = (CircleImageView) view.findViewById(R.id.avatar);
            display_name = (TextView) view.findViewById(R.id.display_name);
            last_message = (EmojiTextView) view.findViewById(R.id.last_message);
            time = (TextView) view.findViewById(R.id.time);
            avatar2 = (CircleImageView) view.findViewById(R.id.avatar2);
            sent = (CircleImageView) view.findViewById(R.id.sent);
        }

        public void bind(Conversation data) {
            database.collection("users")
                    .document(data.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    display_name.setText(value.getString("displayName"));
                                    Glide.with(avatar)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(avatar);
                                }
                            });
        }
    }

    private EventListener<QuerySnapshot> eventListener =
            (value, error) -> {
                if (error != null) return;

                if (value != null) {
                    for (DocumentChange docs : value.getDocumentChanges()) {
                        last_message.setText(docs.getDocument().getString("message"));
                    }
                }
            };

    public interface ItemClickListener {
        public void onItemClick(View view, int position);

        public void onItemLongClick(View view, int position);
    }

    public void setOnItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }
}

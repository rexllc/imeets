package gq.fora.app.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.vanniktech.emoji.EmojiTextView;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.initializeApp;
import gq.fora.app.models.CallType;
import gq.fora.app.models.MessageType;
import gq.fora.app.models.Messages;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.ViewType;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.Utils;

import io.github.ponnamkarthik.richlinkpreview.RichLinkViewSkype;
import io.github.ponnamkarthik.richlinkpreview.ViewListener;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MessageListAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private ArrayList<Messages> mMessageList;
    private Calendar cal = Calendar.getInstance();
    private Calendar other_day = Calendar.getInstance();
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private DatabaseReference messages = _firebase.getReference("messages");
    private DatabaseReference messages_db02 = _firebase.getReference("messages_db02");
    private ChildEventListener _messages_db01_child_listener;
    private ChildEventListener _messages_db02_child_listener;
    private int lastPosition;
    private String chat_path_01;
    private String chat_path_02;
    private SharedPreferences themes;
	private FirebaseFirestore database = FirebaseFirestore.getInstance();

    private static ItemTouchListener mItemTouchListener;
    private String senderId;

    public MessageListAdapter(Context context, String senderId) {
        mContext = context;
        mMessageList = new ArrayList<>();
        senderId = senderId;
    }

    public void setChatItems(ArrayList<Messages> messageList) {
        mMessageList = messageList;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {

        Messages messages = mMessageList.get(position);

        if (mMessageList.get(position).senderId.equals(UserConfig.getInstance().getUid())) {
            if (mMessageList.get(position).type == MessageType.PHOTO) return ViewType.IMAGE_SENT;
            if (mMessageList.get(position).type == MessageType.CALL) return ViewType.CALL_SENT;
            if (Utils.isContainsLinks(mMessageList.get(position).message))
                return ViewType.LINK_SENT;
            return ViewType.MESSAGE_SENT;
        } else {
            if (mMessageList.get(position).type == MessageType.PHOTO)
                return ViewType.IMAGE_RECEIVED;
            if (mMessageList.get(position).type == MessageType.CALL) return ViewType.CALL_RECEIVED;
            if (Utils.isContainsLinks(messages.message)) return ViewType.LINK_RECEIVED;
            return ViewType.MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == ViewType.MESSAGE_SENT) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_me, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == ViewType.MESSAGE_RECEIVED) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_other, parent, false);
            return new ReceivedMessageHolder(view);
        } else if (viewType == ViewType.IMAGE_SENT) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_me_with_image, parent, false);
            return new SentImageHolder(view);
        } else if (viewType == ViewType.IMAGE_RECEIVED) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_other_with_image, parent, false);
            return new ReceivedImageHolder(view);
        } else if (viewType == ViewType.CALL_SENT) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_me_with_call, parent, false);
            return new SentCallHolder(view);
        } else if (viewType == ViewType.CALL_RECEIVED) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_other_with_call, parent, false);
            return new ReceivedCallHolder(view);
        } else if (viewType == ViewType.DATE_HEADER) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_date_headers, parent, false);
            return new DateHeaderHolder(view);
        } else if (viewType == ViewType.LINK_SENT) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_me_with_link, parent, false);
            return new SentLinkViewHolder(view);
        } else if (viewType == ViewType.LINK_RECEIVED) {
            view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_other_with_link, parent, false);
            return new ReceivedLinkViewHolder(view);
        }

        RecyclerView.LayoutParams lp =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.setLayoutParams(lp);

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        themes = initializeApp.context.getSharedPreferences("themes", Context.MODE_PRIVATE);

        Messages message = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case ViewType.MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message, position);
                break;
            case ViewType.MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message, position);
                break;
            case ViewType.IMAGE_SENT:
                ((SentImageHolder) holder).bind(message, position);
                break;
            case ViewType.IMAGE_RECEIVED:
                ((ReceivedImageHolder) holder).bind(message, position);
                break;
            case ViewType.CALL_SENT:
                ((SentCallHolder) holder).bind(message, position);
                break;
            case ViewType.CALL_RECEIVED:
                ((ReceivedCallHolder) holder).bind(message, position);
                break;
            case ViewType.LINK_SENT:
                ((SentLinkViewHolder) holder).bind(message, position);
                break;
            case ViewType.DATE_HEADER:
                ((DateHeaderHolder) holder).bind(message);
                break;
            case ViewType.LINK_RECEIVED:
                ((ReceivedLinkViewHolder) holder).bind(message, position);
        }

        setAnimation(holder.itemView, position);

        // seen(message);
        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mItemTouchListener.onItemClick(position, view);
                    }
                });

        holder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        mItemTouchListener.onItemLongClick(position, view);
                        return false;
                    }
                });
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {

        private TextView timeText, text_gchat_date_me, text_message_seen;
        private EmojiTextView messageText;
        private CardView card_gchat_message_me;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = (EmojiTextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            text_gchat_date_me = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            text_message_seen = (TextView) itemView.findViewById(R.id.text_message_seen);
            card_gchat_message_me = (CardView) itemView.findViewById(R.id.card_gchat_message_me);
        }

        void bind(Messages message, int position) {

            setRoundRightCorner(card_gchat_message_me, position);

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_me.setVisibility(View.GONE);
                } else {
                    text_gchat_date_me.setVisibility(View.VISIBLE);
                }
            } else {
                text_gchat_date_me.setVisibility(View.VISIBLE);
            }

            String msg = "";

            try {
                SecretKeySpec key = (SecretKeySpec) generateKey(message.senderId);

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, key);
                byte[] decode = Base64.decode(message.message, Base64.DEFAULT);
                byte[] decval = c.doFinal(decode);
                messageText.setText(new String(decval));
                msg = new String(decval);
            } catch (Exception e) {
                messageText.setText(message.message);
                msg = message.message;
            }

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            // timeText.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            timeText.setVisibility(View.GONE);

            if (themes.getString("dark_mode", "").equals("true")) {
                text_message_seen.setTextColor(0xFFFFFFFF);
                text_gchat_date_me.setTextColor(0xFFFFFFFF);
            } else {
                text_message_seen.setTextColor(0xFF9E9E9E);
                text_gchat_date_me.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        private TextView timeText, nameText, text_gchat_date_other;
        private CircleImageView profileImage;
        private EmojiTextView messageText;
        private User model;
        private CardView card_gchat_message_other;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = (EmojiTextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            profileImage = (CircleImageView) itemView.findViewById(R.id.image_gchat_profile_other);
            nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            text_gchat_date_other = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            card_gchat_message_other =
                    (CardView) itemView.findViewById(R.id.card_gchat_message_other);
        }

        void bind(Messages message, int position) {

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_other.setVisibility(View.GONE);
                    profileImage.setVisibility(View.INVISIBLE);
                    nameText.setVisibility(View.GONE);
                } else {
                    text_gchat_date_other.setVisibility(View.VISIBLE);
                    profileImage.setVisibility(View.VISIBLE);
                    nameText.setVisibility(View.GONE);
                }
            } else {
                text_gchat_date_other.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.VISIBLE);
                nameText.setVisibility(View.GONE);
            }

            String msg = "";

            try {
                SecretKeySpec key = (SecretKeySpec) generateKey(message.senderId);

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, key);
                byte[] decode = Base64.decode(message.message, Base64.DEFAULT);
                byte[] decval = c.doFinal(decode);
                messageText.setText(new String(decval));
                msg = new String(decval);
                if (themes.getString("dark_mode", "").equals("true")) {
                    card_gchat_message_other.setCardBackgroundColor(0xFF424242);
                } else {
                    card_gchat_message_other.setCardBackgroundColor(0xFFEEF1F6);
                }
                messageText.setTypeface(Typeface.create("normal", Typeface.NORMAL));
            } catch (Exception e) {
                messageText.setText(message.message);
                if (themes.getString("dark_mode", "").equals("true")) {
                    card_gchat_message_other.setCardBackgroundColor(0xFF424242);
                    messageText.setTypeface(Typeface.create("italic", Typeface.ITALIC));
                    messageText.setTextColor(0xFFFFFFFF);
                    text_gchat_date_other.setTextColor(0xFFFFFFFF);
                } else {
                    messageText.setTextColor(0xFF000000);
                    text_gchat_date_other.setTextColor(0xFF9E9E9E);
                    card_gchat_message_other.setCardBackgroundColor(0xFFEEF1F6);

                    switch (themes.getString("wallpaper", "")) {
                        case "wallpapers/wallpaper_00.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_01.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_02.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_03.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_04.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_05.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_06.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_07.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_08.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_09.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_10.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_11.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_12.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_13.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_14.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                        case "wallpapers/wallpaper_17.jpg":
                            text_gchat_date_other.setTextColor(0xFFFFFFFF);
                            messageText.setTextColor(0xFF000000);
                            break;
                    }
                }
                msg = message.message;
            }

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            text_gchat_date_other.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            timeText.setVisibility(View.GONE);
			database.collection("users")
                    .document(message.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    nameText.setText(value.getString("displayName"));
                                    Glide.with(profileImage)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(profileImage);
                                }
                            });
            setRoundLeftCorner(card_gchat_message_other, profileImage, position);
        }
    }

    private class SentImageHolder extends RecyclerView.ViewHolder {
        TextView timeText, text_gchat_date_me, text_message_seen;
        ImageView image_body;
        CardView card_gchat_message_me;

        SentImageHolder(View itemView) {
            super(itemView);

            image_body = (ImageView) itemView.findViewById(R.id.image_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            text_gchat_date_me = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            text_message_seen = (TextView) itemView.findViewById(R.id.text_message_seen);
            card_gchat_message_me = (CardView) itemView.findViewById(R.id.card_gchat_message_me);
        }

        void bind(Messages message, int position) {

            setRoundRightCorner(card_gchat_message_me, position);

            if (message.imageUrl != null) {
                Glide.with(itemView)
                        .load(message.imageUrl)
                        .skipMemoryCache(true)
                        .thumbnail(0.1f)
                        .into(image_body);
            }
            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            // timeText.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setText(
                    new SimpleDateFormat("MMM d", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setVisibility(View.GONE);
            timeText.setVisibility(View.GONE);

            if (themes.getString("dark_mode", "").equals("true")) {
                card_gchat_message_me.setCardBackgroundColor(0xFF424242);
                text_message_seen.setTextColor(0xFFFFFFFF);
                text_gchat_date_me.setTextColor(0xFFFFFFFF);
            } else {
                card_gchat_message_me.setCardBackgroundColor(0xFFEEF1F6);
                text_message_seen.setTextColor(0xFF9E9E9E);
                text_gchat_date_me.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
        }
    }

    private class ReceivedImageHolder extends RecyclerView.ViewHolder {

        private TextView timeText, nameText, text_gchat_date_other;
        private CircleImageView profileImage;
        private ImageView image_body;
        private User model;
        private CardView card_gchat_message_other;

        ReceivedImageHolder(View itemView) {
            super(itemView);

            image_body = (ImageView) itemView.findViewById(R.id.image_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            profileImage = (CircleImageView) itemView.findViewById(R.id.image_gchat_profile_other);
            nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            text_gchat_date_other = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            card_gchat_message_other =
                    (CardView) itemView.findViewById(R.id.card_gchat_message_other);
        }

        void bind(Messages message, int position) {

            if (message.imageUrl != null) {
                Glide.with(itemView)
                        .load(message.imageUrl)
                        .skipMemoryCache(true)
                        .thumbnail(0.1f)
                        .into(image_body);
            }

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_other.setVisibility(View.GONE);
                    profileImage.setVisibility(View.INVISIBLE);
                    nameText.setVisibility(View.GONE);
                } else {
                    text_gchat_date_other.setVisibility(View.VISIBLE);
                    profileImage.setVisibility(View.VISIBLE);
                    nameText.setVisibility(View.GONE);
                }
            } else {
                text_gchat_date_other.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.VISIBLE);
                nameText.setVisibility(View.GONE);
            }

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            text_gchat_date_other.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));
            timeText.setVisibility(View.GONE);

            users.child(message.senderId)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    model = _dataSnapshot.getValue(User.class);

                                    if (model != null) {
                                        nameText.setText(model.displayName);
                                        Glide.with(itemView)
                                                .load(model.userPhoto)
                                                .skipMemoryCache(true)
                                                .thumbnail(0.1f)
                                                .into(profileImage);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            if (themes.getString("dark_mode", "").equals("true")) {
                card_gchat_message_other.setCardBackgroundColor(0xFF424242);
                text_gchat_date_other.setTextColor(0xFFFFFFFF);
            } else {
                card_gchat_message_other.setCardBackgroundColor(0xFFEEF1F6);
                text_gchat_date_other.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
			
			database.collection("users")
                    .document(message.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    nameText.setText(value.getString("displayName"));
                                    Glide.with(profileImage)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(profileImage);
                                }
                            });

            setRoundLeftCorner(card_gchat_message_other, profileImage, position);
        }
    }

    private class SentCallHolder extends RecyclerView.ViewHolder {

        private TextView timeText, text_gchat_date_me, text_message_seen, call_title, call_time;
        private ImageView call_status;
        private User model;
        private CardView card_gchat_message_me;

        SentCallHolder(View itemView) {
            super(itemView);

            call_status = (ImageView) itemView.findViewById(R.id.call_status);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            call_title = (TextView) itemView.findViewById(R.id.call_title);
            call_time = (TextView) itemView.findViewById(R.id.call_time);
            text_gchat_date_me = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            text_message_seen = (TextView) itemView.findViewById(R.id.text_message_seen);
            card_gchat_message_me = (CardView) itemView.findViewById(R.id.card_gchat_message_me);
        }

        void bind(Messages message, int position) {

            setRoundRightCorner(card_gchat_message_me, position);

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            // timeText.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setText(
                    new SimpleDateFormat("MMM d", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setVisibility(View.GONE);
            timeText.setVisibility(View.GONE);
            call_time.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            call_status.setColorFilter(0xFFFFFFFF);

            call_status.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b, int c, int d) {
                            this.setCornerRadius(a);
                            this.setStroke(b, c);
                            this.setColor(d);
                            return this;
                        }
                    }.getIns((int) 360, (int) 0, 0xFFF44336, 0xFFF44336));

            users.child(message.senderId)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    model = _dataSnapshot.getValue(User.class);

                                    if (model != null) {
                                        switch (message.callType) {
                                            case CallType.FAILURE:
                                                call_title.setText(
                                                        model.displayName + " missed your call.");
                                                break;
                                            case CallType.CANCELLED:
                                                call_title.setText(
                                                        model.displayName + " missed your call.");
                                                break;
                                            case CallType.HUNG_UP:
                                                call_title.setText(
                                                        "You called " + model.displayName);
                                                break;
                                            case CallType.NO_ANSWER:
                                                call_title.setText(
                                                        model.displayName + " missed your call.");
                                                break;
                                            case CallType.TIMEOUT:
                                                call_title.setText(
                                                        model.displayName + " missed your call.");
                                                break;
                                            case CallType.DENIED:
                                                call_title.setText(
                                                        model.displayName + " denied your call.");
                                                break;
                                            case CallType.OTHER_DEVICE_ANSWERED:
                                                call_title.setText(
                                                        "You called " + model.displayName);
                                                break;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            if (themes.getString("dark_mode", "").equals("true")) {
                text_message_seen.setTextColor(0xFFFFFFFF);
                text_gchat_date_me.setTextColor(0xFFFFFFFF);
                card_gchat_message_me.setCardBackgroundColor(0xFF424242);
                call_title.setTextColor(0xFFFFFFFF);
                call_time.setTextColor(0xFFFFFFFF);

            } else {
                text_message_seen.setTextColor(0xFF9E9E9E);
                text_gchat_date_me.setTextColor(0xFF9E9E9E);
                card_gchat_message_me.setCardBackgroundColor(0xFFEEF1F6);
                call_title.setTextColor(0xFF000000);
                call_time.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
        }
    }

    private class ReceivedCallHolder extends RecyclerView.ViewHolder {

        private TextView timeText, nameText, text_gchat_date_other, call_title, call_time;
        private CircleImageView profileImage;
        private ImageView call_status;
        private User model, model2;
        private CardView card_gchat_message_other;

        ReceivedCallHolder(View itemView) {
            super(itemView);

            call_status = (ImageView) itemView.findViewById(R.id.call_status);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            call_title = (TextView) itemView.findViewById(R.id.call_title);
            call_time = (TextView) itemView.findViewById(R.id.call_time);
            profileImage = (CircleImageView) itemView.findViewById(R.id.image_gchat_profile_other);
            nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            text_gchat_date_other = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            card_gchat_message_other =
                    (CardView) itemView.findViewById(R.id.card_gchat_message_other);
        }

        void bind(Messages message, int position) {

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_other.setVisibility(View.GONE);
                    profileImage.setVisibility(View.INVISIBLE);
                    nameText.setVisibility(View.GONE);
                } else {
                    text_gchat_date_other.setVisibility(View.VISIBLE);
                    profileImage.setVisibility(View.VISIBLE);
                    nameText.setVisibility(View.GONE);
                }
            } else {
                text_gchat_date_other.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.VISIBLE);
                nameText.setVisibility(View.GONE);
            }

            timeText.setVisibility(View.GONE);

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            text_gchat_date_other.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));
            text_gchat_date_other.setVisibility(View.GONE);

            call_time.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            call_status.setColorFilter(0xFFFFFFFF);

            call_status.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b, int c, int d) {
                            this.setCornerRadius(a);
                            this.setStroke(b, c);
                            this.setColor(d);
                            return this;
                        }
                    }.getIns((int) 360, (int) 0, 0xFFF44336, 0xFFF44336));

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(message.chatId)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    model2 = _dataSnapshot.getValue(User.class);

                                    if (model2 != null) {
                                        switch (message.callType) {
                                            case CallType.FAILURE:
                                                call_title.setText(
                                                        "You missed a call from "
                                                                + model2.displayName);
                                                break;
                                            case CallType.CANCELLED:
                                                call_title.setText(
                                                        "You missed a call from "
                                                                + model2.displayName);
                                                break;
                                            case CallType.HUNG_UP:
                                                call_title.setText(
                                                        model2.displayName + " called you.");
                                                break;
                                            case CallType.NO_ANSWER:
                                                call_title.setText(
                                                        "You missed a call from "
                                                                + model2.displayName);
                                                break;
                                            case CallType.TIMEOUT:
                                                call_title.setText(
                                                        "You missed a call from"
                                                                + model2.displayName);
                                                break;
                                            case CallType.DENIED:
                                                call_title.setText(
                                                        "You denied a call from "
                                                                + model2.displayName);
                                                break;
                                            case CallType.OTHER_DEVICE_ANSWERED:
                                                call_title.setText(
                                                        model2.displayName + " called you.");
                                                break;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            users.child(message.senderId)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    model = _dataSnapshot.getValue(User.class);

                                    if (model != null) {
                                        nameText.setText(model.displayName);
                                        Glide.with(itemView)
                                                .load(model.userPhoto)
                                                .skipMemoryCache(true)
                                                .thumbnail(0.1f)
                                                .into(profileImage);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            if (themes.getString("dark_mode", "").equals("true")) {
                card_gchat_message_other.setCardBackgroundColor(0xFF424242);
                call_title.setTextColor(0xFFFFFFFF);
                call_time.setTextColor(0xFFFFFFFF);
                text_gchat_date_other.setTextColor(0xFFFFFFFF);
            } else {
                card_gchat_message_other.setCardBackgroundColor(0xFFEEF1F6);
                call_title.setTextColor(0xFF000000);
                call_time.setTextColor(0xFF9E9E9E);
                text_gchat_date_other.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
			
			database.collection("users")
                    .document(message.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    nameText.setText(value.getString("displayName"));
                                    Glide.with(profileImage)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(profileImage);
                                }
                            });

            setRoundLeftCorner(card_gchat_message_other, profileImage, position);
        }
    }

    private class SentLinkViewHolder extends RecyclerView.ViewHolder {

        private TextView timeText, text_gchat_date_me, text_message_seen;
        private EmojiTextView messageText;
        private RichLinkViewSkype link_preview;

        SentLinkViewHolder(View itemView) {
            super(itemView);

            messageText = (EmojiTextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            text_gchat_date_me = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            text_message_seen = (TextView) itemView.findViewById(R.id.text_message_seen);
            link_preview = (RichLinkViewSkype) itemView.findViewById(R.id.link_preview);
        }

        void bind(Messages message, int position) {

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_me.setVisibility(View.GONE);
                } else {
                    text_gchat_date_me.setVisibility(View.VISIBLE);
                }
            } else {
                text_gchat_date_me.setVisibility(View.VISIBLE);
            }

            String msg = "";

            try {
                SecretKeySpec key = (SecretKeySpec) generateKey(message.senderId);

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, key);
                byte[] decode = Base64.decode(message.message, Base64.DEFAULT);
                byte[] decval = c.doFinal(decode);
                messageText.setText(new String(decval));
                msg = new String(decval);
            } catch (Exception e) {
                messageText.setText(message.message);
                msg = message.message;
            }

            UrlDetector parser = new UrlDetector(msg, UrlDetectorOptions.Default);
            List<Url> found = parser.detect();

            link_preview.setLink(
                    found.get(0).getOriginalUrl(),
                    new ViewListener() {

                        @Override
                        public void onSuccess(boolean status) {}

                        @Override
                        public void onError(Exception e) {}
                    });

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            // timeText.setText(new SimpleDateFormat("H:mm", Locale.US).format(cal.getTime()));
            text_gchat_date_me.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            timeText.setVisibility(View.GONE);

            if (themes.getString("dark_mode", "").equals("true")) {
                text_message_seen.setTextColor(0xFFFFFFFF);
                text_gchat_date_me.setTextColor(0xFFFFFFFF);
                link_preview.setBackgroundColor(0xFF424242);

            } else {
                text_message_seen.setTextColor(0xFF9E9E9E);
                text_gchat_date_me.setTextColor(0xFF9E9E9E);
                link_preview.setBackgroundColor(0xFFEEF1F6);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_me.setTextColor(0xFFFFFFFF);
                        break;
                }
            }
        }
    }

    private class ReceivedLinkViewHolder extends RecyclerView.ViewHolder {

        private TextView timeText, nameText, text_gchat_date_other;
        private CircleImageView profileImage;
        private EmojiTextView messageText;
        private RichLinkViewSkype link_preview;
        private User model;
        private CardView card_gchat_message_other;

        ReceivedLinkViewHolder(View itemView) {
            super(itemView);

            messageText = (EmojiTextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            profileImage = (CircleImageView) itemView.findViewById(R.id.image_gchat_profile_other);
            nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            text_gchat_date_other = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            link_preview = (RichLinkViewSkype) itemView.findViewById(R.id.link_preview);
            card_gchat_message_other =
                    (CardView) itemView.findViewById(R.id.card_gchat_message_other);
        }

        void bind(Messages message, int position) {

            if (position > 0) {
                if (mMessageList
                        .get(position)
                        .senderId
                        .equalsIgnoreCase(mMessageList.get(position - 1).senderId)) {
                    text_gchat_date_other.setVisibility(View.GONE);
                    profileImage.setVisibility(View.INVISIBLE);
                    nameText.setVisibility(View.GONE);
                } else {
                    text_gchat_date_other.setVisibility(View.VISIBLE);
                    profileImage.setVisibility(View.VISIBLE);
                    nameText.setVisibility(View.GONE);
                }
            } else {
                text_gchat_date_other.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.VISIBLE);
                nameText.setVisibility(View.GONE);
            }

            String msg = "";

            try {
                SecretKeySpec key = (SecretKeySpec) generateKey(message.senderId);

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, key);
                byte[] decode = Base64.decode(message.senderId, Base64.DEFAULT);
                byte[] decval = c.doFinal(decode);
                messageText.setText(new String(decval));
                msg = new String(decval);
            } catch (Exception e) {
                messageText.setText(message.message);
                msg = message.message;
            }

            UrlDetector parser = new UrlDetector(msg, UrlDetectorOptions.Default);
            List<Url> found = parser.detect();

            link_preview.setLink(
                    found.get(0).getOriginalUrl(),
                    new ViewListener() {

                        @Override
                        public void onSuccess(boolean status) {}

                        @Override
                        public void onError(Exception e) {}
                    });

            // Format the stored timestamp into a readable String using method.
            cal.setTimeInMillis(message.chatTime);
            text_gchat_date_other.setText(
                    DateUtils.getRelativeTimeSpanString(timeText.getContext(), message.chatTime));

            timeText.setVisibility(View.GONE);

            users.child(message.senderId)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot _dataSnapshot) {

                                    model = _dataSnapshot.getValue(User.class);

                                    if (model != null) {
                                        nameText.setText(model.displayName);
                                        Glide.with(itemView)
                                                .load(model.userPhoto)
                                                .skipMemoryCache(true)
                                                .thumbnail(0.1f)
                                                .into(profileImage);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError _databaseError) {}
                            });

            if (themes.getString("dark_mode", "").equals("true")) {
                card_gchat_message_other.setCardBackgroundColor(0xFF424242);
                messageText.setTextColor(0xFFFFFFFF);
                text_gchat_date_other.setTextColor(0xFFFFFFFF);
            } else {
                card_gchat_message_other.setCardBackgroundColor(0xFFEEF1F6);
                messageText.setTextColor(0xFF000000);
                text_gchat_date_other.setTextColor(0xFF9E9E9E);
                switch (themes.getString("wallpaper", "")) {
                    case "wallpapers/wallpaper_00.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_01.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_02.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_03.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_04.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_05.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_06.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_07.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_08.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_09.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_10.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_11.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_12.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_13.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_14.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                    case "wallpapers/wallpaper_17.jpg":
                        text_gchat_date_other.setTextColor(0xFFFFFFFF);
                        break;
                }
            }

            database.collection("users")
                    .document(message.chatId)
                    .addSnapshotListener(
                            new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(
                                        DocumentSnapshot value, FirebaseFirestoreException error) {
                                    nameText.setText(value.getString("displayName"));
                                    Glide.with(profileImage)
                                            .load(value.getString("userPhoto"))
                                            .skipMemoryCache(true)
                                            .thumbnail(0.1f)
                                            .into(profileImage);
                                }
                            });

            setRoundLeftCorner(card_gchat_message_other, profileImage, position);
        }
    }

    public class DateHeaderHolder extends RecyclerView.ViewHolder {

        TextView date_headers;

        DateHeaderHolder(View itemView) {
            super(itemView);
            date_headers = (TextView) itemView.findViewById(R.id.date_headers);
        }

        void bind(Messages message) {
            date_headers.setText(
                    DateUtils.getRelativeTimeSpanString(
                            date_headers.getContext(), message.chatTime));
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    private SecretKey generateKey(String pwd) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] b = pwd.getBytes("UTF-8");

        digest.update(b, 0, b.length);

        byte[] key = digest.digest();

        SecretKeySpec sec = new SecretKeySpec(key, "AES");

        return sec;
    }

    public void seen(Messages message) {}

    private void setRoundRightCorner(View _view, int position) {
        if (position == 0) {
            cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 5, 35);
            if (mMessageList.size() == 1) {
                cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 35, 35);
            } else {
                if (!mMessageList
                        .get(position)
                        .senderId
                        .equals(mMessageList.get(position + 1).senderId)) {
                    cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 35, 35);
                }
            }
        } else {
            if (!mMessageList
                    .get(position)
                    .senderId
                    .equals(mMessageList.get(position - 1).senderId)) {
                cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 5, 35);
                if (position == mMessageList.size() - 1) {
                    cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 35, 35);
                } else {
                    if (!mMessageList
                            .get(position)
                            .senderId
                            .equals(mMessageList.get(position + 1))) {
                        cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 35, 35);
                    }
                }
            } else {
                if (position == mMessageList.size() - 1) {
                    if (mMessageList
                            .get(position)
                            .senderId
                            .equals(mMessageList.get(position - 1))) {
                        cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 5, 35);
                    } else {
                        cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 35, 35);
                    }
                } else {
                    if (mMessageList.get(position).senderId.equals(mMessageList.get(position - 1))
                            && mMessageList
                                    .get(position)
                                    .senderId
                                    .equals(mMessageList.get(position + 1))) {
                        cornerRadius(_view, "#f43159", "#f43159", 0, 35, 5, 5, 35);
                    } else {
                        if (mMessageList
                                .get(position)
                                .senderId
                                .equals(mMessageList.get(position + 1))) {
                            cornerRadius(_view, "#f43159", "#f43159", 0, 35, 35, 5, 35);
                        } else {
                            cornerRadius(_view, "#f43159", "#f43159", 0, 35, 5, 35, 35);
                        }
                    }
                }
            }
        }
    }

    private void setRoundLeftCorner(View _view, View _view2, int position) {
        if (position == 0) {
            cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 5);
            if (mMessageList.size() == 1) {
                _view2.setVisibility(View.VISIBLE);
            } else {
                if (!mMessageList
                        .get(position)
                        .senderId
                        .equals(mMessageList.get(position + 1).senderId)) {
                    cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 35);
                    _view2.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (!mMessageList
                    .get(position)
                    .senderId
                    .equals(mMessageList.get(position - 1).senderId)) {
                cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 5);
                _view2.setVisibility(View.INVISIBLE);
                if (position == mMessageList.size() - 1) {
                    cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 35);
                    _view2.setVisibility(View.VISIBLE);
                } else {
                    if (!mMessageList
                            .get(position)
                            .senderId
                            .equals(mMessageList.get(position + 1))) {
                        cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 35);
                        _view2.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                if (position == mMessageList.size() - 1) {
                    if (mMessageList
                            .get(position)
                            .senderId
                            .equals(mMessageList.get(position - 1))) {
                        cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 5, 35, 35, 35);
                    } else {
                        cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 35);
                    }
                    _view2.setVisibility(View.VISIBLE);
                } else {
                    if (mMessageList.get(position).senderId.equals(mMessageList.get(position - 1))
                            && mMessageList
                                    .get(position)
                                    .senderId
                                    .equals(mMessageList.get(position + 1))) {
                        cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 5, 35, 5);
                        _view2.setVisibility(View.INVISIBLE);
                    } else {
                        if (mMessageList
                                .get(position)
                                .senderId
                                .equals(mMessageList.get(position + 1))) {
                            cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 35, 35, 5);
                            _view2.setVisibility(View.INVISIBLE);
                        } else {
                            cornerRadius(_view, "#eef1f6", "#eef1f6", 0, 35, 5, 35, 35);
                            _view2.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }
    }

    private void cornerRadius(
            View _view,
            String _color1,
            String _color2,
            int _str,
            float _n1,
            float _n2,
            float _n3,
            float _n4) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(_color1));
        gd.setStroke(_str, Color.parseColor(_color2));

        gd.setCornerRadii(new float[] {_n1, _n1, _n2, _n2, _n3, _n3, _n4, _n4});
        _view.setBackground(gd);
        _view.setElevation(1);
    }

    public interface ItemTouchListener {
        void onItemClick(int position, View v);

        void onItemLongClick(int position, View v);
    }

    public void setOnItemClickListener(ItemTouchListener itemTouchListener) {
        mItemTouchListener = itemTouchListener;
    }
}

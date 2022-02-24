package gq.fora.app.activities;

import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;
import com.sinch.relinker.TextUtils;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.adapter.MessageListAdapter;
import gq.fora.app.initializeApp;
import gq.fora.app.models.Constants;
import gq.fora.app.models.Messages;
import gq.fora.app.models.UserConfig;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends BaseFragment {

    private String TAG = ChatActivity.class.getSimpleName();

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference messagesRef = database.collection(Constants.KEY_COLLECTION_CHAT);
    private CollectionReference inboxRef = database.collection("inbox");

    private CircleImageView avatar;
    private ImageView back,
            video_call_button,
            call_button,
            pick_image,
            camera,
            button_send,
            emojiButton;
    private LinearLayout toolbar, msg_layout;
    private EmojiEditText input_text;
    private TextView blocked, name, empty, status;
    private ProgressBar loader;
    private EmojiPopup emojiPopup;

    private ArrayList<Messages> listMessages = new ArrayList<>();
    private MessageListAdapter listAdapter;
    private RecyclerView mMessageslist;
    private LinearLayoutManager layoutManager;

    private SlidrInterface slidrInterface;
    private SlidrListener slidrListener;

    private String conversationId = null;
    private String token = null;
    private JSONObject rootObject, subObject;
    private RequestQueue requestQueue;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {}

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_chat, parent, false);
        initViews(savedInstanceState, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (slidrInterface == null) {
            slidrInterface =
                    Slidr.replace(
                            getView().findViewById(R.id.content_container),
                            new SlidrConfig.Builder().position(SlidrPosition.LEFT).build());
        }
    }

    private void initViews(Bundle savedInstanceState, View view) {
        avatar = view.findViewById(R.id.avatar);
        back = view.findViewById(R.id.back);
        video_call_button = view.findViewById(R.id.video_call_button);
        call_button = view.findViewById(R.id.call_button);
        pick_image = view.findViewById(R.id.pick_image);
        camera = view.findViewById(R.id.camera);
        button_send = view.findViewById(R.id.button_send);
        emojiButton = view.findViewById(R.id.emojiButton);
        toolbar = view.findViewById(R.id.toolbar);
        msg_layout = view.findViewById(R.id.msg_layout);
        input_text = view.findViewById(R.id.input_text);
        blocked = view.findViewById(R.id.blocked);
        name = view.findViewById(R.id.name);
        empty = view.findViewById(R.id.empty);
        loader = view.findViewById(R.id.loader);
        mMessageslist = view.findViewById(R.id.messagesList);
        status = view.findViewById(R.id.status);

        SlidrConfig config =
                new SlidrConfig.Builder()
                        .primaryColor(R.color.colorPrimary)
                        .secondaryColor(R.color.colorPrimaryDark)
                        .sensitivity(1f)
                        .scrimColor(Color.BLACK)
                        .scrimStartAlpha(0.8f)
                        .scrimEndAlpha(0f)
                        .velocityThreshold(2400)
                        .distanceThreshold(0.25f)
                        .edge(true)
                        .edgeSize(10)
                        .build();

        Slidr.attach(getActivity(), config);

        ViewGroup rootview = view.findViewById(R.id.rootView);

        /*Initialize EmojiPopup builder*/
        emojiPopup =
                EmojiPopup.Builder.fromRootView(rootview)
                        .setOnEmojiBackspaceClickListener(
                                ignore -> Log.d(TAG, "Clicked on Backspace"))
                        .setOnEmojiClickListener(
                                (ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                        .setOnEmojiPopupShownListener(
                                () -> emojiButton.setImageResource(R.drawable.cmd_keyboard))
                        .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                        .setOnEmojiPopupDismissListener(
                                () -> emojiButton.setImageResource(R.drawable.cmd_emoticon_happy))
                        .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                        .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
                        .build(input_text);

        toolbar.setElevation(3);

        Bundle bundle = this.getArguments();

        listAdapter = new MessageListAdapter(getActivity(), UserConfig.getInstance().getUid());
        layoutManager = new LinearLayoutManager(getActivity());
        mMessageslist.setAdapter(listAdapter);
        mMessageslist.setHasFixedSize(true);
        mMessageslist.setItemAnimator(new DefaultItemAnimator());
        mMessageslist.setLayoutManager(layoutManager);
        layoutManager.setStackFromEnd(true);

        database.collection("users")
                .document(bundle.getString("id"))
                .addSnapshotListener(
                        new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(
                                    DocumentSnapshot value, FirebaseFirestoreException error) {
                                name.setText(value.getString("displayName"));
                                Glide.with(getActivity())
                                        .load(value.getString("userPhoto"))
                                        .skipMemoryCache(true)
                                        .thumbnail(0.1f)
                                        .into(avatar);
                                token = value.getString("fcmToken");
                            }
                        });

        database.collection("messages")
                .document(UserConfig.getInstance().getUid())
                .collection(bundle.getString("id"))
                .orderBy("chatTime")
                .addSnapshotListener(eventListener);

        button_send.setOnClickListener(
                (v) -> {
                    if (!TextUtils.isEmpty(input_text.getText().toString())) {
                        sendTextMessage();
                    }
                });

        Utils.rippleEffects(button_send, "#e0e0e0");
        Utils.rippleEffects(emojiButton, "#e0e0e0");

        UserConfig.getInstance().setStatus(initializeApp.context, status, bundle.getString("id"));

        listAdapter.setOnItemClickListener(itemClickListener);

        emojiButton.setOnClickListener(
                (v) -> {
                    emojiPopup.toggle();
                });

        avatar.setOnClickListener(
                (v) -> {
                    ForaUtil.showMessage(getActivity(), bundle.getString("id"));
                });

        input_text.setOnClickListener(
                (v) -> {
                    if (emojiPopup.isShowing()) {
                        emojiPopup.dismiss();
                    }
                });
    }

    private MessageListAdapter.ItemTouchListener itemClickListener =
            new MessageListAdapter.ItemTouchListener() {
                @Override
                public void onItemClick(int position, View v) {}

                @Override
                public void onItemLongClick(int position, View v) {}
            };

    private void addToSession() {
        Bundle bundle = this.getArguments();
        HashMap<String, Object> map = new HashMap<>();
        map.put(Constants.KEY_SENDER_ID, bundle.getString("id"));
        map.put(Constants.KEY_CHAT_ID, UserConfig.getInstance().getUid());
        map.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map.put("isRequestAccepted", true);
        database.collection("conversations")
                .document(UserConfig.getInstance().getUid())
                .collection("inbox")
                .document(bundle.getString("id"))
                .set(map);
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put(Constants.KEY_SENDER_ID, UserConfig.getInstance().getUid());
        map1.put(Constants.KEY_CHAT_ID, bundle.getString("id"));
        map1.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map1.put("isRequestAccepted", false);
        database.collection("conversations")
                .document(bundle.getString("id"))
                .collection("inbox")
                .document(UserConfig.getInstance().getUid())
                .set(map1);
    }

    private void updateSession() {
        Bundle bundle = this.getArguments();
        HashMap<String, Object> map = new HashMap<>();
        map.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map.put("isRequestAccepted", true);
        database.collection("conversations")
                .document(UserConfig.getInstance().getUid())
                .collection("inbox")
                .document(bundle.getString("id"))
                .update(map);
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map1.put("isRequestAccepted", true);
        database.collection("conversations")
                .document(bundle.getString("id"))
                .collection("inbox")
                .document(UserConfig.getInstance().getUid())
                .update(map1);
        Log.i(ChatActivity.class.getSimpleName(), "Inbox updated.");
    }

    private void sendTextMessage() {
        Bundle bundle = this.getArguments();
        HashMap<String, Object> map = new HashMap<>();
        map.put(Constants.KEY_MESSAGE, String.valueOf(input_text.getText()));
        map.put(Constants.KEY_SENDER_ID, UserConfig.getInstance().getUid());
        map.put(Constants.KEY_CHAT_ID, bundle.getString("id"));
        map.put(Constants.KEY_CALL_TYPE, 13);
        map.put(Constants.KEY_MESSAGE_TYPE, 14);
        map.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map.put(Constants.KEY_IMAGE, "null");
        database.collection("messages")
                .document(UserConfig.getInstance().getUid())
                .collection(bundle.getString("id"))
                .add(map);
        HashMap<String, Object> map2 = new HashMap<>();
        map2.put(Constants.KEY_MESSAGE, String.valueOf(input_text.getText()));
        map2.put(Constants.KEY_SENDER_ID, UserConfig.getInstance().getUid());
        map2.put(Constants.KEY_CHAT_ID, UserConfig.getInstance().getUid());
        map2.put(Constants.KEY_CALL_TYPE, 13);
        map2.put(Constants.KEY_MESSAGE_TYPE, 14);
        map2.put(Constants.KEY_TIMESTAMP, System.currentTimeMillis());
        map2.put(Constants.KEY_IMAGE, "null");
        database.collection("messages")
                .document(bundle.getString("id"))
                .collection(UserConfig.getInstance().getUid())
                .add(map2);
        if (listAdapter.getItemCount() == 0) {
            addToSession();
        } else {
            updateSession();
        }
        sendNotifications(input_text.getText().toString());
        if (emojiPopup.isShowing()) {
            emojiPopup.dismiss();
        }
        input_text.setText("");
    }

    private EventListener<QuerySnapshot> eventListener =
            (value, error) -> {
                if (error != null) return;

                if (value != null) {
                    for (DocumentChange docs : value.getDocumentChanges()) {
                        if (docs.getType() == DocumentChange.Type.ADDED) {
                            Messages messages = new Messages();
                            messages.id = docs.getDocument().getString("id");
                            messages.senderId = docs.getDocument().getString("senderId");
                            messages.chatId = docs.getDocument().getString("chatId");
                            messages.type = docs.getDocument().getDouble("type");
                            messages.callType =
                                    Integer.parseInt(docs.getDocument().get("callType").toString());
                            messages.chatTime = docs.getDocument().getLong("chatTime");
                            messages.imageUrl = docs.getDocument().getString("imageUrl");
                            messages.message = docs.getDocument().getString("message");
                            listMessages.add(messages);
                        }
                    }
                }

                int count = listMessages.size();

                listAdapter.setChatItems(listMessages);
                if (count == 0) {
                    listAdapter.notifyDataSetChanged();
                } else {
                    listAdapter.notifyItemRangeChanged(listMessages.size(), listMessages.size());
                }
                empty.setVisibility(View.GONE);
                mMessageslist.scrollToPosition(listMessages.size() - 1);
            };

    private void sendNotifications(String text) {
        requestQueue = Volley.newRequestQueue(initializeApp.context);
        rootObject = new JSONObject();
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("chat_id", UserConfig.getInstance().getUid());
            dataObject.put("image_url", UserConfig.getInstance().getUserPhoto());
            subObject = new JSONObject();
            subObject.put("title", UserConfig.getInstance().getDisplayName());
            subObject.put("body", text);
            subObject.put("mutable_content", true);
            subObject.put("sound", "tweet");
        } catch (JSONException exception) {
        }
        String url0 = "https://fcm.googleapis.com/fcm/send";

        try {
            rootObject.put("to", token);
            rootObject.put("direct_boot_ok", true);
            rootObject.put("notification", subObject);
            rootObject.put("data", dataObject);
        } catch (JSONException e) {
            Toast.makeText(getActivity(), "Error : " + e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        url0,
                        response -> {
                            /*onResponse*/
                        },
                        error -> {
                            /*onErrorResponse*/
                        }) {
                    @Override
                    public byte[] getBody() {
                        return rootObject.toString().getBytes();
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put(
                                "Authorization",
                                "key=AAAAuxF0tnI:APA91bEFvqPRYyispnlM0KyZVDhXMQxP93N8I8yxpHwrS2Xp26XFjxdwgEJLpmxvQYU0pfP7BHNRtjkStH3HzlsHB_QDrcsAOo42u_1cv4AmVTzC_13oGiiGdAflIKEudSqKaXv2XZ3E");
                        return headers;
                    }
                };

        requestQueue.add(request);
    }
}

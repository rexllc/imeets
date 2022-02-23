package gq.fora.app.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;

import gq.fora.app.R;
import gq.fora.app.adapter.CommentsAdapter;
import gq.fora.app.dao.DAOComments;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.ui.Comments;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class CommentsFragment extends BottomSheetDialogFragment implements SlidrListener {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference commentsRef = database.collection("comments");
    private DocumentReference docs;
    private CollectionReference usersRef = database.collection("users");
    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");

    private ValueEventListener comments_event_listener;

    private RecyclerView rv_comment_list;
    private ImageView close;
    private ImageView options;
    private ImageView button_send;
    private ImageView emojiToggle;
    private EmojiEditText edittext1;
    private EmojiPopup emojiPopup;
    private String TAG = "FORA";
    private EmojiTextView emojiText;
    private SlidrInterface slidrInterface;
    private SlidrListener slidrListener;
    private DAOComments comments;
    private int currentPage = 10;
    private int totalPages;
    private boolean isLoading = true;
    private LinearLayoutManager layoutManager;

    private ArrayList<Comments> commentsList = new ArrayList<>();
    private CommentsAdapter listAdapter;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    CommentsFragment.this.dismiss();
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstance) {
        View view = inflater.inflate(R.layout.activity_comment, parent, false);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.DialogStyle);
        initViews(savedInstance, view);
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle arg0) {
        Dialog dialog = super.onCreateDialog(arg0);
        dialog.setOnShowListener(
                new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                        setupFullHeight(bottomSheetDialog);
                    }
                });

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (slidrInterface == null) {
            slidrInterface =
                    Slidr.replace(
                            getView().findViewById(R.id.content_container),
                            new SlidrConfig.Builder().position(SlidrPosition.VERTICAL).build());
        }
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet =
                (FrameLayout) bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        CoordinatorLayout.LayoutParams layoutParams =
                new CoordinatorLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        bottomSheet.setLayoutParams(layoutParams);
        bottomSheet.setBackgroundColor(android.R.color.transparent);
    }

    private void initViews(Bundle savedInstance, View sheetView) {
        rv_comment_list = (RecyclerView) sheetView.findViewById(R.id.rv_comment_list);
        close = (ImageView) sheetView.findViewById(R.id.close);
        options = (ImageView) sheetView.findViewById(R.id.options);
        button_send = (ImageView) sheetView.findViewById(R.id.button_send);
        emojiToggle = (ImageView) sheetView.findViewById(R.id.emojiToggle);
        edittext1 = (EmojiEditText) sheetView.findViewById(R.id.edittext1);

        Bundle bundle = this.getArguments();
        String key = bundle.getString("comment_id");
        comments = new DAOComments();
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        rv_comment_list.setLayoutManager(layoutManager);
        listAdapter = new CommentsAdapter(getActivity());
        rv_comment_list.setAdapter(listAdapter);
        rv_comment_list.setHasFixedSize(true);

        docs = commentsRef.document(key);

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

        ViewGroup rootview = sheetView.findViewById(R.id.rootLayout);

        /*Initialize EmojiPopup builder*/
        emojiPopup =
                EmojiPopup.Builder.fromRootView(rootview)
                        .setOnEmojiBackspaceClickListener(
                                ignore -> Log.d(TAG, "Clicked on Backspace"))
                        .setOnEmojiClickListener(
                                (ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                        .setOnEmojiPopupShownListener(
                                () -> emojiToggle.setImageResource(R.drawable.cmd_keyboard))
                        .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                        .setOnEmojiPopupDismissListener(
                                () -> emojiToggle.setImageResource(R.drawable.cmd_emoticon_happy))
                        .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                        .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
                        .build(edittext1);

        commentsRef
                .whereEqualTo("storyId", key)
                .limitToLast(20)
                .orderBy("userId")
                .addSnapshotListener(eventListener);

        Utils.rippleEffects(close, "#e0e0e0");
        Utils.rippleEffects(options, "#e0e0e0");
        Utils.rippleEffects(button_send, "#e0e0e0");
        Utils.rippleEffects(emojiToggle, "#e0e0e0");

        close.setOnClickListener(
                (View v) -> {
                    dismiss();
                });

        options.setOnClickListener((View v) -> {});

        button_send.setOnClickListener(
                (View v) -> {
                    if (!edittext1.getText().toString().isEmpty()) {
                        String commentId = "CMNT_" + System.currentTimeMillis() + "CYZ";
                        HashMap<String, Object> data = new HashMap<>();
                        data.put("userId", UserConfig.getInstance().getUid());
                        data.put("text", String.valueOf(edittext1.getText()));
                        data.put("storyId", key);
                        commentsRef.document(commentId).set(data);
                    }
                });

        emojiToggle.setOnClickListener(
                (View v) -> {
                    emojiPopup.toggle();
                });

        rv_comment_list.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int totalItemCount = layoutManager.getItemCount();
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                        if (!isLoading && totalItemCount <= (lastVisibleItem + 1)) {
                            if (currentPage < totalPages) {
                                isLoading = true;
                            }
                        }
                    }
                });
    }

    @Override
    public int getTheme() {
        return super.getTheme();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        getDialog().onBackPressed();
    }

    @Override
    public void onSlideStateChanged(int state) {}

    @Override
    public void onSlideChange(float arg0) {}

    @Override
    public void onSlideOpened() {}

    @Override
    public boolean onSlideClosed() {
        getDialog().dismiss();
        return true;
    }

    @Override
    public void onCancel(@NonNull DialogInterface arg0) {
        super.onCancel(arg0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface arg0) {
        super.onDismiss(arg0);
    }

    private EventListener<QuerySnapshot> eventListener =
            (value, error) -> {
                if (error != null) return;

                if (value != null) {
                    for (DocumentChange docs : value.getDocumentChanges()) {
                        Comments valueList = new Comments();
                        valueList.userId = docs.getDocument().getString("userId");
                        valueList.text = docs.getDocument().getString("text");
                        valueList.storyId = docs.getDocument().getString("storyId");
                        commentsList.add(valueList);
                    }
                }

                totalPages = commentsList.size();
                listAdapter.setItems(commentsList);
                listAdapter.notifyDataSetChanged();
                isLoading = false;
            };
}

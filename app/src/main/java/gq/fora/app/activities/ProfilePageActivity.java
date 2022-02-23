package gq.fora.app.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import de.hdodenhof.circleimageview.CircleImageView;

import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.adapter.SharedMediaListAdapter;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.friends.Friends;
import gq.fora.app.models.friends.add.AddFriends;
import gq.fora.app.models.friends.blocklist.BlockFriends;
import gq.fora.app.models.friends.blocklist.BlockList;
import gq.fora.app.utils.Utils;
import gq.fora.app.widgets.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;

public class ProfilePageActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference messages = firebase.getReference("messages");
    private DatabaseReference friends = firebase.getReference("friends");
    private DatabaseReference databaseReference = firebase.getReference("friends");
    private DatabaseReference blocklist = firebase.getReference("blocklist");

    private LinearLayout toolbar;
    private ImageView back, options;
    private CircleImageView avatar;
    private ImageView verification_check_badge;
    private TextView display_name, shared_media, empty, friends_list_count, username;
    private MaterialButton add_friend_button, message_button;
    private RecyclerView shared_media_list;
    private ArrayList<HashMap<String, Object>> chat = new ArrayList<>();
    private SharedMediaListAdapter sharedAdapter;
    private String status = "none";
    private String blockStatus = "none";
    private String title;
    private String message;
    private String positiveButton;
    private String negativeButton;
    private ValueEventListener friends_child_listener_1;
    private ValueEventListener friends_child_listener_2;
    private ValueEventListener blocklist_child_listener;
    private ValueEventListener blocklist_child_listener2;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Window window;
    private View view;
    private WindowInsetsControllerCompat insetsController;
    private WindowInsetsCompat insets;
    private ArrayList<Friends> friendList = new ArrayList<>();

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        // keyboard is opened
                        insetsController.hide(WindowInsetsCompat.Type.ime());
                    } else {
                        // keyboard is closed
                    }
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_profile_page, parent, false);
        initializeViews(savedInstanceState, view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    private void initializeViews(Bundle savedInstanceState, View view) {
        toolbar = view.findViewById(R.id.toolbar);
        back = view.findViewById(R.id.back);
        options = view.findViewById(R.id.options);
        avatar = view.findViewById(R.id.avatar);
        display_name = view.findViewById(R.id.display_name);
        shared_media = view.findViewById(R.id.shared_media);
        add_friend_button = view.findViewById(R.id.add_friend_button);
        message_button = view.findViewById(R.id.message_button);
        shared_media_list = view.findViewById(R.id.shared_media_list);
        empty = view.findViewById(R.id.empty);
        friends_list_count = view.findViewById(R.id.friends_list_count);
        username = view.findViewById(R.id.username);
        verification_check_badge = view.findViewById(R.id.verification_check_badge);

        window = getActivity().getWindow();
        view = window.getDecorView();
        insetsController = new WindowInsetsControllerCompat(window, view);
        insets = ViewCompat.getRootWindowInsets(view);
        Bundle bundle1 = this.getArguments();

        CollectionReference ref = db.collection("friends");

        DocumentReference docRef = ref.document(UserConfig.getInstance().getUid());

        docRef.collection(bundle1.getString("userId")).addSnapshotListener(snapShotListener);

        blocklist_child_listener =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot arg0) {
                        BlockList block = arg0.getValue(BlockList.class);
                        if (block != null) {
                            if (block.getUserId().equals(bundle1.getString("userId"))) {
                                if (block.getStatus().equals("blocked")) {
                                    blockStatus = "blocked";
                                } else {
                                    if (block.getStatus().equals("unblocked")) {
                                        blockStatus = "unblocked";
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError arg0) {}
                };

        blocklist_child_listener2 =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot arg0) {
                        BlockList block = arg0.getValue(BlockList.class);
                        if (block != null) {
                            if (block.getStatus().equals("blocked")) {
                                MaterialDialog mDialog =
                                        new MaterialDialog.Builder(getActivity())
                                                .setTitle("Block!")
                                                .setMessage("You have been blocked by this user.")
                                                .setCancelable(false)
                                                .setPositiveButton(
                                                        "Okay",
                                                        new MaterialDialog.OnClickListener() {
                                                            @Override
                                                            public void onClick(
                                                                    DialogInterface dialogInterface,
                                                                    int which) {
                                                                // Cancel Operation
                                                                getActivity()
                                                                        .getSupportFragmentManager()
                                                                        .popBackStack();
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                .setNegativeButton(
                                                        "Learn More",
                                                        new MaterialDialog.OnClickListener() {
                                                            @Override
                                                            public void onClick(
                                                                    DialogInterface dialogInterface,
                                                                    int which) {
                                                                getActivity()
                                                                        .getSupportFragmentManager()
                                                                        .popBackStack();
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                .build();

                                // Show Dialog
                                mDialog.show();
                            } else {
                                if (block.getStatus().equals("unblocked")) {}
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError arg0) {}
                };

        message_button.setVisibility(View.VISIBLE);

        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        // keyboard is opened
                        insetsController.hide(WindowInsetsCompat.Type.ime());
                    } else {
                        // keyboard is closed
                    }
                });

        options.setOnClickListener(
                (View v) -> {
                    openMenu();
                });

        add_friend_button.setOnClickListener(
                (View v) -> {
                    if (status.equals("none")) {
                        AddFriends.getInstance()
                                .Add(
                                        UserConfig.getInstance().getUid(),
                                        bundle1.getString("userId"));
                        FirebaseMessaging.getInstance()
                                .subscribeToTopic(bundle1.getString("userId"));
                        add_friend_button.setText("Pending");
                    } else {
                        if (status.equals("confirm")) {
                            AddFriends.getInstance()
                                    .confirmFriends(
                                            UserConfig.getInstance().getUid(),
                                            bundle1.getString("userId"));
                            FirebaseMessaging.getInstance()
                                    .subscribeToTopic(bundle1.getString("userId"));
                            add_friend_button.setText("Friends");
                            Snackbar.make(
                                            add_friend_button,
                                            "You're now friends.",
                                            Snackbar.LENGTH_SHORT)
                                    .show();
                        } else {
                            if (status.equals("friends")) {
                                AddFriends.getInstance()
                                        .Unfriend(
                                                UserConfig.getInstance().getUid(),
                                                bundle1.getString("userId"));
                                FirebaseMessaging.getInstance()
                                        .unsubscribeFromTopic(bundle1.getString("userId"));
                                add_friend_button.setText("Add Friend");
                            } else {
                                if (status.equals("pending")) {
                                    MaterialDialog mDialog =
                                            new MaterialDialog.Builder(getActivity())
                                                    .setTitle("Cancel Friend Request?")
                                                    .setMessage(
                                                            "Do you really want to cancel your"
                                                                    + " friend request?")
                                                    .setCancelable(false)
                                                    .setPositiveButton(
                                                            "Cancel",
                                                            new MaterialDialog.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface
                                                                                dialogInterface,
                                                                        int which) {
                                                                    // Cancel Operation
                                                                    AddFriends.getInstance()
                                                                            .removeRequest(
                                                                                    UserConfig
                                                                                            .getInstance()
                                                                                            .getUid(),
                                                                                    bundle1
                                                                                            .getString(
                                                                                                    "userId"));
                                                                    FirebaseMessaging.getInstance()
                                                                            .unsubscribeFromTopic(
                                                                                    bundle1
                                                                                            .getString(
                                                                                                    "userId"));
                                                                    add_friend_button.setText(
                                                                            "Add Friend");
                                                                    message_button.setVisibility(
                                                                            View.GONE);
                                                                    Snackbar.make(
                                                                                    add_friend_button,
                                                                                    "Friend request"
                                                                                        + " cancelled.",
                                                                                    Snackbar
                                                                                            .LENGTH_SHORT)
                                                                            .show();
                                                                    dialogInterface.dismiss();
                                                                }
                                                            })
                                                    .setNegativeButton(
                                                            "Abort",
                                                            new MaterialDialog.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface
                                                                                dialogInterface,
                                                                        int which) {
                                                                    dialogInterface.dismiss();
                                                                }
                                                            })
                                                    .build();

                                    // Show Dialog
                                    mDialog.show();
                                }
                            }
                        }
                    }

                    blocklist
                            .child(UserConfig.getInstance().getUid())
                            .child(bundle1.getString("userId"))
                            .addListenerForSingleValueEvent(blocklist_child_listener);
                    blocklist
                            .child(bundle1.getString("userId"))
                            .child(UserConfig.getInstance().getUid())
                            .addListenerForSingleValueEvent(blocklist_child_listener2);
                });

        message_button.setOnClickListener(
                (View v) -> {
                    Fragment chatFragment = new ChatActivity();
                    Bundle bundle = new Bundle();
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                            .add(android.R.id.content, chatFragment)
                            .addToBackStack(null)
                            .commit();
                    bundle.putString("id", bundle1.getString("userId"));
                    chatFragment.setArguments(bundle);
                });

        blocklist
                .child(UserConfig.getInstance().getUid())
                .child(bundle1.getString("userId"))
                .addListenerForSingleValueEvent(blocklist_child_listener);
        blocklist
                .child(bundle1.getString("userId"))
                .child(UserConfig.getInstance().getUid())
                .addListenerForSingleValueEvent(blocklist_child_listener2);

        databaseReference
                .child(bundle1.getString("userId"))
                .orderByChild("isFriends")
                .equalTo(true)
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    Friends friend = data.getValue(Friends.class);
                                    friendList.add(friend);
                                }
                                friends_list_count.setText(friendList.size() + " friends");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
    }

    private void initializeLogic() {
        toolbar.setElevation(3);
        Utils.rippleEffects(back, "#e0e0e0");
        Utils.rippleEffects(options, "#e0e0e0");
        getUserInfo();
        int spanCount = 2;
        int spacing = 5;
        boolean includeEdge = false;
        shared_media_list.addItemDecoration(
                new GridSpacingItemDecoration(spanCount, spacing, includeEdge));

        if (chat.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            shared_media_list.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            shared_media_list.setVisibility(View.VISIBLE);
        }

        if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
            // keyboard is opened
            insetsController.hide(WindowInsetsCompat.Type.ime());
        } else {
            // keyboard is closed
        }
    }

    private void getUserInfo() {
        Bundle bundle = this.getArguments();
        database.collection("users")
                .document(bundle.getString("userId"))
                .addSnapshotListener(
                        new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(
                                    DocumentSnapshot value, FirebaseFirestoreException error) {
                                display_name.setText(value.getString("displayName"));
                                Glide.with(getActivity())
                                        .load(value.getString("userPhoto"))
                                        .skipMemoryCache(true)
                                        .thumbnail(0.1f)
                                        .into(avatar);
                                username.setText(value.getString("username"));
                                if (value.getBoolean("isVerified")) {
                                    verification_check_badge.setVisibility(View.VISIBLE);
                                } else {
                                    verification_check_badge.setVisibility(View.GONE);
                                }
                            }
                        });
        messages.child(bundle.getString("userId"))
                .child(UserConfig.getInstance().getUid())
                .orderByChild("imageUrl")
                .addChildEventListener(
                        new ChildEventListener() {

                            @Override
                            public void onChildAdded(DataSnapshot arg0, String arg1) {
                                GenericTypeIndicator<HashMap<String, Object>> _ind =
                                        new GenericTypeIndicator<HashMap<String, Object>>() {};
                                final String _childKey = arg0.getKey();
                                final HashMap<String, Object> _childValue = arg0.getValue(_ind);

                                if (_childValue.get("type").toString().equals("20")) {
                                    chat.add(_childValue);
                                    sharedAdapter = new SharedMediaListAdapter(chat);
                                    shared_media_list.setAdapter(sharedAdapter);
                                    shared_media_list.setHasFixedSize(true);
                                    shared_media_list.setLayoutManager(
                                            new GridLayoutManager(getActivity(), 2));
                                    sharedAdapter.notifyDataSetChanged();
                                    empty.setVisibility(View.GONE);
                                    shared_media_list.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot arg0, String arg1) {
                                GenericTypeIndicator<HashMap<String, Object>> _ind =
                                        new GenericTypeIndicator<HashMap<String, Object>>() {};
                                final String _childKey = arg0.getKey();
                                final HashMap<String, Object> _childValue = arg0.getValue(_ind);

                                if (_childValue.get("type").toString().equals("20")) {
                                    chat.add(_childValue);
                                    sharedAdapter = new SharedMediaListAdapter(chat);
                                    shared_media_list.setAdapter(sharedAdapter);
                                    shared_media_list.setHasFixedSize(true);
                                    shared_media_list.setLayoutManager(
                                            new GridLayoutManager(getActivity(), 2));
                                    sharedAdapter.notifyDataSetChanged();
                                    empty.setVisibility(View.GONE);
                                    shared_media_list.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onChildRemoved(DataSnapshot arg0) {}

                            @Override
                            public void onChildMoved(DataSnapshot arg0, String arg1) {}

                            @Override
                            public void onCancelled(DatabaseError arg0) {}
                        });
    }

    public void openMenu() {
        BottomSheetDialog bs = new BottomSheetDialog(getActivity());
        View sheetView =
                LayoutInflater.from(getActivity()).inflate(R.layout.bottom_sheet_menu, null);
        bs.setContentView(sheetView);

        final LinearLayout remove_friend =
                (LinearLayout) sheetView.findViewById(R.id.remove_friend);
        final LinearLayout block_friend = (LinearLayout) sheetView.findViewById(R.id.block_friend);
        final LinearLayout report_friend =
                (LinearLayout) sheetView.findViewById(R.id.report_friend);
        final LinearLayout line = (LinearLayout) sheetView.findViewById(R.id.line);
        final ImageView unfriend = (ImageView) sheetView.findViewById(R.id.unfriend);
        final ImageView report = (ImageView) sheetView.findViewById(R.id.block);
        final ImageView block = (ImageView) sheetView.findViewById(R.id.report);
        final TextView block_text = (TextView) sheetView.findViewById(R.id.block_text);

        Utils.rippleRoundStroke(remove_friend, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(block_friend, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(report_friend, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");

        unfriend.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        block.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        report.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        line.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(60, 0xFFE0E0E0));

        if (status.equals("friends")) {
            remove_friend.setVisibility(View.VISIBLE);
        } else {
            remove_friend.setVisibility(View.GONE);
        }

        remove_friend.setOnClickListener(
                (View v) -> {
                    bs.dismiss();
                });

        if (blockStatus.equals("none")) {
            title = "Block?";
            message = "Do you really want to block this user?";
            positiveButton = "Block";
            negativeButton = "Cancel";
            block_text.setText("Block");
        } else {
            if (blockStatus.equals("blocked")) {
                title = "Unlock?";
                message = "Do you really want to unblock this user?";
                positiveButton = "Unblock";
                negativeButton = "Cancel";
                block_text.setText("Unblock");
            } else {
                if (blockStatus.equals("unblocked")) {
                    title = "Block?";
                    message = "Do you really want to block this user?";
                    positiveButton = "Block";
                    negativeButton = "Cancel";
                    block_text.setText("Block");
                }
            }
        }

        block_friend.setOnClickListener(
                (View v) -> {
                    BottomSheetMaterialDialog mBottomSheetDialog =
                            new BottomSheetMaterialDialog.Builder(getActivity())
                                    .setTitle(title)
                                    .setMessage(message)
                                    .setCancelable(false)
                                    .setPositiveButton(
                                            positiveButton,
                                            new BottomSheetMaterialDialog.OnClickListener() {
                                                @Override
                                                public void onClick(
                                                        DialogInterface dialogInterface,
                                                        int which) {
                                                    blockUser();
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                    .setNegativeButton(
                                            negativeButton,
                                            new BottomSheetMaterialDialog.OnClickListener() {
                                                @Override
                                                public void onClick(
                                                        DialogInterface dialogInterface,
                                                        int which) {
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                    .build();

                    // Show Dialog
                    mBottomSheetDialog.show();
                    bs.dismiss();
                });

        report_friend.setOnClickListener(
                (View v) -> {
                    bs.dismiss();
                });

        bs.show();
    }

    public void blockUser() {
        Bundle bundle2 = this.getArguments();
        if (blockStatus.equals("none")) {
            BlockFriends.getInstance()
                    .BlockUser(UserConfig.getInstance().getUid(), bundle2.getString("userId"));
            Snackbar.make(add_friend_button, "User has been blocked.", Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            if (blockStatus.equals("blocked")) {
                BlockFriends.getInstance()
                        .UnblockUser(
                                UserConfig.getInstance().getUid(), bundle2.getString("userId"));
                Snackbar.make(add_friend_button, "User has been unblocked.", Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                if (blockStatus.equals("unblocked")) {
                    BlockFriends.getInstance()
                            .BlockUser(
                                    UserConfig.getInstance().getUid(), bundle2.getString("userId"));
                    Snackbar.make(
                                    add_friend_button,
                                    "User hass been blocked.",
                                    Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        }
        blocklist
                .child(UserConfig.getInstance().getUid())
                .child(bundle2.getString("userId"))
                .addListenerForSingleValueEvent(blocklist_child_listener);
    }

    private EventListener<QuerySnapshot> snapShotListener =
            (value, error) -> {
                if (error != null) {
                    return;
                }

                if (value != null) {
                    for (DocumentChange docs : value.getDocumentChanges()) {
                        if (docs.getDocument().getString("status").equals("pending")) {
                            add_friend_button.setText("Pending");
                            status = "pending";
                            message_button.setVisibility(View.GONE);
                        } else if (docs.getDocument().getString("status").equals("friends")) {
                            add_friend_button.setText("Friends");
                            status = "friends";
                            message_button.setVisibility(View.VISIBLE);
                        }
                    }
                }
            };
}

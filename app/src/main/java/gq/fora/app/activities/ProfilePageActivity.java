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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
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
import gq.fora.app.utils.Utils;
import gq.fora.app.widgets.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfilePageActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();

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

    private Window window;
    private View view;
    private WindowInsetsControllerCompat insetsController;
    private WindowInsetsCompat insets;
    private List<Friends> friendList = new ArrayList<>();

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

        database.collection("friends")
                .document(UserConfig.getInstance().getUid())
                .collection("list")
                .document(bundle1.getString("userId"))
                .addSnapshotListener(snapShotListener);

        database.collection("friends")
                .document(bundle1.getString("userId"))
                .collection("list")
                .addSnapshotListener(
                        (snapshot, exception) -> {
                            if (snapshot != null) {
                                for (DocumentChange docs : snapshot.getDocumentChanges()) {
                                    if (docs.getDocument().getBoolean("isFriends") == false) {
                                        continue;
                                    }
                                    List<Friends> listFriends = new ArrayList<>();
                                    Friends friends = new Friends();
                                    friends.isFriends = docs.getDocument().getBoolean("isFriends");
                                    listFriends.add(friends);
                                    friends_list_count.setText(listFriends.size() + " friends");
                                }
                            }
                        });

        database.collection("block")
                .document(UserConfig.getInstance().getUid())
                .collection("users")
                .document(bundle1.getString("userId"))
                .addSnapshotListener(
                        (value, exception) -> {
                            if (exception != null) return;

                            if (value != null) {
                                if (value.contains("userId") && value.contains("status")) {
                                    if (value.getString("userId")
                                            .equals(bundle1.getString("userId"))) {
                                        if (value.getString("status").equals("blocked")) {
                                            blockStatus = "blocked";
                                        } else {
                                            blockStatus = "unblocked";
                                        }
                                    }
                                }
                            }
                        });

        database.collection("block")
                .document(bundle1.getString("userId"))
                .collection("users")
                .document(UserConfig.getInstance().getUid())
                .addSnapshotListener(
                        (value, exception) -> {
                            if (exception != null) return;

                            if (value != null) {
                                if (value.contains("status")) {
                                    if (value.getString("status").equals("blocked")) {
                                        showDialog();
                                    }
                                }
                            }
                            Utils utils = new Utils(getActivity());
                            utils.copyText(value.toString());
                        });

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

        message_button.setVisibility(View.GONE);
    }

    private void getUserInfo() {
        Bundle bundle = this.getArguments();
        database.collection("users")
                .document(bundle.getString("userId"))
                .get()
                .addOnCompleteListener(
                        (task) -> {
                            display_name.setText(task.getResult().getString("displayName"));
                            Glide.with(avatar.getContext())
                                    .load(task.getResult().getString("userPhoto"))
                                    .skipMemoryCache(true)
                                    .thumbnail(0.1f)
                                    .into(avatar);
                            username.setText(task.getResult().getString("username"));
                            if (task.getResult().getBoolean("isVerified")) {
                                verification_check_badge.setVisibility(View.VISIBLE);
                            } else {
                                verification_check_badge.setVisibility(View.GONE);
                            }
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
    }

    private EventListener<DocumentSnapshot> snapShotListener =
            (value, error) -> {
                if (error != null) {
                    return;
                }

                if (value != null) {
                    if (value.contains("status")) {
                        if (value.contains("userId") && value.contains("status")) {
                            if (value.getString("userId")
                                    .equals(UserConfig.getInstance().getUid())) {
                                if (value.getString("status").equals("pending")) {
                                    add_friend_button.setText("Accept");
                                    status = "confirm";

                                } else if (value.getString("status").equals("friends")) {
                                    add_friend_button.setText("Friends");
                                    status = "friends";
                                    message_button.setVisibility(View.VISIBLE);
                                }
                            } else {
                                if (value.getString("status").equals("pending")) {
                                    status = "pending";
                                    add_friend_button.setText("Pending");
                                    message_button.setVisibility(View.GONE);
                                } else {
                                    add_friend_button.setText("Friends");
                                    status = "friends";
                                    message_button.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                }
            };

    private void showDialog() {
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
                                            DialogInterface dialogInterface, int which) {
                                        // Cancel Operation
                                        getActivity().getSupportFragmentManager().popBackStack();
                                        dialogInterface.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                "Learn More",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        getActivity().getSupportFragmentManager().popBackStack();
                                        dialogInterface.dismiss();
                                    }
                                })
                        .build();

        // Show Dialog
        mDialog.show();
    }
}

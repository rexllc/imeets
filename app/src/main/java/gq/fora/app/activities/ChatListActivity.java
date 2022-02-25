package gq.fora.app.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.*;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.fragments.ChatsFragment;
import gq.fora.app.fragments.PeopleFragment;
import gq.fora.app.initializeApp;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.notify.Notify;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.Utils;

import kotlin.Unit;

import me.ibrahimsn.lib.SmoothBottomBar;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListActivity extends BaseFragment {

    private CircleImageView avatar;
    private LinearLayout toolbar;
    private ConstraintLayout add_story;
    private ImageView contacts, search, search_icon;
    private CoordinatorLayout coordinator;
    private TextView title, connection_status;
    private String TAG = "FORA";
    private FloatingActionButton fab;
    private SharedPreferences data;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private ChildEventListener _users_child_listener;
    private Calendar cal = Calendar.getInstance();
    private User userInfo;
    private ExecutorService executor;
    private Handler handler;
    private TimerTask timer;
    private Timer _timer = new Timer();

    private String mAccountType = "gq.fora.app";
    private double position = 0;
    private SharedPreferences sharedPreferences;
    private SmoothBottomBar bottomBar;
    private Fragment chatsFragment = new ChatsFragment();
    private Fragment peopleFragment = new PeopleFragment();
    private FragmentManager fm;

    private static final int PICKER_REQUEST_IMAGE = 100;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().finishAffinity();
                }
            };

    @NonNull
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccountManager mAccountMgr = AccountManager.get(getContext());
        mAccountMgr.addOnAccountsUpdatedListener(new AccountsUpdateListener(), null, false);
    }

    private ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri uri = result.getData().getData();
                            // Use the uri to load the image
                            executor.execute(
                                    () -> {
                                        // Background work here
                                        handler.post(
                                                () -> {
                                                    // UI Thread work here

                                                });
                                    });
                        } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
                            // Use ImagePicker.getError(result.getData()) to show an error
                        }
                    });

    private final ActivityResultLauncher<String> requestWriteSettingsPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    result -> {
                        if (result) {
                            /*Start Chat*/
                        } else {
                            /*Permission not granted*/
                            Snackbar.make(
                                            getView(),
                                            "Fora will not works until you granted the required"
                                                    + " permissions.",
                                            Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    });

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_chat_list, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        getUserInfo();
        users.keepSynced(true);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        if (!Settings.System.canWrite(getActivity())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
            startActivity(intent);
        }
        return _view;
    }

    @Override
    public void onStart() {
        super.onStart();
        checkInternet();
        checkTheme();
        if (!peopleFragment.isAdded() && !chatsFragment.isAdded()) {
            fm.beginTransaction()
                    .add(R.id.main_container, peopleFragment, "2")
                    .hide(peopleFragment)
                    .commit();
            fm.beginTransaction().add(R.id.main_container, chatsFragment, "1").commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle arg0) {
        super.onSaveInstanceState(arg0);
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {

        fab = view.findViewById(R.id.fab);
        title = view.findViewById(R.id.title);
        toolbar = view.findViewById(R.id.toolbar);
        coordinator = view.findViewById(R.id.coordinator);
        avatar = view.findViewById(R.id.avatar);
        connection_status = view.findViewById(R.id.connection_status);
        search = view.findViewById(R.id.search);
        contacts = view.findViewById(R.id.contacts);
        bottomBar = view.findViewById(R.id.bottomBar);
		
		fab.hide();

        fm = getActivity().getSupportFragmentManager();

        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        data = getActivity().getSharedPreferences("token", Activity.MODE_PRIVATE);

        String path = FileUtils.getPackageDataDir(getContext()) + "/user/accounts.json";

        if (!FileUtils.isExistFile(path)) {
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new AuthActivity())
                    .addToBackStack(null)
                    .commit();
        }

        fab.setOnClickListener((View v) -> {});

        avatar.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new AccountActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });
    }

    public void initializeLogic() {
        toolbar.setElevation(3);
        bottomBar.setElevation(8);
        PopupMenu popupMenu = new PopupMenu(getActivity(), null);
        popupMenu.inflate(R.menu.fora_tab_menu);
        Menu menu = popupMenu.getMenu();
        bottomBar.setOnItemSelected(
                _position -> {
                    if (_position == 0) {
                        fm.beginTransaction()
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .hide(peopleFragment)
                                .show(chatsFragment)
                                .commit();

                        title.setText("Chats");
                        toolbar.setElevation((float) 3);
                    } else if (_position == 1) {
                        fm.beginTransaction()
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                                .hide(chatsFragment)
                                .show(peopleFragment)
                                .commit();

                        title.setText("People");
                        toolbar.setElevation((float) 0);
                    }
                    return Unit.INSTANCE;
                });
    }

    public void getUserInfo() {
        if (UserConfig.getInstance().isLogin()) {
            Glide.with(getActivity()).load(UserConfig.getInstance().getUserPhoto()).into(avatar);
        }
    }

    private class AccountsUpdateListener implements OnAccountsUpdateListener {
        @Override
        public void onAccountsUpdated(Account[] accounts) {
            Account newAccount = null;
            for (final Account account : accounts) {
                if (account.type.equals(mAccountType)) {
                    newAccount = account;
                }
            }

            if (newAccount == null) {
                // account removed, now you can handle your private data and remove anything you
                // want here
                FirebaseAuth.getInstance().signOut();
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, new AuthActivity())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private void openChannelSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

        // for Android 5-7
        intent.putExtra("app_package", getActivity().getPackageName());
        intent.putExtra("app_uid", getActivity().getApplicationInfo().uid);

        // for Android 8 and above
        intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());

        startActivity(intent);
    }

    public void showNotification(String title, String body, int progress, int max) {
        try {
            Notify.create(initializeApp.context)
                    .setTitle(title)
                    .setContent(body)
                    .setProgress(max, progress, false)
                    .setChannelId("1")
                    .setChannelName("Upload Task")
                    .setChannelDescription("Notification for uploading.")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setImportance(Notify.Importance.HIGH)
                    .setAutoCancel(true)
                    .enableVibration(true)
                    .setId(100)
                    .setColor(R.color.primary)
                    .show();
        } catch (Exception ex) {
            ex.printStackTrace();
            FileUtils.writeFile(
                    ex.toString(),
                    FileUtils.getPublicDir(Environment.DIRECTORY_DOCUMENTS)
                            + "/"
                            + "logs-"
                            + System.currentTimeMillis()
                            + ".txt");
        }
    }

    public void showDialog(String title, String message) {
        MaterialDialog mDialog =
                new MaterialDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(
                                "Okay",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        // Delete Operation
                                        dialogInterface.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                "Report",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .build();

        // Show Dialog
        mDialog.show();
    }

    public void checkTheme() {
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            darkMode(true);
        } else {
            darkMode(false);
        }
    }

    public void darkMode(boolean isNight) {
        if (isNight) {
            toolbar.setBackgroundColor(0xFF212121);
            title.setTextColor(0xFFFFFFFF);
            coordinator.setBackgroundColor(0xFF212121);
            Utils.rippleRoundStroke(search, "#424242", "#eeeeee", 360, 0, "#212121");
            Utils.rippleRoundStroke(contacts, "#424242", "#eeeeee", 360, 0, "#212121");
            bottomBar.setItemIconTint(Color.parseColor("#3f61dc"));
            bottomBar.setBarBackgroundColor(Color.parseColor("#212121"));
            bottomBar.setBarIndicatorColor(Color.parseColor("#424242"));
            bottomBar.setItemTextColor(Color.parseColor("#f43159"));
            search.setColorFilter(0xFFFFFFFF);
            contacts.setColorFilter(0xFFFFFFFF);
            bottomBar.setBackgroundColor(0xFF212121);
        } else {
            toolbar.setBackgroundColor(0xFFFFFFFF);
            title.setTextColor(0xFF000000);
            coordinator.setBackgroundColor(0xFFFFFFFF);
            Utils.rippleRoundStroke(search, "#eeeeee", "#eeeeee", 360, 0, "#212121");
            Utils.rippleRoundStroke(contacts, "#eeeeee", "#eeeeee", 360, 0, "#212121");
            bottomBar.setItemIconTint(Color.parseColor("#f43159"));
            bottomBar.setBarBackgroundColor(Color.parseColor("#ffffff"));
            bottomBar.setBarIndicatorColor(Color.parseColor("#f5f5f5"));
            bottomBar.setItemTextColor(Color.parseColor("#f43159"));
            search.setColorFilter(0xFF000000);
            contacts.setColorFilter(0xFF000000);
            bottomBar.setBackgroundColor(0xFFFFFFFF);
        }
    }

    private void checkInternet() {
        if (ForaUtil.isConnected(getActivity())) {
            connection_status.setVisibility(View.GONE);
        } else {
            connection_status.setVisibility(View.VISIBLE);
        }
    }

    private void openMenu() {}
}

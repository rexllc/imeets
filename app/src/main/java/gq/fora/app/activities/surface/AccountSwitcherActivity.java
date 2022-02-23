package gq.fora.app.activities.surface;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;

import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;
import dev.shreyaspatil.MaterialDialog.model.TextAlignment;

import gq.fora.app.R;
import gq.fora.app.activities.AuthActivity;
import gq.fora.app.activities.SplashActivity;
import gq.fora.app.adapter.AccountListAdapter;
import gq.fora.app.listener.onclick.RecyclerItemClickListener;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.PreferencesManager;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class AccountSwitcherActivity extends BaseFragment {

    private LinearLayout toolbar;
    private ImageView back;
    private RecyclerView account_list;
    private MaterialButton add_account_button;
    private ArrayList<HashMap<String, Object>> accountList = new ArrayList<>();
    private LinearLayoutManager layoutAdapter;
    private AccountListAdapter adapter;
    private double position = 0;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private PreferencesManager preferencesManager;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_account_switcher, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        toolbar = view.findViewById(R.id.toolbar);
        back = view.findViewById(R.id.back);
        account_list = view.findViewById(R.id.account_list);
        add_account_button = view.findViewById(R.id.add_account_button);

        preferencesManager = new PreferencesManager(getActivity());

        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        String path = FileUtils.getPackageDataDir(getContext()) + "/user/accounts.json";
        String path2 = FileUtils.getPackageDataDir(getContext()) + "/user";

        if (FileUtils.isExistFile(path)) {
            accountList =
                    new Gson()
                            .fromJson(
                                    FileUtils.readFile(path),
                                    new TypeToken<
                                            ArrayList<HashMap<String, Object>>>() {}.getType());

            ForaUtil.sortListMap(accountList, "last_signed", false, false);

            layoutAdapter = new LinearLayoutManager(getActivity());
            adapter = new AccountListAdapter(getActivity());
            account_list.setAdapter(adapter);
            adapter.setItems(accountList);
            account_list.setHasFixedSize(true);
            account_list.setLayoutManager(layoutAdapter);
            adapter.notifyItemInserted(0);
        }

        add_account_button.setOnClickListener(
                (View v) -> {
                    if (accountList.size() > 4) {
                        MaterialDialog mDialog =
                                new MaterialDialog.Builder(getActivity())
                                        .setTitle("Account Limit Reached")
                                        .setMessage(
                                                "You have been reach the available accounts that"
                                                        + " can be added to this device.",
                                                TextAlignment.START)
                                        .setCancelable(false)
                                        .setPositiveButton(
                                                "Okay",
                                                new MaterialDialog.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialogInterface,
                                                            int which) {
                                                        // Operation
                                                    }
                                                })
                                        .build();

                        // Show Dialog
                        mDialog.show();
                    } else {
                        getActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .add(android.R.id.content, new AuthActivity())
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .addToBackStack(null)
                                .commit();
                    }
                });

        account_list.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getActivity(),
                        account_list,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                // do whatever
                                if (!accountList
                                        .get(position)
                                        .get("userId")
                                        .toString()
                                        .equals(UserConfig.getInstance().getUid())) {
                                    MaterialDialog mDialog =
                                            new MaterialDialog.Builder(getActivity())
                                                    .setTitle("Switch User?")
                                                    .setMessage(
                                                            "Are you sure you want to switch as"
                                                                    + " this user?")
                                                    .setCancelable(false)
                                                    .setPositiveButton(
                                                            "Switch",
                                                            (dialogInterface, which) -> {
                                                                users.child(
                                                                                accountList
                                                                                        .get(
                                                                                                position)
                                                                                        .get(
                                                                                                "userId")
                                                                                        .toString())
                                                                        .addListenerForSingleValueEvent(
                                                                                new ValueEventListener() {
                                                                                    @Override
                                                                                    public void
                                                                                            onDataChange(
                                                                                                    DataSnapshot
                                                                                                            arg0) {
                                                                                        User user =
                                                                                                arg0
                                                                                                        .getValue(
                                                                                                                User
                                                                                                                        .class);
                                                                                        if (user
                                                                                                != null) {
                                                                                            showLoadingDialog(
                                                                                                    user
                                                                                                            . email,
                                                                                                    accountList
                                                                                                            .get(
                                                                                                                    position)
                                                                                                            .get(
                                                                                                                    "password")
                                                                                                            .toString());
                                                                                        }
                                                                                    }

                                                                                    @Override
                                                                                    public void
                                                                                            onCancelled(
                                                                                                    DatabaseError
                                                                                                            error) {}
                                                                                });
                                                                dialogInterface.dismiss();
                                                            })
                                                    .setNegativeButton(
                                                            "Cancel",
                                                            (dialogInterface, which) -> {
                                                                dialogInterface.dismiss();
                                                            })
                                                    .build();

                                    // Show Dialog
                                    mDialog.show();
                                }
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                // do whatever
                                BottomSheetMaterialDialog mBottomSheetDialog =
                                        new BottomSheetMaterialDialog.Builder(getActivity())
                                                .setTitle("Remove Account?")
                                                .setMessage(
                                                        "Are you sure you want to remove this"
                                                                + " account?")
                                                .setCancelable(false)
                                                .setPositiveButton(
                                                        "Remove",
                                                        R.drawable.ic_delete,
                                                        new BottomSheetMaterialDialog
                                                                .OnClickListener() {
                                                            @Override
                                                            public void onClick(
                                                                    DialogInterface dialogInterface,
                                                                    int which) {
                                                                if (FileUtils.isExistFile(path)) {
                                                                    accountList =
                                                                            new Gson()
                                                                                    .fromJson(
                                                                                            FileUtils
                                                                                                    .readFile(
                                                                                                            path),
                                                                                            new TypeToken<
                                                                                                    ArrayList<
                                                                                                            HashMap<
                                                                                                                    String,
                                                                                                                    Object>>>() {}.getType());
                                                                    accountList.remove(position);
                                                                    FileUtils.writeFile(
                                                                            path2
                                                                                    + "/accounts.json",
                                                                            new Gson()
                                                                                    .toJson(
                                                                                            accountList));
                                                                    adapter.notifyItemRemoved(
                                                                            position);
                                                                }
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                .setNegativeButton(
                                                        "Cancel",
                                                        R.drawable.ic_close,
                                                        new BottomSheetMaterialDialog
                                                                .OnClickListener() {
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
                            }
                        }));
    }

    public void initializeLogic() {
        toolbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
    }

    private void showLoadingDialog(String email, String password) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        final LayoutInflater inflater = getLayoutInflater();
        final View convertView = inflater.inflate(R.layout.loading_view, null);
        alertDialog.setView(convertView);
        final ProgressBar progressbar1 = (ProgressBar) convertView.findViewById(R.id.progressbar1);
        ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressbar1);
        Sprite doubleBounce = new WanderingCubes();
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressbar1.getIndeterminateDrawable().setTint(0xFFFFFFFF);
        alertDialog.setCancelable(false);
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();
		logoutButtonClicked();
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                getActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(android.R.id.content, new SplashActivity())
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                Toast.makeText(
                                                getActivity(),
                                                task.getException().toString(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                            preferencesManager.clear();
                            alertDialog.dismiss();
                        });
    }

    private void logoutButtonClicked() {
        if (getSinchServiceInterface() != null) {
            UserController uc =
                    Sinch.getUserControllerBuilder()
                            .context(getActivity())
                            .applicationKey(SinchService.APP_KEY)
                            .userId(UserConfig.getInstance().getUid())
                            .environmentHost(SinchService.ENVIRONMENT)
                            .build();
            uc.unregisterPushToken(
                    new PushTokenRegistrationCallback() {
                        @Override
                        public void onPushTokenRegistered() {
                            Log.d("Switcher", "Successfully unregistered Push Token!");
                        }

                        @Override
                        public void onPushTokenRegistrationFailed(SinchError error) {
                            Log.e("Switcher", "Unregistration of the Push Token failed!");
                        }
                    });
            getSinchServiceInterface().stopClient();
        }
    }
}

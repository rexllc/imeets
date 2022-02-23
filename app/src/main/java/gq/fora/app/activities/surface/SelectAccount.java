package gq.fora.app.activities.surface;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;
import gq.fora.app.R;
import gq.fora.app.activities.AuthActivity;
import gq.fora.app.activities.SignUpActivity;
import gq.fora.app.activities.SplashActivity;
import gq.fora.app.adapter.AccountList2Adapter;
import gq.fora.app.listener.onclick.RecyclerItemClickListener;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.ForaUtil;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SelectAccount extends Fragment {

    private MaterialButton login_button, signup_button;
    private RecyclerView account_list;
    private ArrayList<HashMap<String, Object>> accountList = new ArrayList<>();
    private LinearLayoutManager layoutAdapter;
    private AccountList2Adapter adapter;
    private double position = 0;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private TimerTask timer;
    private Timer _timer = new Timer();
    private byte[] decval;
    private byte[] decode;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().finishAffinity();
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_select_account, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {

        account_list = view.findViewById(R.id.account_list);
        login_button = view.findViewById(R.id.login_button);
        signup_button = view.findViewById(R.id.signup_button);

        String path = FileUtils.getPackageDataDir(getContext()) + "/user/accounts.json";
        String path2 = FileUtils.getPackageDataDir(getContext()) + "/user";

        if (FileUtils.isExistFile(path)) {
            accountList =
                    new Gson()
                            .fromJson(
                                    FileUtils.readFile(path),
                                    new TypeToken<
                                            ArrayList<HashMap<String, Object>>>() {}.getType());

            layoutAdapter = new LinearLayoutManager(getActivity());
            adapter = new AccountList2Adapter(getActivity(), adapter, accountList);
            account_list.setAdapter(adapter);
            account_list.setHasFixedSize(true);
            account_list.setLayoutManager(layoutAdapter);
            adapter.notifyItemInserted(0);
        }

        login_button.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, new AuthActivity())
                            .addToBackStack(null)
                            .commit();
                });

        signup_button.setOnClickListener(
                (View v) -> {
                    try {
                        final AlertDialog alertDialog =
                                new AlertDialog.Builder(getActivity()).create();
                        final LayoutInflater inflater = getLayoutInflater();
                        final View convertView =
                                (View) inflater.inflate(R.layout.loading_view, null);
                        alertDialog.setView(convertView);
                        final ProgressBar progressbar1 =
                                (ProgressBar) convertView.findViewById(R.id.progressbar1);
                        ProgressBar progressBar =
                                (ProgressBar) convertView.findViewById(R.id.progressbar1);
                        Sprite doubleBounce = new WanderingCubes();
                        progressBar.setIndeterminateDrawable(doubleBounce);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                            progressbar1
                                    .getIndeterminateDrawable()
                                    .setColorFilter(
                                            0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        alertDialog.setCancelable(false);
                        alertDialog
                                .getWindow()
                                .setBackgroundDrawableResource(android.R.color.transparent);
                        alertDialog.show();
                        timer =
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        getActivity()
                                                .runOnUiThread(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                if (ForaUtil.isConnected(
                                                                        getActivity())) {
                                                                    alertDialog.dismiss();
                                                                    getActivity()
                                                                            .getSupportFragmentManager()
                                                                            .beginTransaction()
                                                                            .add(
                                                                                    android.R
                                                                                            .id
                                                                                            .content,
                                                                                    new SignUpActivity())
                                                                            .setTransition(
                                                                                    FragmentTransaction
                                                                                            .TRANSIT_FRAGMENT_OPEN)
                                                                            .addToBackStack(null)
                                                                            .commit();
                                                                } else {
                                                                    Snackbar.make(
                                                                                    signup_button,
                                                                                    "No Internet Connection",
                                                                                    Snackbar
                                                                                            .LENGTH_SHORT)
                                                                            .show();
                                                                }
                                                            }
                                                        });
                                    }
                                };
                        _timer.schedule(timer, (int) (2000));
                    } catch (Exception e) {

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
                                users.child(
                                                accountList
                                                        .get((int) position)
                                                        .get("userId")
                                                        .toString())
                                        .addListenerForSingleValueEvent(
                                                new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(DataSnapshot arg0) {
                                                        User user = arg0.getValue(User.class);
                                                        if (user != null) {
                                                            
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError arg0) {}
                                                });
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                // do whatever
                                BottomSheetMaterialDialog mBottomSheetDialog =
                                        new BottomSheetMaterialDialog.Builder(getActivity())
                                                .setTitle("Remove Account?")
                                                .setMessage(
                                                        "Are you sure you want to remove this account?")
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
                                                                    accountList.remove(
                                                                            (int) position);
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

    public void initializeLogic() {}

    private void showLoadingDialog(String email, String password) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        final LayoutInflater inflater = getLayoutInflater();
        final View convertView = (View) inflater.inflate(R.layout.loading_view, null);
        alertDialog.setView(convertView);
        final ProgressBar progressbar1 = (ProgressBar) convertView.findViewById(R.id.progressbar1);
        ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressbar1);
        Sprite doubleBounce = new WanderingCubes();
        progressBar.setIndeterminateDrawable(doubleBounce);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            progressbar1
                    .getIndeterminateDrawable()
                    .setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        alertDialog.setCancelable(false);
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnCompleteListener(
                        new OnCompleteListener() {

                            @Override
                            public void onComplete(Task task) {
                                boolean isSuccess = task.isSuccessful();
                                final String errorMessage =
                                        task.getException() != null
                                                ? task.getException().getMessage()
                                                : "";
                                if (isSuccess) {
                                    getActivity()
                                            .getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(android.R.id.content, new SplashActivity())
                                            .setTransition(
                                                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .addToBackStack(null)
                                            .commit();
                                } else {
                                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT)
                                            .show();
                                }
                                alertDialog.dismiss();
                            }
                        });
    }

    public void loginDialog() {}

    private SecretKey generateKey(String pwd) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] b = pwd.getBytes("UTF-8");

        digest.update(b, 0, b.length);

        byte[] key = digest.digest();

        SecretKeySpec sec = new SecretKeySpec(key, "AES");

        return sec;
    }
}

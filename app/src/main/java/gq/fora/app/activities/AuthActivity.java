package gq.fora.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.transition.TransitionManager;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;
import dev.shreyaspatil.MaterialDialog.model.TextAlignment;

import gq.fora.app.R;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class AuthActivity extends Fragment {

    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");
    private String TAG = AuthActivity.class.getSimpleName();
    private FirebaseAuth mAuth;
    private LinearLayout linear1;
    private EditText edittext1, edittext2;
    private MaterialButton login_button, signup_button, forgot_button;
    private ImageView app_logo, password_show_toggle;
    private MaterialAlertDialogBuilder dialog;
    private TimerTask timer;
    private Timer _timer = new Timer();

    private ArrayList<HashMap<String, Object>> accountList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> accountList2 = new ArrayList<>();
    private double position = 0;
    private boolean isPasswordHidden = true;
    private Window window;
    private View view;
    private AlertDialog alertDialog;

    private OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().finishAffinity();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        window = getActivity().getWindow();
        view = window.getDecorView();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_auth, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initializeBundle(Bundle savedInstanceState, View view) {

        login_button = view.findViewById(R.id.login_button);
        signup_button = view.findViewById(R.id.signup_button);
        edittext1 = view.findViewById(R.id.edittext1);
        edittext2 = view.findViewById(R.id.edittext2);
        linear1 = view.findViewById(R.id.linear1);
        app_logo = view.findViewById(R.id.app_logo);
        password_show_toggle = view.findViewById(R.id.password_show_toggle);
        forgot_button = view.findViewById(R.id.forgot_button);

        // initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        login_button.setEnabled(false);

        password_show_toggle.setOnClickListener(
                (View v) -> {
                    if (edittext2
                            .getTransformationMethod()
                            .equals(PasswordTransformationMethod.getInstance())) {
                        /*Show Password*/
                        password_show_toggle.setImageResource(R.drawable.ion_eye_disabled);
                        edittext2.setTransformationMethod(
                                HideReturnsTransformationMethod.getInstance());
                    } else {
                        /*Hide Password*/
                        password_show_toggle.setImageResource(R.drawable.ion_eye);
                        edittext2.setTransformationMethod(
                                PasswordTransformationMethod.getInstance());
                    }
                    edittext2.setSelection(edittext2.getText().length());
                });

        edittext2.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(edittext2.getText().toString())) {
                            login_button.setEnabled(true);
                            password_show_toggle.setVisibility(View.VISIBLE);
                        } else {
                            login_button.setEnabled(false);
                            password_show_toggle.setVisibility(View.GONE);
                        }
                    }
                });

        login_button.setOnClickListener(
                (v) -> {
                    showLoading();
                    if (!edittext1.getText().toString().trim().isEmpty()
                            && !edittext2.getText().toString().trim().isEmpty()) {
                        if (Utils.getInstance(getActivity())
                                .isValidEmail(edittext1.getText().toString())) {
                            loginUser(edittext1.getText().toString().trim());
                        } else {
                            users.orderByChild("username")
                                    .equalTo(edittext1.getText().toString().trim())
                                    .addListenerForSingleValueEvent(
                                            new ValueEventListener() {
                                                @Override
                                                public void onDataChange(
                                                        DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot ds :
                                                            dataSnapshot.getChildren()) {
                                                        User model = ds.getValue(User.class);
                                                        if (dataSnapshot.exists()) {
                                                            if (model != null) {
                                                                loginUser(model.email);
                                                            }
                                                        } else {
                                                            loginFailed();
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(
                                                        DatabaseError databaseError) {}
                                            });
                        }
                    }
                    WindowInsetsControllerCompat insetsController =
                            new WindowInsetsControllerCompat(window, view);
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        // keyboard is opened
                        insetsController.hide(WindowInsetsCompat.Type.ime());
                    } else {
                        // keyboard is closed
                    }
                });

        signup_button.setOnClickListener(
                (View v) -> {
                    try {
                        showLoading();
                        new Handler(Looper.getMainLooper())
                                .postDelayed(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                alertDialog.dismiss();
                                                getActivity()
                                                        .getSupportFragmentManager()
                                                        .beginTransaction()
                                                        .add(
                                                                android.R.id.content,
                                                                new SignUpActivity())
                                                        .setTransition(
                                                                FragmentTransaction
                                                                        .TRANSIT_FRAGMENT_OPEN)
                                                        .addToBackStack(null)
                                                        .commit();
                                            }
                                        },
                                        2000);

                    } catch (Exception e) {

                    }
                });

        String path2 = FileUtils.getPackageDataDir(getActivity()) + "/user/accounts.json";

        if (FileUtils.isExistFile(path2)) {
            accountList2 =
                    new Gson()
                            .fromJson(
                                    FileUtils.readFile(path2),
                                    new TypeToken<
                                            ArrayList<HashMap<String, Object>>>() {}.getType());
        }

        forgot_button.setOnClickListener(
                (v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new ForgotPasswordActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });
    }

    private void initializeLogic() {
        getActivity()
                .getWindow()
                .getDecorView()
                .setOnApplyWindowInsetsListener(
                        (view, insetsController) -> {
                            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
                            new Handler(Looper.getMainLooper())
                                    .post(
                                            () -> {
                                                if (insets.isVisible(
                                                        WindowInsetsCompat.Type.ime())) {
                                                    // keyboard is opened
                                                    TransitionManager.beginDelayedTransition(
                                                            linear1);
                                                    app_logo.setVisibility(View.GONE);
                                                } else {
                                                    // keyboard is closed
                                                    TransitionManager.beginDelayedTransition(
                                                            linear1);
                                                    app_logo.setVisibility(View.VISIBLE);
                                                }
                                            });
                            return view.onApplyWindowInsets(insetsController);
                        });

        edittext2.setOnEditorActionListener(
                (textView, actionId, keyEvent) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        login_button.performClick();
                        return true;
                    }
                    return false;
                });
    }

    private void verifyEmail() {

        FirebaseAuth.getInstance()
                .getCurrentUser()
                .sendEmailVerification()
                .addOnCompleteListener(
                        getActivity(),
                        (task) -> {
                            if (task.isSuccessful()) {
                                showMessage();
                                FirebaseAuth.getInstance().signOut();
                            } else {
                                Log.e(TAG, "sendEmailVerification", task.getException());
                            }
                        });
    }

    private void showMessage() {
        MaterialDialog mDialog2 =
                new MaterialDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.dialog_email_title))
                        .setMessage(getActivity().getString(R.string.dialog_msg_email))
                        .setCancelable(false)
                        .setPositiveButton(
                                getActivity().getString(R.string.dialog_ok),
                                (dialogInterface, which) -> {
                                    // Operation
                                    dialogInterface.dismiss();
                                })
                        .build();

        // Show Dialog
        mDialog2.show();
    }

    private void addAccountToList() {
        String path = FileUtils.getPackageDataDir(getActivity()) + "/user";
        if (!FileUtils.isExistFile(path)) {
            FileUtils.makeDir(path);
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", UserConfig.getInstance().getUid());
            map.put("require_password", true);
            map.put("password", edittext2.getText().toString());
            map.put("last_signed", System.currentTimeMillis());
            accountList.add(map);
            FileUtils.writeFile(path + "/accounts.json", new Gson().toJson(accountList));
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new SplashActivity())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        } else {
            for (int _i = 0; _i < (float) (accountList2.size()); _i++) {
                if (accountList2
                        .get(_i)
                        .get("userId")
                        .toString()
                        .equals(UserConfig.getInstance().getUid())) {
                    MaterialDialog mDialog3 =
                            new MaterialDialog.Builder(getActivity())
                                    .setTitle("Error")
                                    .setMessage("Account is already existed on this device.")
                                    .setCancelable(false)
                                    .setPositiveButton(
                                            getActivity().getString(R.string.dialog_ok),
                                            (dialogInterface, which) -> {
                                                // Operation
                                                dialogInterface.dismiss();
                                            })
                                    .build();
                    // Show Dialog
                    mDialog3.show();
                    FirebaseAuth.getInstance().signOut();
                } else {
                    HashMap<String, Object> map2 = new HashMap<>();
                    map2.put("userId", UserConfig.getInstance().getUid());
                    map2.put("require_password", true);
                    map2.put("password", edittext2.getText().toString());
                    map2.put("last_signed", System.currentTimeMillis());
                    accountList2.add(map2);
                    FileUtils.writeFile(path + "/accounts.json", new Gson().toJson(accountList2));
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, new SplashActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    }

    private void showLoading() {
        alertDialog = new AlertDialog.Builder(getActivity()).create();
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
    }

    private void emailNotVerifiedDialog() {
        MaterialDialog mDialog =
                new MaterialDialog.Builder(getActivity())
                        .setTitle(
                                getActivity().getString(R.string.email_not_verified),
                                TextAlignment.CENTER)
                        .setMessage(
                                getActivity().getString(R.string.verify_your_email),
                                TextAlignment.CENTER)
                        .setCancelable(false)
                        .setPositiveButton(
                                getActivity().getString(R.string.dialog_ok),
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        // Operation
                                        dialogInterface.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                getActivity().getString(R.string.resend),
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        verifyEmail();
                                        dialogInterface.dismiss();
                                    }
                                })
                        .build();

        // Show Dialog
        mDialog.show();
    }

    private void loginUser(String username) {
        mAuth.signInWithEmailAndPassword(username, edittext2.getText().toString().trim())
                .addOnCompleteListener(
                        getActivity(),
                        (task) -> {
                            final String errorMessage =
                                    task.getException() != null
                                            ? task.getException().getMessage()
                                            : "";
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the
                                // signed-in
                                // user's information
                                Log.d(TAG, "signInWithEmail:success");
                                if (FirebaseAuth.getInstance().getCurrentUser().isEmailVerified()) {
                                    addAccountToList();
                                } else {
                                    emailNotVerifiedDialog();
                                }
                            } else {
                                // If sign in fails, display a message to
                                // the
                                // user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Snackbar.make(login_button, errorMessage, Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                            alertDialog.dismiss();
                        });
    }

    private void loginFailed() {
        Snackbar.make(login_button, "The account doesn't exist.", Snackbar.LENGTH_SHORT).show();
    }
}

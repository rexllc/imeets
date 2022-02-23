package gq.fora.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import gq.fora.app.R;
import gq.fora.app.activities.calling.BaseActivity;
import gq.fora.app.utils.Utils;

public class VerifyPasswordResetActivity extends BaseActivity {

    private LinearLayout toolbar;
    private FrameLayout actionbar;
    private EditText password, confirm_password;
    private MaterialButton save_button;
    private ImageView back;
    private TextView title;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user;
    private AlertDialog builder;
    private String shared_data = "";
    private String shared_key = "";
    private String shared_uid = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_password_reset);
        initViews(savedInstanceState);

        if (getIntent().getStringExtra("oobCode") != null
                && getIntent().getStringExtra("lang") != null) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(
                            () -> {
                                mAuth.setLanguageCode(getIntent().getStringExtra("lang"));
                                mAuth.verifyPasswordResetCode(getIntent().getStringExtra("oobCode"))
                                        .addOnCompleteListener(
                                                (task) -> {
                                                    if (task.isSuccessful()) {
                                                    } else {
                                                    }
                                                });
                            },
                            2000);
        }
    }

    public void initViews(Bundle savedInstanceState) {
        actionbar = findViewById(R.id.actionbar);
        password = findViewById(R.id.password);
        confirm_password = findViewById(R.id.confirm_password);
        save_button = findViewById(R.id.save_button);

        View action_bar =
                LayoutInflater.from(VerifyPasswordResetActivity.this)
                        .inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText("Forgot password");
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    finish();
                });

        save_button.setOnClickListener(
                (v) -> {
                    if (isValidCredentials()) {
                        newPassword();
                    }
                });
    }

    private void showLoading() {
        builder = new AlertDialog.Builder(this).create();
        View _view = LayoutInflater.from(this).inflate(R.layout.progressbar_loading, null);
        builder.setView(_view);
        final ProgressBar loader = (ProgressBar) _view.findViewById(R.id.loader);
        final LinearLayout root = (LinearLayout) _view.findViewById(R.id.root);
        loader.getIndeterminateDrawable().setTint(R.color.primary);
        root.setElevation(8);
        builder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        builder.setCancelable(false);
        builder.show();
    }

    private void newPassword() {
        showLoading();
        mAuth.confirmPasswordReset(
                        getIntent().getStringExtra("oobCode"),
                        confirm_password.getText().toString())
                .addOnCompleteListener(
                        (task) -> {
                            if (task.isSuccessful()) {
                                getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(android.R.id.content, new AuthActivity())
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                Toast.makeText(
                                                VerifyPasswordResetActivity.this,
                                                task.getException().getMessage(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                            builder.dismiss();
                        });
    }

    private boolean isValidCredentials() {
        if (TextUtils.isEmpty(password.getText().toString())) {
            Snackbar.make(save_button, "Enter your new password.", Snackbar.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(confirm_password.getText().toString())) {
            Snackbar.make(save_button, "Enter confirm password.", Snackbar.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(getIntent().getStringExtra("oobCode"))) {
            Snackbar.make(
                            save_button,
                            "Invalid response code from the server.",
                            Snackbar.LENGTH_SHORT)
                    .show();
            return false;
        }
        return true;
    }
}

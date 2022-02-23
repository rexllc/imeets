package gq.fora.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.utils.Utils;

public class ForgotPasswordActivity extends BaseFragment {

    private LinearLayout toolbar;
    private FrameLayout actionbar;
    private EditText email;
    private MaterialButton next_button;
    private ImageView back;
    private TextView title;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private AlertDialog builder;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater layoutInflater,
            @Nullable ViewGroup viewGroup,
            @Nullable Bundle bundle) {
        View view =
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.activity_forgot_password, viewGroup, false);
        initViews(bundle, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    public void initViews(Bundle savedInstanceState, View view) {
        actionbar = view.findViewById(R.id.actionbar);
        email = view.findViewById(R.id.email_or_username);
        next_button = view.findViewById(R.id.next_button);

        mAuth = FirebaseAuth.getInstance();

        user = mAuth.getCurrentUser();
        DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText("Forgot password");
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        next_button.setOnClickListener(
                (v) -> {
                    if (Utils.getInstance(getActivity()).isValidEmail(email.getText().toString())) {
                        showLoading();
                        mAuth.sendPasswordResetEmail(email.getText().toString())
                                .addOnCompleteListener(
                                        (task) -> {
                                            if (task.isSuccessful()) {
                                                showMessage(
                                                        "Password reset sent",
                                                        "Password reset code was sent to your email"
                                                                + " address.");
                                            } else {
                                                showMessage(
                                                        "An error occurred",
                                                        "This action is cannot be completed. Please"
                                                                + " try again.");
                                            }
                                            builder.dismiss();
                                        });
                    } else {
                        showMessage("Invalid email", "Your email address is invalid.");
                    }
                });
    }

    private void showLoading() {
        builder = new AlertDialog.Builder(getActivity()).create();
        View _view = LayoutInflater.from(getActivity()).inflate(R.layout.progressbar_loading, null);
        builder.setView(_view);
        final ProgressBar loader = (ProgressBar) _view.findViewById(R.id.loader);
        final LinearLayout root = (LinearLayout) _view.findViewById(R.id.root);
        loader.getIndeterminateDrawable().setTint(R.color.primary);
        root.setElevation(8);
        builder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        builder.setCancelable(false);
        builder.show();
    }

    private void showMessage(String title, String message) {
        MaterialAlertDialogBuilder mDialog2 =
                new MaterialAlertDialogBuilder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(
                                getActivity().getString(R.string.dialog_ok),
                                (dialogInterface, which) -> {
                                    // Operation
                                    dialogInterface.dismiss();
                                });

        // Show Dialog
        mDialog2.show();
    }
}

package gq.fora.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.models.UserConfig;
import gq.fora.app.utils.PreferencesManager;
import gq.fora.app.utils.Utils;

import java.util.HashMap;

public class ChangeEmailActivity extends BaseFragment {

    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");
    private LinearLayout toolbar;
    private ImageView back;
    private TextView title, change_email_info;
    private FrameLayout actionbar;
    private EditText email;
    private MaterialButton save_button;

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private PreferencesManager preferencesManager;
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
                        .inflate(R.layout.activity_change_email, viewGroup, false);
        initViews(bundle, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    public void initViews(Bundle savedInstanceState, View view) {
        actionbar = view.findViewById(R.id.actionbar);
        email = view.findViewById(R.id.email);
        change_email_info = view.findViewById(R.id.change_email_info);
        save_button = view.findViewById(R.id.save_button);

        mAuth = FirebaseAuth.getInstance();

        preferencesManager = new PreferencesManager(getActivity());
        user = mAuth.getCurrentUser();

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText(getResources().getString(R.string.change_email_actionbar_title));
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        save_button.setOnClickListener(
                (v) -> {
                    confirmPassword();
                });

        email.setText(user.getEmail());
        email.setSelection(email.getText().length());
    }

    public void confirmPassword() {
        BottomSheetDialog dialog = new BottomSheetDialog(getActivity(), R.style.DialogStyle);
        View sheetView =
                LayoutInflater.from(dialog.getContext())
                        .inflate(R.layout.bottom_sheet_confirm_password, null);
        dialog.setContentView(sheetView);
        final EditText password = (EditText) sheetView.findViewById(R.id.password);
        final MaterialButton save_button =
                (MaterialButton) sheetView.findViewById(R.id.save_button);

        final FrameLayout frameLayout = (FrameLayout) dialog.findViewById(R.id.design_bottom_sheet);
        frameLayout.setBackgroundColor(android.R.color.transparent);

        save_button.setOnClickListener(
                (v) -> {
                    showLoading();
                    AuthCredential credential =
                            EmailAuthProvider.getCredential(
                                    preferencesManager.getString("email_address"),
                                    password.getText().toString());

                    // Prompt the user to re-provide their sign-in credentials
                    user.reauthenticate(credential)
                            .addOnCompleteListener(
                                    (task) -> {
                                        final String errorMessage =
                                                task.getException() != null
                                                        ? task.getException().getMessage()
                                                        : "";
                                        Log.d("ChangeNameActivity", "User re-authenticated.");
                                        if (task.isSuccessful()) {
                                            updateUserEmail();
                                        } else {
                                            Utils.getInstance(getActivity())
                                                    .showToast(errorMessage);
                                            builder.dismiss();
                                        }
                                    });

                    dialog.dismiss();
                });

        dialog.show();
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

    private void updateUserEmail() {
        user.updateEmail(email.getText().toString().trim())
                .addOnSuccessListener(
                        (result) -> {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("email", user.getEmail());
                            users.child(UserConfig.getInstance().getUid())
                                    .updateChildren(map)
                                    .addOnSuccessListener(
                                            (task) -> {
                                                Utils.getInstance(getActivity())
                                                        .showToast("Changes was saved.");
                                                builder.dismiss();
                                                verifyEmail();
                                            });
                        })
                .addOnFailureListener(
                        (error) -> {
                            Utils.getInstance(getActivity()).showToast("Update failed.");
                            builder.dismiss();
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
                            } else {
                                Log.e(
                                        "ChangeEmailActivity",
                                        "sendEmailVerification",
                                        task.getException());
                                builder.dismiss();
                                Utils.getInstance(getActivity())
                                        .showToast("Failed to send verification.");
                            }
                        });
    }

    private void showMessage() {
        MaterialDialog mDialog2 =
                new MaterialDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.dialog_email_title))
                        .setMessage(getActivity().getString(R.string.dialog_msg_email, email.getText().toString()))
                        .setCancelable(false)
                        .setPositiveButton(
                                getActivity().getString(R.string.dialog_ok),
                                (dialogInterface, which) -> {
                                    // Operation
                                    dialogInterface.dismiss();
                                    getActivity().getSupportFragmentManager().popBackStack();
                                })
                        .build();

        // Show Dialog
        mDialog2.show();
    }
}

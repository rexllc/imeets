package gq.fora.app.activities;

import android.app.AlertDialog;
import android.net.Uri;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.models.UserConfig;
import gq.fora.app.utils.PreferencesManager;
import gq.fora.app.utils.Utils;

import java.util.HashMap;

public class ChangeNameActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private TextView change_name_info, title;
    private FrameLayout actionbar;
    private EditText first_name, middle_name, last_name;
    private MaterialButton save_button;

    private LinearLayout toolbar;
    private ImageView back;
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
                        .inflate(R.layout.activity_change_name, viewGroup, false);
        initViews(bundle, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    public void initViews(Bundle savedInstanceState, View view) {
        change_name_info = view.findViewById(R.id.change_name_info);
        first_name = view.findViewById(R.id.first_name);
        middle_name = view.findViewById(R.id.middle_name);
        last_name = view.findViewById(R.id.last_name);
        actionbar = view.findViewById(R.id.actionbar);
        save_button = view.findViewById(R.id.save_button);

        mAuth = FirebaseAuth.getInstance();

        preferencesManager = new PreferencesManager(getActivity());
        user = mAuth.getCurrentUser();

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText(getResources().getString(R.string.change_name_actionbar_title));
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

        database.collection("users")
                .document(UserConfig.getInstance().getUid())
                .get()
                .addOnCompleteListener(
                        (task) -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                first_name.setText(task.getResult().getString("firstName"));
                                if (task.getResult().getString("middleName") != null) {
                                    middle_name.setText(task.getResult().getString("middleName"));
                                }
                                last_name.setText(task.getResult().getString("lastName"));
                                first_name.setSelection(first_name.getText().length());
                                middle_name.setSelection(middle_name.getText().length());
                                last_name.setSelection(last_name.getText().length());
                            }
                        });
    }

    private void confirmPassword() {
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
                                    new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            final String errorMessage =
                                                    task.getException() != null
                                                            ? task.getException().getMessage()
                                                            : "";
                                            Log.d("ChangeNameActivity", "User re-authenticated.");
                                            if (task.isSuccessful()) {
                                                updateUserDisplayName();
                                            } else {
                                                Utils.getInstance(getActivity())
                                                        .showToast(errorMessage);
                                                builder.dismiss();
                                            }
                                        }
                                    });

                    dialog.dismiss();
                });

        dialog.show();
    }

    private void updateUserDisplayName() {
        FirebaseAuth.getInstance()
                .getCurrentUser()
                .updateProfile(
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(
                                        first_name.getText().toString().trim()
                                                + " "
                                                + last_name.getText().toString())
                                .setPhotoUri(Uri.parse(UserConfig.getInstance().getUserPhoto()))
                                .build())
                .addOnCompleteListener(
                        (task) -> {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("firstName", first_name.getText().toString());
                            map.put("middleName", middle_name.getText().toString());
                            map.put("lastName", last_name.getText().toString());
                            map.put(
                                    "displayName",
                                    first_name.getText().toString().trim()
                                            + " "
                                            + middle_name.getText().toString().trim()
                                            + " "
                                            + last_name.getText().toString().trim());
                            database.collection("users")
                                    .document(UserConfig.getInstance().getUid())
                                    .update(map)
                                    .addOnCompleteListener(
                                            (success) -> {
                                                getActivity()
                                                        .getSupportFragmentManager()
                                                        .popBackStack();
                                                Utils.getInstance(getActivity())
                                                        .showToast("Changes was updated.");
                                                builder.dismiss();
                                            });
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
}

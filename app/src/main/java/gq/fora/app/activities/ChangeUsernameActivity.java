package gq.fora.app.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.initializeApp;
import gq.fora.app.models.UserConfig;
import gq.fora.app.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class ChangeUsernameActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");
    private FirebaseUser user;
    private FrameLayout actionbar;
    private EditText edittext1;
    private TextView info, error, title;
    private MaterialButton save_button;
    private ProgressBar checker;
    private ImageView back, available;
    private LinearLayout linear1, toolbar;
    private String links = "";

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
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_change_username, parent, false);
        initViews(savedInstanceState, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    public void initViews(Bundle bundle, View view) {
        linear1 = view.findViewById(R.id.linear1);
        edittext1 = view.findViewById(R.id.edittext1);
        info = view.findViewById(R.id.info);
        error = view.findViewById(R.id.error);
        save_button = view.findViewById(R.id.save_button);
        checker = view.findViewById(R.id.checker);
        actionbar = view.findViewById(R.id.actionbar);
        available = view.findViewById(R.id.available);

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText("Change username");
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });
        setTextSpan(info, getResources().getString(R.string.username_info));
        save_button.setEnabled(false);
        checker.getIndeterminateDrawable().setTint(R.color.primary);
        edittext1.setFocusable(true);

        database.collection("users")
                .document(UserConfig.getInstance().getUid())
                .addSnapshotListener(
                        (value, exception) -> {
                            if (exception != null) return;

                            if (value != null) {
                                edittext1.setText(value.getString("username"));
                                edittext1.setSelection(edittext1.getText().length());
                            }
                        });

        edittext1.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        checker.setVisibility(View.VISIBLE);
                        available.setVisibility(View.GONE);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        database.collection("users")
                                .addSnapshotListener(
                                        (value, exception) -> {
                                            if (value != null) {
                                                value.getDocuments()
                                                        .forEach(
                                                                (docs) -> {
                                                                    checkUsername(docs);
                                                                });
                                            } else {
                                                error.setVisibility(View.GONE);
                                                available.setVisibility(View.VISIBLE);
                                                save_button.setEnabled(true);
                                                TransitionManager.beginDelayedTransition(linear1);
                                            }
                                        });
                    }
                });

        save_button.setOnClickListener(
                v -> {
                    AlertDialog builder = new AlertDialog.Builder(getActivity()).create();
                    View _view =
                            LayoutInflater.from(getActivity())
                                    .inflate(R.layout.progressbar_loading, null);
                    builder.setView(_view);
                    final ProgressBar loader = (ProgressBar) _view.findViewById(R.id.loader);
                    final LinearLayout root = (LinearLayout) _view.findViewById(R.id.root);
                    loader.getIndeterminateDrawable().setTint(R.color.primary);
                    root.setElevation(8);
                    builder.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    builder.setCancelable(false);
                    builder.show();
                    new Handler(Looper.getMainLooper())
                            .postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Map<String, Object> map = new HashMap<>();
                                            map.put("username", edittext1.getText().toString());
                                            users.child(UserConfig.getInstance().getUid())
                                                    .updateChildren(map)
                                                    .addOnSuccessListener(
                                                            (task) -> {
                                                                getActivity()
                                                                        .getSupportFragmentManager()
                                                                        .popBackStack();
                                                                Toast.makeText(
                                                                                getActivity(),
                                                                                "Username has been"
                                                                                    + " updated.",
                                                                                Toast.LENGTH_SHORT)
                                                                        .show();
                                                                builder.dismiss();
                                                            });
                                        }
                                    },
                                    3000);
                });
    }

    public void setTextSpan(final TextView _txt, final String _value) {
        _txt.setAutoLinkMask(android.text.util.Linkify.ALL);

        _txt.setLinkTextColor(0xFF1976D2);

        _txt.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        // _txt.setTextIsSelectable(true);
        updateSpan(_value, _txt);
    }

    private void updateSpan(String str, TextView _txt) {
        SpannableString ssb = new SpannableString(str);
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("Learn more about Username on Fora.");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            ProfileSpan span = new ProfileSpan();
            ssb.setSpan(span, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        _txt.setText(ssb);
    }

    private class ProfileSpan extends android.text.style.ClickableSpan {

        @Override
        public void onClick(View view) {

            if (view instanceof TextView) {
                TextView tv = (TextView) view;

                if (tv.getText() instanceof Spannable) {
                    Spannable sp = (Spannable) tv.getText();
                    int start = sp.getSpanStart(this);
                    int end = sp.getSpanEnd(this);
                    links = sp.subSequence(start, end).toString();
                }
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            ds.setColor(Color.parseColor("#1976D2"));
            ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
    }

    private void checkUsername(@NonNull DocumentSnapshot docs) {
        new Handler(Looper.getMainLooper())
                .postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (UserConfig.getInstance().getUsername() != null) {
                                    if (!String.valueOf(edittext1.getText())
                                            .equals(UserConfig.getInstance().getUsername())) {
                                        if (!TextUtils.isEmpty(
                                                String.valueOf(edittext1.getText()).trim())) {
                                            if (docs.getString("username").equals(edittext1.getText().toString())) {
                                                error.setVisibility(View.VISIBLE);
                                                save_button.setEnabled(false);
                                                available.setVisibility(View.GONE);
                                                TransitionManager.beginDelayedTransition(linear1);
                                            } else {
                                                error.setVisibility(View.GONE);
                                                available.setVisibility(View.VISIBLE);
                                                save_button.setEnabled(true);
                                                TransitionManager.beginDelayedTransition(linear1);
                                            }

                                        } else {
                                            save_button.setEnabled(false);
                                            error.setVisibility(View.GONE);
                                            available.setVisibility(View.GONE);
                                            TransitionManager.beginDelayedTransition(linear1);
                                        }
                                    }
                                }
                                checker.setVisibility(View.GONE);
                                error.setText(
                                        initializeApp.context
                                                .getResources()
                                                .getString(
                                                        R.string.error_username,
                                                        edittext1.getText().toString()));
                            }
                        },
                        2000);
    }
}

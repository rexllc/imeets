package gq.fora.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.utils.Utils;

public class AccountSettingsActivity extends BaseFragment {

    private FrameLayout actionbar;
    private ImageView back, account_name, email, address, password;
    private TextView title,
            personal_info,
            personal_info2,
            account_name_title,
            account_name_description,
            email_title,
            email_description,
            address_title,
            address_description,
            security_info,
            security_info2,
            password_title,
            password_description;
    private LinearLayout toolbar,
            account_name_layout,
            email_layout,
            address_layout,
            password_layout;

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
                        .inflate(R.layout.activity_account_settings, viewGroup, false);
        initViews(bundle, view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    public void initViews(Bundle savedInstanceState, View view) {
        actionbar = view.findViewById(R.id.actionbar);
        account_name = view.findViewById(R.id.account_name);
        account_name_title = view.findViewById(R.id.account_name_title);
        account_name_layout = view.findViewById(R.id.account_name_layout);
        account_name_description = view.findViewById(R.id.account_name_description);
        personal_info = view.findViewById(R.id.personal_info);
        personal_info2 = view.findViewById(R.id.personal_info2);
        email = view.findViewById(R.id.email);
        email_title = view.findViewById(R.id.email_title);
        email_layout = view.findViewById(R.id.email_layout);
        address = view.findViewById(R.id.address);
        address_title = view.findViewById(R.id.address_title);
        address_layout = view.findViewById(R.id.address_layout);
		password = view.findViewById(R.id.password);
		password_layout = view.findViewById(R.id.password_layout);
		password_title = view.findViewById(R.id.password_title);

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText(getResources().getString(R.string.account_settings_actionbar_title));
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        account_name_layout.setOnClickListener(
                (v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new ChangeNameActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });

        email_layout.setOnClickListener(
                (v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new ChangeEmailActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });

        address_layout.setOnClickListener((v) -> {});

        password_layout.setOnClickListener((v) -> {});

        Utils.rippleRoundStroke(account_name_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(email_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(address_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
		Utils.rippleRoundStroke(password_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
    }
}

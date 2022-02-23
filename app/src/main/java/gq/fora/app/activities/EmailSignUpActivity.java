package gq.fora.app.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import gq.fora.app.R;
import gq.fora.app.models.User;

public class EmailSignUpActivity extends Fragment {

    private LinearLayout linear1;
    private EditText edittext1, edittext2;
    private MaterialButton next_button;
    private MaterialButton previous_button;
    private TextView title;
    private FirebaseAuth mAuth;
    private String TAG = "FORA";
    private SharedPreferences data;
    private AlertDialog alertDialog;
    private OnCompleteListener<Void> mAuth_updateProfileListener;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference usersRef = database.collection("users");

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
        View _view = _inflater.inflate(R.layout.activity_signup, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle _savedInstanceState, View _view) {
        mAuth = FirebaseAuth.getInstance();
        linear1 = _view.findViewById(R.id.linear1);
        edittext1 = _view.findViewById(R.id.edittext1);
        edittext2 = _view.findViewById(R.id.edittext2);
        next_button = _view.findViewById(R.id.next_button);
        previous_button = _view.findViewById(R.id.previous_button);
        title = _view.findViewById(R.id.title);

        data = getContext().getSharedPreferences("token", Activity.MODE_PRIVATE);

        previous_button.setOnClickListener(
                (View v) -> {
                    try {
                        InputMethodManager imm =
                                (InputMethodManager)
                                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (imm.isAcceptingText()) {
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext1.getWindowToken(), 0);
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext2.getWindowToken(), 0);
                        }
                    } catch (Exception e) {

                    }
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        mAuth_updateProfileListener =
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> _param1) {
                        final boolean _success = _param1.isSuccessful();
                        final String _errorMessage =
                                _param1.getException() != null
                                        ? _param1.getException().getMessage()
                                        : "";
                        if (_success) {
                            verifyEmail();
                            alertDialog.dismiss();
                        } else {
                            alertDialog.dismiss();
                        }
                    }
                };

        next_button.setOnClickListener(
                (View v) -> {
                    if (edittext1.getText().toString().isEmpty()
                            || edittext2.getText().toString().isEmpty()) {
                        Snackbar.make(
                                        next_button,
                                        "All fields are required.",
                                        Snackbar.LENGTH_SHORT)
                                .show();
                    } else {
                        edittext1.setEnabled(false);
                        edittext2.setEnabled(false);
                        previous_button.setEnabled(false);
                        Bundle bundle = this.getArguments();
                        if (bundle != null) {
                            mAuth.createUserWithEmailAndPassword(
                                            edittext1.getText().toString().trim(),
                                            edittext2.getText().toString().trim())
                                    .addOnCompleteListener(
                                            getActivity(),
                                            new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(
                                                        @NonNull Task<AuthResult> task) {
                                                    final String errorMessage =
                                                            task.getException() != null
                                                                    ? task.getException()
                                                                            .getMessage()
                                                                    : "";
                                                    if (task.isSuccessful()) {
                                                        // Sign in success, update UI with the
                                                        // signed-in
                                                        // user's information
                                                        String uid =
                                                                FirebaseAuth.getInstance()
                                                                        .getCurrentUser()
                                                                        .getUid();
                                                        createAccount(
                                                                uid,
                                                                edittext1
                                                                        .getText()
                                                                        .toString()
                                                                        .trim(),
                                                                edittext2
                                                                        .getText()
                                                                        .toString()
                                                                        .trim(),
                                                                bundle.getString("first_name"),
                                                                bundle.getString("last_name"),
                                                                data.getString("token", ""));
                                                    } else {
                                                        // If sign up fails, display a message to
                                                        // the
                                                        // user.
                                                        Log.w(
                                                                TAG,
                                                                "createUserWithEmail:failure",
                                                                task.getException());
                                                        AlertDialog.Builder builder =
                                                                new AlertDialog.Builder(
                                                                        getActivity());
                                                        builder.setTitle("Error");
                                                        builder.setMessage(errorMessage);
                                                        builder.setPositiveButton(
                                                                "Okay",
                                                                (d, w) -> {
                                                                    d.dismiss();
                                                                });
                                                    }
                                                }
                                            });
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Error");
                            builder.setMessage("Something went wrong. Please try again.");
                            builder.setPositiveButton(
                                    "Okay",
                                    (d, w) -> {
                                        d.dismiss();
                                    });
                            builder.show();
                            edittext1.setEnabled(true);
                            edittext2.setEnabled(true);
                            previous_button.setEnabled(true);
                        }
                    }
                });
    }

    public void initializeLogic() {
        title.setText(getString(R.string.your_email_and_password));
        edittext1.setHint(getString(R.string.textfield_1_hint));
        edittext2.setHint(getString(R.string.textfield_2_hint));
        edittext1.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edittext2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        edittext2.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                            next_button.performClick();
                            return true;
                        }
                        return false;
                    }
                });
    }

    public void createAccount(
            final String userId,
            final String email,
            final String password,
            final String first_name,
            final String last_name,
            final String token) {
        User user = new User(userId, email, password, first_name, last_name, token);
        usersRef.document(userId).set(user);
        FirebaseAuth.getInstance()
                .getCurrentUser()
                .updateProfile(
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(first_name + " " + last_name)
                                .setPhotoUri(
                                        Uri.parse(
                                                "https://firebasestorage.googleapis.com/v0/b/fora-store.appspot.com/o/images%2FWindows-10-user-icon-big.png?alt=media&token=14f5a679-de20-49ca-ae7f-0df1f16e3660"))
                                .build())
                .addOnCompleteListener(mAuth_updateProfileListener);

        alertDialog = new AlertDialog.Builder(getActivity()).create();
        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.loading_view, null);
        alertDialog.setView(convertView);
        final ProgressBar progressbar1 = (ProgressBar) convertView.findViewById(R.id.progressbar1);
        Sprite doubleBounce = new WanderingCubes();
        progressbar1.setIndeterminateDrawable(doubleBounce);
        progressbar1.getIndeterminateDrawable().setTint(0xFFFFFFFF);
        alertDialog.setCancelable(false);
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();
    }

    public void verifyEmail() {

        FirebaseAuth.getInstance()
                .getCurrentUser()
                .sendEmailVerification()
                .addOnCompleteListener(
                        getActivity(),
                        (task) -> {
                            if (task.isSuccessful()) {
                                getActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .add(android.R.id.content, new AgreementsActivity())
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .addToBackStack(null)
                                        .commit();
                                FirebaseAuth.getInstance().signOut();
                            } else {
                                Log.e(TAG, "sendEmailVerification", task.getException());
                            }
                        });
    }
}

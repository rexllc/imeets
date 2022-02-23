package gq.fora.app.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;

public class ForaAuthenticator extends AbstractAccountAuthenticator {

    public ForaAuthenticator(Context context) {
        super(context);
    }

    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse response,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle options) {
        // intent to start the login activity
        return null;
    }

    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response, Account account, Bundle options) {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse response,
            Account account,
            String authTokenType,
            Bundle options)
            throws NetworkErrorException {
        // retrieve authentication tokens from account manager storage or custom storage or
        // re-authenticate old tokens and return new ones
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return authTokenType;
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response, Account account, String[] features)
            throws NetworkErrorException {
        // check whether the account supports certain features
        return null;
    }

    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response,
            Account account,
            String authTokenType,
            Bundle options) {
        // when the user's session has expired or requires their previously available credentials to
        // be updated, here is the function to do it.
        FirebaseAuth.getInstance().signOut();
        return null;
    }

    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) {
        Bundle result = new Bundle();
        boolean allowed = true; // or whatever logic you want here
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, allowed);
        return result;
    }
}

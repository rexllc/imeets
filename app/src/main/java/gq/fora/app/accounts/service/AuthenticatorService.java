package gq.fora.app.accounts.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import gq.fora.app.accounts.ForaAuthenticator;

public class AuthenticatorService extends Service {

    private ForaAuthenticator authenticator;

    @Override
    public void onCreate() {
        authenticator = new ForaAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}

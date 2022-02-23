package gq.fora.app.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import gq.fora.app.service.SinchService;

public class ComponentRegistrar {

    private String appKey;
    private String appSecret;
    private String environment;
    private Bundle bundle;

    public ComponentRegistrar(@NonNull Context ctx) {
        ServiceInfo ai = null;
        try {
            ComponentName myService = new ComponentName(ctx, SinchService.class);
            ai = ctx.getPackageManager().getServiceInfo(myService, PackageManager.GET_META_DATA);
            bundle = ai.metaData;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAppKey() {
        return bundle.getString("SINCH_APP_KEY");
    }

    public String getAppSecret() {
        return bundle.getString("SINCH_APP_SECRET");
    }

    public String getEnvironmentHost() {
        return bundle.getString("SINCH_ENVIRONMENT");
    }
}

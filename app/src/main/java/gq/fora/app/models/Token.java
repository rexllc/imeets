package gq.fora.app.models;

import android.os.Build;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Token {
	
	public String fcm_token;
	public String device_id;
	public String device_manufacturer;
	public String device_model;
	public String device_version;
	
	public Token() {
		//Default public constructor.
	}
	
	public Token(final String token) {
		this.fcm_token = token;
		this.device_id = Build.ID;
		this.device_manufacturer = Build.MANUFACTURER;
		this.device_model = Build.MODEL;
		this.device_version = Build.VERSION.RELEASE;
	}

}
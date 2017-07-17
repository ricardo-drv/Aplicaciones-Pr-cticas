package fcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by Gato on 10/07/2017.
 */

public class SquawkFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(getPackageName().getClass().getName(), "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);


    }

    private void sendRegistrationToServer(String refreshedToken) {

    }
}

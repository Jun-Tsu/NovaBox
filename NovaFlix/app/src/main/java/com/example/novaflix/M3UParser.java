package com.example.novaflix;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class M3UParser {

    public static List<Channel> parseM3U(String m3uUrl) {
        List<Channel> channels = new ArrayList<>();
        try {
            URL url = new URL(m3uUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String channelName = "";
            String channelLogo = "";
            String streamUrl = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF")) {
                    // Parse channel information
                    String[] parts = line.split(",");
                    channelName = parts[parts.length - 1].trim();
                    String info = parts[0];

                    // Extract logo URL if available
                    channelLogo = ""; // Reset logo before parsing new channel
                    String[] infoParts = info.split(" ");
                    for (String part : infoParts) {
                        if (part.startsWith("tvg-logo")) {
                            channelLogo = part.split("=")[1].replace("\"", "").trim();
                            // Log the parsed logo URL
                            Log.d("M3UParser", "Parsed logo URL: " + channelLogo);
                        }
                    }

                    // If no logo URL is found, log a warning
                    if (channelLogo.isEmpty()) {
                        Log.w("M3UParser", "No logo URL found for channel: " + channelName);
                    }
                } else if (!line.startsWith("#") && !line.isEmpty()) {
                    // Stream URL
                    streamUrl = line;
                    // Create a new Channel object and add to the list
                    channels.add(new Channel(channelName, channelLogo, streamUrl));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channels;
    }
}

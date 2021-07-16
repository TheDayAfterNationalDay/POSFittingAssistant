package dev.twoc.posfa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

public class POSAttributesGetter {

    public static final OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build();

    public static void main(String[] args) {
        JsonArray ids = getMarketGroups();
        CountDownLatch count = new CountDownLatch(ids.size());//use this to wait until all async link finish.

        for (JsonElement element : ids) {
            getMarketGroup(element.getAsInt(), new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    getMarketGroup(element.getAsInt(), this);//if failed, retry
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    count.countDown();
                    System.out.println(count.getCount());
                }
            });
        }

        try {
            count.await();//wait until count down to zero.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static JsonArray getMarketGroups() {
        return JsonParser.parseString(getResponseSync("https://esi.evetech.net/latest/markets/groups/?datasource=tranquility")).getAsJsonArray();
    }

    public static void getMarketGroup(int id, Callback callback) {
        getResponseAsync("https://esi.evetech.net/latest/markets/groups/" + id + "/?datasource=tranquility&language=en", callback);
    }

    public static String getResponseSync(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response;
        ResponseBody body;
        try {
            response = client.newCall(request).execute();
            if (response.code() != HTTP_OK) {
                return getResponseSync(url);//if failed, retry
            }

            body = response.body();
            if (body != null) {
                return body.string();
            } else {
                return "";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return getResponseSync(url);//retry
        }
    }

    public static void getResponseAsync(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(callback);
    }

}

package com.bytedance.androidcamp.network.demo;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.bytedance.androidcamp.network.demo.model.Cat;
import com.bytedance.androidcamp.network.demo.newtork.ICatService;
import com.bytedance.androidcamp.network.demo.utils.NetworkUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static String CAT_JSON =
            "{\"breeds\":[],\"id\":\"293\",\"url\":\"https://cdn2.thecatapi.com/images/293.jpg\",\"width\":429,\"height\":500}";

    private Retrofit retrofit;
    private ICatService catService;
    private Gson gson;

    public TextView mTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTv = findViewById(R.id.tv);
    }

    public void testJSONObject(View view) {
        // TODO 1: Parse CAT_JSON using JSONObject
        String id;
        try {
            JSONObject object = new JSONObject(CAT_JSON);
            id = object.getString("id");
        }
        catch (JSONException e)
        {
            id = "Error";
        }

        mTv.setText(id);
    }

    public void testGson(View view) {
        // TODO 2: Parse CAT_JSON using Gson
        Cat cat = getGson().fromJson(CAT_JSON, Cat.class);
        String url = cat.getUrl();
        mTv.setText(url);

    }

    @SuppressLint("StaticFieldLeak")
    public void testHttpURLConnectionSync(View view) {
        // TODO 4: Fix crash of NetworkOnMainThreadException
        new AsyncTask<String,Integer,String>()
        {
            @Override
            protected String doInBackground(String... strings) {
                return NetworkUtils.getResponseWithHttpURLConnection(ICatService.HOST + ICatService.PATH);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                mTv.setText(s);
            }
        }.execute();

    }

    public void testRetrofitSync(View view) throws Exception {
        // TODO 5: Making request in retrofit
        new Thread()
        {
            @Override
            public void run() {
                try {
                    final String s;
                    Cat cat = null;
                    Response<List<Cat>> response = getCatService().randomCat(1).execute();
                    cat = response.body().get(0);
                    if (cat != null)
                    {
                        s = cat.toString();
                    }
                    else
                    {
                        s = "Cat Null";
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTv.setText(s);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }

    public void testUpdateUI(View view) {
        // TODO 6: Fix crash of CalledFromWrongThreadException
        new Thread() {
            @Override public void run() {
                final String s = NetworkUtils.getResponseWithHttpURLConnection(ICatService.HOST + ICatService.PATH);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTv.setText(s);
                    }
                });

            }
        }.start();
    }

    public void testHttpURLConnectionAsync(View view) {
        // HttpURLConnection Async
        new Thread() {
            @Override public void run() {
                final String s = NetworkUtils.getResponseWithHttpURLConnection(ICatService.HOST + ICatService.PATH);
                try {
                    JSONArray cats = new JSONArray(s);
                    JSONObject cat = cats.getJSONObject(0);
                    final String id = cat.getString("id");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTv.setText(id);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "retrofit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }

    public void testRetrofitAsync(View view) {
        Call<List<Cat>> call = getCatService().randomCat(1);
        call.enqueue(new Callback<List<Cat>>() {
            @Override
            public void onResponse(Call<List<Cat>> call, Response<List<Cat>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Cat> cats = response.body();
                    mTv.setText(cats.get(0).getUrl());
                }
            }

            @Override
            public void onFailure(Call<List<Cat>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "retrofit: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    private ICatService getCatService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(ICatService.HOST)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        if (catService == null) {
            catService = retrofit.create(ICatService.class);
        }
        return catService;
    }
}
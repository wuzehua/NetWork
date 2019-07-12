package com.bytedance.androidcamp.network.dou;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bytedance.androidcamp.network.dou.api.IMiniDouyinService;
import com.bytedance.androidcamp.network.dou.model.GetResponse;
import com.bytedance.androidcamp.network.dou.model.PostResponse;
import com.bytedance.androidcamp.network.dou.model.Video;
import com.bytedance.androidcamp.network.dou.util.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;
    private static final String TAG = "MainActivity";
    private RecyclerView mRv;
    private List<Video> mVideos = new ArrayList<>();
    public Uri mSelectedImage;
    private Uri mSelectedVideo;
    public Button mBtn;
    private Button mBtnRefresh;

    // TODO 8: initialize retrofit & miniDouyinService
    private Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl(IMiniDouyinService.BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
    private IMiniDouyinService miniDouyinService = retrofit.create(IMiniDouyinService.class);

    private IMiniDouyinService getMiniDouyinService()
    {
        if(retrofit == null)
        {
            retrofit = new Retrofit.Builder()
                        .baseUrl(IMiniDouyinService.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
        }
        if(miniDouyinService == null)
        {
            miniDouyinService = retrofit.create(IMiniDouyinService.class);
        }

        return miniDouyinService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
        initBtns();
    }

    private void initBtns() {
        mBtn = findViewById(R.id.btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = mBtn.getText().toString();
                if (getString(R.string.select_an_image).equals(s)) {
                    chooseImage();
                } else if (getString(R.string.select_a_video).equals(s)) {
                    chooseVideo();
                } else if (getString(R.string.post_it).equals(s)) {
                    if (mSelectedVideo != null && mSelectedImage != null) {
                        postVideo();
                    } else {
                        throw new IllegalArgumentException("error data uri, mSelectedVideo = "
                                + mSelectedVideo
                                + ", mSelectedImage = "
                                + mSelectedImage);
                    }
                } else if ((getString(R.string.success_try_refresh).equals(s))) {
                    mBtn.setText(R.string.select_an_image);
                }
            }
        });

        mBtnRefresh = findViewById(R.id.btn_refresh);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
        }

        public void bind(final Activity activity, final Video video) {
            Glide.with(activity).load(video.getImageUrl()).into(img);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VideoActivity.launch(activity, video.getVideoUrl());
                }
            });
        }
    }

    private void initRecyclerView() {
        mRv = findViewById(R.id.rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mRv.setAdapter(new RecyclerView.Adapter<MyViewHolder>() {
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                return new MyViewHolder(
                        LayoutInflater.from(MainActivity.this)
                                .inflate(R.layout.video_item_view, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int i) {
                final Video video = mVideos.get(i);
                viewHolder.bind(MainActivity.this, video);
            }

            @Override
            public int getItemCount() {
                return mVideos.size();
            }
        });
    }

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE);
    }

    public void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = ["
                + requestCode
                + "], resultCode = ["
                + resultCode
                + "], data = ["
                + data
                + "]");

        if (resultCode == RESULT_OK && null != data) {
            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                mBtn.setText(R.string.select_a_video);
            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
                mBtn.setText(R.string.post_it);
            }
        }
    }

    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        File f = new File(ResourceUtils.getRealPath(MainActivity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }


    private void postVideo() {
        mBtn.setText("POSTING...");
        mBtn.setEnabled(false);
        final MultipartBody.Part coverImagePart = getMultipartFromUri("cover_image", mSelectedImage);
        final MultipartBody.Part videoPart = getMultipartFromUri("video", mSelectedVideo);
        // TODO 9: post video & update buttons
        Call<PostResponse> call = getMiniDouyinService().postVideo("123456","Rain",coverImagePart,videoPart);
        call.enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if(response.body() != null && response.body().getSuccess())
                {
                    mBtn.setEnabled(true);
                    mBtn.setText(R.string.select_an_image);
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                mBtn.setEnabled(true);
                mBtn.setText(R.string.select_an_image);
                Toast.makeText(MainActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }


    public void fetchFeed(View view) {
        mBtnRefresh.setText("requesting...");
        mBtnRefresh.setEnabled(false);
        // TODO 10: get videos & update recycler list
        Call<GetResponse> call = getMiniDouyinService().getVideos();
        call.enqueue(new Callback<GetResponse>() {
            @Override
            public void onResponse(Call<GetResponse> call, Response<GetResponse> response) {
                if(response.body() != null && response.body().getSuccess())
                {
                    mBtnRefresh.setEnabled(true);
                    mBtnRefresh.setText(R.string.refresh_feed);
                    mVideos = response.body().getVideos();
                    initRecyclerView();
                }
            }

            @Override
            public void onFailure(Call<GetResponse> call, Throwable t) {
                mBtnRefresh.setEnabled(true);
                mBtnRefresh.setText(R.string.refresh_feed);
                Toast.makeText(MainActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });


    }
}

package com.foobnix.pdf.search.activity;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.MagicHelper;


public class ImagePageFragment extends Fragment {
    public static final String POS = "pos";
    public static final String PAGE_PATH = "pagePath";
    public static final String IS_TEXTFORMAT = "isTEXT";
    public static volatile int count = 0;
    int page;
    Handler handler;

    long lifeTime = 0;
    int loadImageId;
    boolean fistTime = true;
    private PageImaveView image;
    private TextView text;
    Runnable callback = new Runnable() {

        @Override
        public void run() {
            if (!isDetached()) {
                loadImageGlide();
            } else {
                LOG.d("Image page is detached");
            }
        }

    };

    public String getPath() {
        return getArguments().getString(PAGE_PATH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.page_n, container, false);

        page = getArguments().getInt(POS);


        text = (TextView) view.findViewById(R.id.text1);
        image = (PageImaveView) view.findViewById(R.id.myImage1);

        image.setPageNumber(page);
        text.setText(getString(R.string.page) + " " + (page + 1));

        text.setTextColor(MagicHelper.getTextColor());

        handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                LOG.d("ImagePageFragment1  run ", page, "getPriority", getPriority(), "counter", count);
                if (isVisible()) {
                    if (0 == getPriority()) {
                        handler.post(callback);
                    } else if (1 == getPriority()) {
                        handler.postDelayed(callback, 200);
                    } else {
                        handler.postDelayed(callback, getPriority() * 200);
                    }
                }

            }
        }, 150);
        lifeTime = System.currentTimeMillis();


        return view;
    }
    SimpleTarget<Bitmap> target = null;

    public void loadImageGlide() {
        target = new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                text.setVisibility(View.GONE);
                if (resource != null && image != null) {
                    image.addBitmap(resource);
                }
            }
        };
        LOG.d("Glide-load-into", getActivity());
        IMG.with(getActivity())
                .asBitmap()
                .load(getPath())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        LOG.d("listener-onLoadFailed", page, isFirstResource);
                        if (fistTime) {
                            text.setVisibility(View.VISIBLE);
                            loadImageGlide();
                            fistTime = false;
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        LOG.d("listener-onResourceReady", page, resource);

                        target.onResourceReady(resource, null);
                        return true;
                    }
                })
                .into(target);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (image != null) {
            image.clickUtils.init();
            float[] values = new float[10];
            image.imageMatrix().getValues(values);
            if (values[Matrix.MSCALE_X] == 0) {
                PageImageState.get().isAutoFit = true;
                image.autoFit();
                LOG.d("fonResume-autofit", page);
            }
        }
    }

    public int getPriority() {
        return Math.min(Math.abs(PageImageState.currentPage - page), 10);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LOG.d("ImagePageFragment1 onDestroyView ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        // ImageLoader.getInstance().cancelDisplayTaskForID(loadImageId);
//        IMG.clear(getActivity(), target);


        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        image = null;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(Build.VERSION.SDK_INT >= 17 && !getActivity().isDestroyed()) {
            IMG.clear(getActivity(), target);
        }

        LOG.d("ImagePageFragment1 onDetach ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        image = null;
    }

}

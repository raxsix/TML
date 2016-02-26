
package eu.raxsix.tml.network;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import eu.raxsix.tml.application.MyApplication;


public class VolleySingleton {

    public static final String TAG = VolleySingleton.class.getSimpleName();

    // Reference to class object
    private static VolleySingleton sInstance = null;

    private RequestQueue mRequestQueue;

    private ImageLoader mImageLoader;

    // We do not want to other classes to use constructor, we make it private
    private VolleySingleton() {

        // NB A key concept is that the RequestQueue must be instantiated with the Application context,
        // not an Activity context. This ensures that the RequestQueue will last for the lifetime of your app,
        // instead of being recreated every time the activity is recreated (for example,
        // when the user rotates the device).
        mRequestQueue = Volley.newRequestQueue(MyApplication.getAppContext());


        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {

            private LruCache<String, Bitmap> cache = new LruCache<>((int) Runtime.getRuntime().maxMemory() / 1024 / 8);


            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }

    public static VolleySingleton getInstance() {

        if (sInstance == null) {

            sInstance = new VolleySingleton();
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue() {

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }
//
//    public <T> void addToRequestQueue(Request<T> req) {
//        req.setTag(TAG);
//        getRequestQueue().add(req);
//    }
//
//    public void cancelPendingRequests(Object tag) {
//        if (mRequestQueue != null) {
//            mRequestQueue.cancelAll(tag);
//        }
//    }
//
//    public ImageLoader getImageLoader() {
//
//        return mImageLoader;
//    }
}


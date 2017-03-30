package xyz.nobu.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by nobu on 3/29/17.
 */

public class ThumbnailDownloader<T> extends HandlerThread {

    /**
     * MESSAGE_DOWNLOAD will be used to identify messages as download
     * requests. (ThumbnailDownloader will set this as the "what" on
     * any new download messages it creates.
     *
     * mRequestHandler will store a reference to the Handler responsible
     * for queueing download requests as messages onto the ThumbnailDownloader
     * background thread. This handler will also be in charge of processing
     * download request messages when they are pulled off the queue.
     *
     * The mRequestMap variable is a ConcurrentHashMap, a thread-safe
     * version of HashMap. Here, using a download request's identifying
     * objects of type T as a key, you can store and retrieve the URL
     * associated with a particular request. (In this case, the identifying
     * object is a PhotoHolder, so the request response can be easily routed
     * back to the UI element where the downloaded image should be placed.
     */

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    /**
     * Using this listener delegates the responsibility of what to do with
     * the downloaded image to a class other than ThumbnailDownloader (in
     * this case, to PhotoGalleryFragment). Doing so separates the downloading
     * task from the UI updating task (putting the images into ImageViews),
     * so that ThumbnailDownloader could be used for downloading into other
     * kinds of View objects as needed.
     */
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    /**
     * HandlerThread.onLooperPrepared() is called before the Looper checks
     * the queue for the first time. This makes it a good place to create
     * your Handler implementation.
     */
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            /**
             * The new message represents a download request for the
             * specified T target (a PhotoHolder from the RecyclerView).
             * Recall that PhotoGalleryFragment's RecyclerView's adapter
             * implementation calls queueThumbnail(...) from onBindView-
             * Holder(...), passing along the PhotoHolder the image is
             * being downloaded for and the URL location of the image to
             * download.
             * Notice that the message itself does not include the URL.
             * Instead you update mRequestMap with a mapping between
             * the request identifier(PhotoHolder) and the URL for the
             * request. Later you will pull the URL from mRequestMap
             * to ensure that you are always downloading the most recently
             * requested URL for a given PhotoHolder instance. (This
             * is important because ViewHolder objects in RecyclerView
             * are recycled and reused.)
             */
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    /**
     * The handlerRequest() method is a helper method where the downloading
     * happens. Here you check for the existence of a URL. Then you pass the
     * URL to a new instance of your FlickrFetchr.
     * @param target
     */
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            /**
             * You could send a custom Message back to the main thread requesting
             * to add the image to the UI, similar to how you queued a request
             * on the background thread to download the image. However, this
             * would require another subclass of Handler, with an override of
             * handleMessage(...). Instead, let's use another handy Handler method
             * post(Runnable).
             * Handler.post(Runnable) is a convenience method for posting Messages
             * that look like this:
             *
             * Runnable myRunnable = new Runnable() {
             *     @Override
             *     public void run() {
             *         // Your code here
             *     }
             * };
             *
             * Message m = mHandler.obtainMessage();
             * m.callback = myRunnable;
             *
             * When a Message has its callback field set, it is not routed to its
             * target Handler when pullet off the message queue. Instead, the
             * run() method of the Runnable stored in callback is executed directly.
             *
             * Because mResponseHandler is associated with the main thread's
             * Looper all of the code inside of run() will be executed on the
             * main thread.
             */
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url || mHasQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}

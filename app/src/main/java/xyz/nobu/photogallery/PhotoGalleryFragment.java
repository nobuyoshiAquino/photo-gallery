package xyz.nobu.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nobu on 3/24/17.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * You retained the fragment so that roation does not repeatedly
         * fire off new AsyncTasks to fetch the JSON data.
         */
        setRetainInstance(true);

        /**
         * The call to execute() will start you AsyncTask, which will
         * then fire up its background thread and call doInBackground(...)
         */
        new FetchItemsTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    /**
     * The easiest way to work with a background thread is with a utility
     * class called AsyncTask, which creates a background thread for you
     * and runs the code in the doInBackground(...) method on the thread.
     *
     * The first type parameter allows you to specify the type of input
     * parameters you will pass to execute(), which in turn dictates the
     * type of input parameters doInBackground(...) will receive.
     *
     * The second type parameter allows you to specify the type for
     * sending progress updates.
     * Progress updates usually happen in the middle of an ongoing background
     * process. The problem is that you cannot make the necessary UI updates
     * inside the background process. So AsyncTask provides publishProgress(...)
     * and onProgressUpdate(...).
     * You call publishProgress(...) from doInBackground(...) in the back-
     * -ground thread. This will make onProgressUpdate(...) be called on the
     * UI thread. So you can do your UI updates in onProgressUpdate(...),
     * but control them from doInBackground(...) with publishProgress(...).
     *
     * The third parameter is the type of result produced by your AsyncTask.
     * It sets the type of value returned by doInBackground(...) as well
     * as the type of onPostExecute(...)'s input parameter.
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
}

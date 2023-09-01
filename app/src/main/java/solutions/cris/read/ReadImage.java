package solutions.cris.read;

// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;

import solutions.cris.object.Image;

import static androidx.core.content.FileProvider.getUriForFile;

//        CRIS - Client Record Information System
//        Copyright (C) 2018  Chris Tyler, CRIS.Solutions
//
//        This program is free software: you can redistribute it and/or modify
//        it under the terms of the GNU General Public License as published by
//        the Free Software Foundation, either version 3 of the License, or
//        (at your option) any later version.
//
//        This program is distributed in the hope that it will be useful,
//        but WITHOUT ANY WARRANTY; without even the implied warranty of
//        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//        GNU General Public License for more details.
//
//        You should have received a copy of the GNU General Public License
//        along with this program.  If not, see <https://www.gnu.org/licenses/>.

public class ReadImage extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Image imageDocument;
    private ImageView imageView;
    private File imageFile;
    private View parent;
    private Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.read_image, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        imageDocument = (Image) ((ListActivity) getActivity()).getDocument();
        client = ((ListActivity) getActivity()).getClient();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(imageDocument.getSummaryLine1());

        LocalDB localDB = LocalDB.getInstance();

        imageView = (ImageView) parent.findViewById(R.id.image);

        loadImage();
    }

    // MENU BLOCK
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //SHARE
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        createShareActionProvider(shareOption);
    }

    // SHARE MENU ITEM (Both methods are required)
    private void createShareActionProvider(MenuItem menuItem) {
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Uri uri = getUriForFile(getActivity(), "solutions.cris.fileprovider", imageFile);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareActionProvider.setShareIntent(shareIntent);
    }

    public void loadImage() {
        LocalDB localDB = LocalDB.getInstance();
        // Get the blob
        byte[] buffer = localDB.getBlob(imageDocument.getBlobID());
        // Clean up any existing pdfs in the local directory
        // Note: because createTempFile is used this step should not be necessary. However,
        // no harm in removing them anyway
        File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        });
        for (File file:files){
            // Nothing we can do if the delete fails
            file.delete();
        }
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "CRIS_" + timeStamp + ".jpg";
            imageFile = new File(dir, imageFileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(buffer);
            bos.flush();
            bos.close();
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath(), bmOptions);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            throw new CRISException("Error writing Image to directory: " + ex.getMessage());
        }

    }

}

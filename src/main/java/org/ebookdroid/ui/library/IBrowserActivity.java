package org.ebookdroid.ui.library;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.ui.library.adapters.BookShelfAdapter;
import org.emdev.common.filesystem.FileSystemScanner;

import java.io.File;

public interface IBrowserActivity extends FileSystemScanner.ProgressListener {

	Context getContext();

	Activity getActivity();

	void setCurrentDir(File newDir);

	void showDocument(Uri uri, Bookmark b);

	void showDocument(Uri uri, Bookmark b, BookShelfAdapter adapter);

	void loadThumbnail(String path, ImageView imageView, int defaultResID);
}

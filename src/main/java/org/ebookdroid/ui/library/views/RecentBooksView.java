package org.ebookdroid.ui.library.views;

import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BookShelfAdapter;

import java.io.File;

public class RecentBooksView extends android.widget.ListView implements AdapterView.OnItemClickListener {
	protected final IBrowserActivity base;
	protected final BookShelfAdapter adapter;

	public RecentBooksView(final IBrowserActivity base, final BookShelfAdapter adapter) {
		super(base.getContext());

		this.base = base;
		this.adapter = adapter;

		setAdapter(adapter);
		setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
		final BookNode bs = (BookNode) adapter.getItem(i);
		base.showDocument(Uri.fromFile(new File(bs.path)), null, adapter);
	}
}

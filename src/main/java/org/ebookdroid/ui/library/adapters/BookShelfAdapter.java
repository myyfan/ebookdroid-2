package org.ebookdroid.ui.library.adapters;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.ebookdroid.R;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.utils.FileUtils;
import org.emdev.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class BookShelfAdapter extends BaseAdapter {

	private final IBrowserActivity base;
	private final IdentityHashMap<DataSetObserver, DataSetObserver> observers = new IdentityHashMap<DataSetObserver, DataSetObserver>();

	public final int id;
	public final String name;
	public final String path;
	public final String mpath;

	final List<BookNode> nodes = new ArrayList<BookNode>();
	public boolean measuring = false;

	public BookShelfAdapter(final IBrowserActivity base, final int index, final String name, final String path) {
		this.base = base;
		this.id = index;
		this.name = name;
		this.path = path;
		this.mpath = FileUtils.invertMountPrefix(path);
	}

	@Override
	public int getCount() {
		return nodes.size();
	}

	@Override
	public Object getItem(final int position) {
		return nodes.get(position);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public View getView(final int position, final View view, final ViewGroup parent) {
		final BookNode node = nodes.get(position);
		BaseViewHolder holder;
		if (LibSettings.current().useBookcase) {
			ShelfViewHolder h = BaseViewHolder.getOrCreateViewHolder(ShelfViewHolder.class, R.layout.thumbnail, view,
					parent);
			holder = h;

			if (!measuring) {
				h.textView.setText(StringUtils.cleanupTitle(node.name));
				base.loadThumbnail(node.path, h.imageView, R.drawable.recent_item_book);
			}
		} else {
			ListViewHolder h = BaseViewHolder.getOrCreateViewHolder(ListViewHolder.class, R.layout.recentitem, view,
					parent);
			holder = h;

			final File file = new File(node.path);

			h.name.setText(file.getName());

			base.loadThumbnail(node.path, h.imageView, R.drawable.recent_item_book);

			h.info.setText(FileUtils.getFileDate(file.lastModified()));
			h.fileSize.setText(FileUtils.getFileSize(file.length()));
		}
		return holder.getView();
	}

	public String getPath() {
		return path;
	}

	@Override
	public void registerDataSetObserver(final DataSetObserver observer) {
		if (!observers.containsKey(observer)) {
			super.registerDataSetObserver(observer);
			observers.put(observer, observer);
		}
	}

	@Override
	public void unregisterDataSetObserver(final DataSetObserver observer) {
		if (null != observers.remove(observer)) {
			super.unregisterDataSetObserver(observer);
		}
	}

	public static class ShelfViewHolder extends BaseViewHolder {
		ImageView imageView;
		TextView textView;

		@Override
		public void init(final View convertView) {
			super.init(convertView);
			this.imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
			this.textView = (TextView) convertView.findViewById(R.id.thumbnailText);
		}
	}

	public static class ListViewHolder extends BaseViewHolder {
		TextView name;
		ImageView imageView;
		TextView info;
		TextView fileSize;

		@Override
		public void init(final View convertView) {
			super.init(convertView);
			name = (TextView) convertView.findViewById(R.id.recentItemName);
			imageView = (ImageView) convertView.findViewById(R.id.recentItemIcon);
			info = (TextView) convertView.findViewById(R.id.recentItemInfo);
			fileSize = (TextView) convertView.findViewById(R.id.recentItemfileSize);
		}
	}
}

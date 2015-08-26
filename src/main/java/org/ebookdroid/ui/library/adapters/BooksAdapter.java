package org.ebookdroid.ui.library.adapters;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import org.ebookdroid.R;
import org.ebookdroid.common.notifications.INotificationManager;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.views.BookshelfView;
import org.ebookdroid.ui.library.views.RecentBooksView;
import org.emdev.common.filesystem.FileSystemScanner;
import org.emdev.common.filesystem.MediaManager;
import org.emdev.common.settings.base.BooleanPreferenceDefinition;
import org.emdev.ui.tasks.AsyncTask;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;
import org.emdev.utils.collections.SparseArrayEx;
import org.emdev.utils.collections.TLIterator;

import java.io.File;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class BooksAdapter extends PagerAdapter implements FileSystemScanner.Listener, Iterable<BookShelfAdapter> {
	public final static int SERVICE_SHELVES = 2;
	public static final int RECENT_INDEX = 0;
	public static final int SEARCH_INDEX = 1;

	private final static AtomicInteger SEQ = new AtomicInteger(SERVICE_SHELVES);

	public final static BooleanPreferenceDefinition SEARCH_PAUSED =
			new BooleanPreferenceDefinition(R.string.pref_internal_searchpaused_id,
				R.string.pref_internal_searchpaused_defvalue);

	final IBrowserActivity base;
	final SparseArrayEx<BookShelfAdapter> data = new SparseArrayEx<BookShelfAdapter>();
	final TreeMap<String, BookShelfAdapter> folders = new TreeMap<String, BookShelfAdapter>();

	private final RecentAdapter recent;
	private final FileSystemScanner scanner;
	private final List<DataSetObserver> _dsoList = new ArrayList<DataSetObserver>();
	private final Activity activity;
	private String searchQuery;
	private SearchTask searchTask;

	public BooksAdapter(final IBrowserActivity base, final RecentAdapter adapter,
						Activity activity) {
		this.base = base;
		this.recent = adapter;
		this.activity = activity;
		this.recent.registerDataSetObserver(new RecentUpdater());

		this.scanner = new FileSystemScanner(base.getActivity(), this);
		this.scanner.addListener(this);
		this.scanner.addListener(base);

		this.searchQuery = LibSettings.current().searchBookQuery;
	}

	public void onDestroy() {
		scanner.shutdown();
		data.clear();
		folders.clear();
		if (searchTask != null) searchTask.cancel();
	}

	@Override
	public void destroyItem(final View collection, final int position, final Object view) {
		((ViewPager) collection).removeView((View) view);
		((View) view).destroyDrawingCache();
	}

	@Override
	public TLIterator<BookShelfAdapter> iterator() {
		return data.iterator();
	}

	@Override
	public int getCount() {
		return getListCount();
	}

	@Override
	public Object instantiateItem(final ViewGroup container, final int position) {
		View view;
		if (LibSettings.current().useBookcase) {
			view = new BookshelfView(base, container, getList(position));
		} else {
			view = new RecentBooksView(base, getList(position));
		}
		container.addView(view, 0);
		return view;
	}

	@Override
	public boolean isViewFromObject(final View arg0, final Object arg1) {
		return arg0.equals(arg1);
	}

	@Override
	public void restoreState(final Parcelable arg0, final ClassLoader arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	private void addShelf(final BookShelfAdapter a) {
		data.append(a.id, a);
		folders.put(a.path, a);
		if (a.mpath != null) {
			folders.put(a.mpath, a);
		}
	}

	private void removeShelf(final BookShelfAdapter a) {
		data.remove(a.id);
		folders.remove(a.path);
		if (a.mpath != null) {
			folders.remove(a.mpath);
		}
	}

	public synchronized BookShelfAdapter getShelf(final String path) {
		final BookShelfAdapter a = folders.get(path);
		if (a != null) {
			return a;
		}
		final String mpath = FileUtils.invertMountPrefix(path);
		return mpath != null ? folders.get(path) : null;
	}

	public synchronized int getShelfPosition(final BookShelfAdapter shelf) {
		checkServiceAdapters();
		return data.indexOfValue(shelf);
	}

	public synchronized BookShelfAdapter getList(final int index) {
		return data.valueAt(index);
	}

	public synchronized int getListCount() {
		return data.size();
	}

	public synchronized int getListCount(final int currentList) {
		checkServiceAdapters();
		if (0 <= currentList && currentList < data.size()) {
			return getList(currentList).nodes.size();
		}
		return 0;
	}

	public String getListName(final int currentList) {
		checkServiceAdapters();
		final BookShelfAdapter list = getList(currentList);
		return list != null ? LengthUtils.safeString(list.name) : "";
	}

	public String getListPath(final int currentList) {
		checkServiceAdapters();
		final BookShelfAdapter list = getList(currentList);
		return list != null ? LengthUtils.safeString(list.path) : "";
	}

	public synchronized List<String> getListNames() {
		checkServiceAdapters();

		final int size = data.size();

		if (size == 0) {
			return null;
		}

		final List<String> result = new ArrayList<String>(data.size());
		for (int index = 0; index < size; index++) {
			final BookShelfAdapter a = data.valueAt(index);
			result.add(a.name);
		}
		return result;
	}

	public synchronized List<String> getListPaths() {
		checkServiceAdapters();

		final int size = data.size();

		if (size == 0) {
			return null;
		}

		final List<String> result = new ArrayList<String>(data.size());
		for (int index = 0; index < size; index++) {
			final BookShelfAdapter a = data.valueAt(index);
			result.add(a.path);
		}
		return result;
	}

	public synchronized BookNode getItem(final int currentList, final int position) {
		checkServiceAdapters();
		if (0 <= currentList && currentList < data.size()) {
			return getList(currentList).nodes.get(position);
		}
		throw new RuntimeException("Wrong list id: " + currentList + "/" + data.size());
	}

	public long getItemId(final int position) {
		return position;
	}

	public synchronized void clearData() {
		final BookShelfAdapter[] service = new BookShelfAdapter[SERVICE_SHELVES];
		for (int i = 0; i < service.length; i++) {
			service[i] = data.get(i);
		}

		data.clear();
		folders.clear();
		SEQ.set(SERVICE_SHELVES);

		for (int i = 0; i < service.length; i++) {
			if (service[i] != null) {
				data.append(i, service[i]);
			} else {
				getService(i);
			}
		}

		notifyDataSetChanged();
	}

	public synchronized void clearSearch() {
		final BookShelfAdapter search = getService(SEARCH_INDEX);
		search.nodes.clear();
		search.notifyDataSetChanged();
		SettingsManager.clearSearch();
	}

	protected synchronized void checkServiceAdapters() {
		for (int i = 0; i < SERVICE_SHELVES; i++) {
			getService(i);
		}
	}

	protected synchronized BookShelfAdapter getService(final int index) {
		BookShelfAdapter a = data.get(index);
		if (a == null) {
			switch (index) {
				case RECENT_INDEX:
					a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.recent_title), "");
					break;
				case SEARCH_INDEX:
					a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.search_results_title), "");
					// Restore last search.
					Map<String, BookSettings> d = SettingsManager.getSearchFound();
					if (d != null) {
						for (BookSettings bs : d.values()) {
							a.nodes.add(new BookNode(bs));
						}
						Collections.sort(a.nodes);
						a.notifyDataSetChanged();
					}
					break;
			}
			if (a != null) {
				data.append(index, a);
			}
		}
		return a;
	}

	public void startScan() {
		clearData();
		final LibSettings libSettings = LibSettings.current();
		final Set<String> folders = new LinkedHashSet<String>(libSettings.autoScanDirs);
		if (libSettings.autoScanRemovableMedia) {
			folders.addAll(MediaManager.getReadableMedia());
		}
		scanner.startScan(libSettings.allowedFileTypes, folders);
	}

	public void startScan(final String path) {
		final LibSettings libSettings = LibSettings.current();
		scanner.startScan(libSettings.allowedFileTypes, path);
	}

	public void startScan(final Collection<String> paths) {
		final LibSettings libSettings = LibSettings.current();
		scanner.startScan(libSettings.allowedFileTypes, paths);
	}

	public void stopScan() {
		scanner.stopScan();
	}

	public synchronized void removeAll(final Collection<String> paths) {
		boolean found = false;
		for (final String path : paths) {
			scanner.stopObservers(path);
			found |= removeAllImpl(path);
		}
		if (found) {
			notifyDataSetChanged();
		}
	}

	public synchronized void removeAll(final String path) {
		scanner.stopObservers(path);
		if (removeAllImpl(path)) {
			notifyDataSetChanged();
		}
	}

	private boolean removeAllImpl(final String path) {
		final String ap = path + "/";
		final TLIterator<BookShelfAdapter> iter = data.iterator();
		boolean found = false;
		while (iter.hasNext()) {
			final BookShelfAdapter next = iter.next();
			final boolean eq = next.path.startsWith(ap) || next.path.equals(path) || next.mpath != null
					&& (next.mpath.startsWith(ap) || next.mpath.equals(path));
			if (eq) {
				folders.remove(next.path);
				if (next.mpath != null) {
					folders.remove(next.mpath);
				}
				iter.remove();
				found = true;
			}
		}
		iter.release();
		return found;
	}

	public boolean startSearch(final String searchQuery) {
		String tmp = this.searchQuery;
		this.searchQuery = LengthUtils.safeString(searchQuery).trim();
		LibSettings.updateSearchBookQuery(this.searchQuery);

		if (tmp == null || !tmp.equals(this.searchQuery))
			clearSearch();

		if (LengthUtils.isEmpty(this.searchQuery)) {
			return false;
		}

		if (!scanner.isScan()) {
			if (searchTask != null) searchTask.cancel();
			searchTask = new SearchTask();
			searchTask.execute("");
		}

		return true;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	protected synchronized void onNodesFound(final List<BookNode> nodes) {
		final BookShelfAdapter search = getService(SEARCH_INDEX);
		search.nodes.addAll(nodes);
		Collections.sort(search.nodes);
		search.notifyDataSetChanged();
	}

	@Override
	public synchronized void onFileScan(final File parent, final File[] files) {
		final String dir = parent.getAbsolutePath();
		BookShelfAdapter a = getShelf(dir);

		if (LengthUtils.isEmpty(files)) {
			if (a != null) {
				onDirDeleted(parent.getParentFile(), parent);
			}
			return;
		}

		boolean newShelf = false;
		if (a == null) {
			a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
			addShelf(a);
			newShelf = true;
		}

		final BookShelfAdapter search = getService(SEARCH_INDEX);
		boolean found = false;
		for (final File f : files) {
			BookNode node = recent.getNode(f.getAbsolutePath());
			if (node == null) {
				node = new BookNode(f, null);
			}
			a.nodes.add(node);
			if (acceptSearch(node)) {
				found = true;
				search.nodes.add(node);
			}
		}
		if (newShelf) {
			notifyDataSetChanged();
		} else {
			a.notifyDataSetChanged();
		}
		if (found) {
			Collections.sort(search.nodes);
			search.notifyDataSetChanged();
		}
	}

	@Override
	public synchronized void onFileAdded(final File parent, final File f) {
		if (f == null) {
			return;
		}

		if (!LibSettings.current().allowedFileTypes.accept(f)) {
			return;
		}

		final String dir = parent.getAbsolutePath();
		boolean newShelf = false;
		BookShelfAdapter a = getShelf(dir);

		if (a == null) {
			a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
			addShelf(a);
			newShelf = true;
		}

		BookNode node = recent.getNode(f.getAbsolutePath());
		if (node == null) {
			node = new BookNode(f, null);
		}
		a.nodes.add(node);
		Collections.sort(a.nodes);
		if (newShelf) {
			notifyDataSetChanged();
		} else {
			a.notifyDataSetChanged();
		}

		if (acceptSearch(node)) {
			final BookShelfAdapter search = getService(SEARCH_INDEX);
			search.nodes.add(node);
			Collections.sort(search.nodes);
			search.notifyDataSetChanged();
		}

		if (LibSettings.current().showNotifications) {
			INotificationManager.instance.notify(R.string.notification_file_add, f.getAbsolutePath(), null);
		}
	}

	@Override
	public synchronized void onFileDeleted(final File parent, final File f) {
		if (f == null) {
			return;
		}
		final BookShelfAdapter a = getShelf(parent.getAbsolutePath());
		if (a == null) {
			return;
		}

		final String path = f.getAbsolutePath();
		final BookShelfAdapter search = getService(SEARCH_INDEX);

		for (final Iterator<BookNode> i = a.nodes.iterator(); i.hasNext(); ) {
			final BookNode node = i.next();
			if (path.equals(node.path)) {
				i.remove();
				if (a.nodes.isEmpty()) {
					removeShelf(a);
					this.notifyDataSetChanged();
				} else {
					a.notifyDataSetChanged();
				}
				if (search.nodes.remove(node)) {
					search.notifyDataSetChanged();
				}
				if (LibSettings.current().showNotifications) {
					INotificationManager.instance.notify(R.string.notification_file_delete, f.getAbsolutePath(), null);
				}
				return;
			}
		}
	}

	@Override
	public void onDirAdded(final File parent, final File f) {
		final LibSettings libSettings = LibSettings.current();
		scanner.startScan(libSettings.allowedFileTypes, f.getAbsolutePath());
	}

	@Override
	public synchronized void onDirDeleted(final File parent, final File f) {
		final String dir = f.getAbsolutePath();
		final BookShelfAdapter a = getShelf(dir);
		if (a != null) {
			removeShelf(a);
			this.notifyDataSetChanged();
		}
	}

	protected boolean acceptSearch(final BookNode node) {
		if (LengthUtils.isEmpty(searchQuery)) {
			return false;
		}
		final String bookTitle = StringUtils.cleanupTitle(node.name).toLowerCase();
		final int pos = bookTitle.indexOf(searchQuery);
		return pos >= 0;
	}

	public void registerDataSetObserver(final DataSetObserver dataSetObserver) {
		_dsoList.add(dataSetObserver);
	}

	protected void notifyDataSetInvalidated() {
		for (final DataSetObserver dso : _dsoList) {
			dso.onInvalidated();
		}
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		for (final DataSetObserver dso : _dsoList) {
			dso.onChanged();
		}
	}

	private final class RecentUpdater extends DataSetObserver {

		@Override
		public void onChanged() {
			updateRecentBooks();
		}

		@Override
		public void onInvalidated() {
			updateRecentBooks();
		}

		private void updateRecentBooks() {
			final BookShelfAdapter ra = getService(RECENT_INDEX);
			ra.nodes.clear();
			final int count = recent.getCount();
			for (int i = 0; i < count; i++) {
				final BookNode book = recent.getItem(i);
				ra.nodes.add(book);
				final BookShelfAdapter a = getShelf(new File(book.path).getParent());
				if (a != null) {
					a.notifyDataSetInvalidated();
				}
			}
			ra.notifyDataSetChanged();
			BooksAdapter.this.notifyDataSetInvalidated();
		}
	}

	class SearchTask extends AsyncTask<String, String, Void> {
		private final BlockingQueue<BookNode> queue = new ArrayBlockingQueue<BookNode>(160, true);
		private Integer notificationId = null;
		private AtomicBoolean cancelled = new AtomicBoolean(false);

		@Override
		protected void onPreExecute() {
			base.showProgress(true);
		}

		private int getTotal() {
			int total = 0;
			int aIndex = 0;
			while (aIndex < getListCount()) {
				if (aIndex != RECENT_INDEX && aIndex != SEARCH_INDEX) {
					total += getListCount(aIndex);
				}
				aIndex++;
			}
			return total;
		}

		@Override
		protected Void doInBackground(final String... paths) {
			cancelled.set(false);
			int aIndex = SERVICE_SHELVES;
			final int total = getTotal();
			int count = 1;
			int matches = 0;
			Map<String, BookSettings> found = SettingsManager.getSearchFound();
			Map<String, BookSettings> notFound = SettingsManager.getSearchNotFound();
			while (aIndex < getListCount() && !cancelled.get()) {
				int nIndex = 0;
				while (nIndex < getListCount(aIndex)  && !cancelled.get()
						&& aIndex != RECENT_INDEX && aIndex != SEARCH_INDEX) {
					final BookNode node = getItem(aIndex, nIndex);
					publishProgress("searching "+count+" of "+total);
					if (!found.containsKey(node.path) && !notFound.containsKey(node.path)) {
						if (acceptSearch(node)) {
							queue.offer(node);
							publishProgress("");
							matches++;
							SettingsManager.storeSearch(node.path, true);
						} else {  // Did not find match in title, check the doc...
							DocSearch ds = new DocSearch(activity, node.path);
							if (ds.search(searchQuery)) {
								queue.offer(node);
								publishProgress("");
								matches++;
								SettingsManager.storeSearch(node.path, true);
							} else {
								SettingsManager.storeSearch(node.path, false);
							}
						}
					}
					nIndex++;
					count++;
				}
				aIndex++;
			}
			cancelled.set(true);
			final int m = found.values().size() + matches;
			final boolean paused = (--count != total);
			final String mes = paused?
					"Search Paused, "+m+" matches out of "+total+" books.":
					"Search Complete, "+m+" matches out of "+total+" books.";
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					notificationId = INotificationManager.instance.notify(
							notificationId, "EBookDroid Search", mes, null);
					SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();
					try {
						SEARCH_PAUSED.setPreferenceValue(edit, paused);
					} finally {
						edit.commit();
					}
				}
			});
			return null;
		}

		@Override
		protected void onProgressUpdate(final String... values) {
			final ArrayList<BookNode> nodes = new ArrayList<BookNode>();
			while (!queue.isEmpty()) {
				nodes.add(queue.poll());
			}
			if (!nodes.isEmpty()) {
				onNodesFound(nodes);
			}
			final int length = LengthUtils.length(values);
			if (length > 0 && !cancelled.get()) {
				final String last = values[length - 1];
				if (last.length() > 0) {
					notificationId = INotificationManager.instance.notify(
						notificationId, "EBookDroid Search", last, null);
				}
			}
		}

		@Override
		protected void onPostExecute(final Void v) {
			onProgressUpdate("");
			base.showProgress(false);
		}

		public void cancel() {
			cancelled.set(true);
		}
	}
}

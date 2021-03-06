package org.ebookdroid.common.settings.books;


import android.database.sqlite.SQLiteDatabase;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface IDBAdapter {

	LogContext LCTX = LogManager.root().lctx("DBAdapter", false);

	void onCreate(final SQLiteDatabase db);

	void onDestroy(final SQLiteDatabase db);

	Map<String, BookSettings> getAllBooks();

	Map<String, BookSettings> getRecentBooks(boolean all);

	BookSettings getBookSettings(String fileName);

	boolean storeBookSettings(BookSettings bs);

	boolean storeBookSettings(List<BookSettings> list);

	boolean restoreBookSettings(Collection<BookSettings> c);

	boolean clearRecent();

	void delete(BookSettings current);

	boolean deleteAll();

	boolean updateBookmarks(BookSettings book);

	boolean deleteBookmarks(String book, List<Bookmark> bookmarks);

	boolean deleteAllBookmarks();

	boolean removeBookFromRecents(BookSettings bs);

	public Map<String, BookSettings> getSearchFound();

	public Map<String, BookSettings> getSearchNotFound();

	public boolean storeSearch(final String book, final boolean found);

	public boolean clearSearch();

}

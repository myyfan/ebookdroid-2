package org.ebookdroid.common.settings.books;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

/**
 * Version 9 DB adapter.  Adds support for searching.
 * Created by sstanf on 11/12/13.
 */
public class DBAdapterV9 extends DBAdapterV8 {
	public static final int VERSION = 9;

	public static final String DB_SEARCH_CREATE = "create table search ("
			// Book file path
			+ "book varchar(1024) primary key, "
			// 1 if book matches, 0 if not.
			+ "found integer not null "
			// ...
			+ ");";

	public static final String DB_SEARCH_FOUND = "SELECT b.book, b.last_updated, "
		+"b.first_page_offset, b.doc_page, b.view_page, b.zoom, b.view_mode, "
		+"b.page_align, b.page_animation, b.flags, b.offset_x, b.offset_y, "
		+"b.contrast, b.gamma, b.exposure, b.type_specific "
		+"FROM book_settings b, search s where s.found = 1 and b.book = s.book";
	public static final String DB_SEARCH_NOT_FOUND = "SELECT b.book, b.last_updated, "
			+"b.first_page_offset, b.doc_page, b.view_page, b.zoom, b.view_mode, "
			+"b.page_align, b.page_animation, b.flags, b.offset_x, b.offset_y, "
			+"b.contrast, b.gamma, b.exposure, b.type_specific "
			+"FROM book_settings b, search s where s.found = 0 and b.book = s.book";
	public static final String DB_SEARCH_STORE = "INSERT OR REPLACE INTO search (book, found) VALUES (?, ?)";
	public static final String DB_SEARCH_DEL = "DELETE FROM search";

	public DBAdapterV9(final DBSettingsManager manager) {
		super(manager);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL(DB_BOOK_CREATE);
		db.execSQL(DB_BOOKMARK_CREATE);
		db.execSQL(DB_SEARCH_CREATE);
	}

	private void finishLastSearch(Map<String, BookSettings> map, boolean found) {
		try {
			final SQLiteDatabase db = manager.getReadableDatabase();
			try {
				final Cursor c = db.rawQuery("SELECT book FROM search WHERE found="+(found?"1":"0"), null);
				if (c != null) {
					try {
						for (boolean next = c.moveToFirst(); next; next = c.moveToNext()) {
							String path = c.getString(0);
							if (!map.containsKey(path)) {
								final BookSettings bs = new BookSettings(path);
								loadBookmarks(bs, db);
								map.put(bs.fileName, bs);
							}
						}
					} finally {
						close(c);
					}
				}
			} finally {
				manager.closeDatabase(db);
			}
		} catch (final Throwable th) {
			LCTX.e("Retrieving last search results failed: ", th);
		}
	}

	@Override
	public Map<String, BookSettings> getSearchFound() {
		Map<String, BookSettings> ret = getBookSettings(DB_SEARCH_FOUND, true);
		finishLastSearch(ret, true);
		return ret;
	}

	@Override
	public Map<String, BookSettings> getSearchNotFound() {
		Map<String, BookSettings> ret =  getBookSettings(DB_SEARCH_NOT_FOUND, true);
		finishLastSearch(ret, false);
		return ret;
	}

	@Override
	public boolean storeSearch(final String book, final boolean found) {
		try {
			final SQLiteDatabase db = manager.getWritableDatabase();
			try {
				db.beginTransaction();

				final Object[] args = new Object[]{
						// Book
						book,
						// Found
						(found?1:0),
				};

				db.execSQL(DB_SEARCH_STORE, args);

				db.setTransactionSuccessful();

				return true;
			} finally {
				endTransaction(db);
			}
		} catch (final Throwable th) {
			LCTX.e("Update search failed: ", th);
		}
		return false;
	}

	@Override
	public boolean clearSearch() {
		try {
			final SQLiteDatabase db = manager.getWritableDatabase();
			try {
				db.beginTransaction();

				db.execSQL(DB_SEARCH_DEL, new Object[]{});

				db.setTransactionSuccessful();

				return true;
			} finally {
				endTransaction(db);
			}
		} catch (final Throwable th) {
			LCTX.e("Clear search failed: ", th);
		}
		return false;
	}
}

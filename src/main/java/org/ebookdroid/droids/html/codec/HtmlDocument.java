package org.ebookdroid.droids.html.codec;

import android.graphics.RectF;
import android.os.Looper;
import android.webkit.WebView;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.ebookdroid.ui.viewer.viewers.HTMLView;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 * Allow use of HTML documents.
 * Created by sstanf on 2/11/14.
 */
public class HtmlDocument extends AbstractCodecDocument {
	private final String fileName;
	private final HtmlPage page;

	// Used for search.
	private int si = 0;
	private HTMLView webView;


	protected HtmlDocument(AbstractCodecContext context, String fileName) {
		super(context, 0);
		this.fileName = fileName;
		this.page = new HtmlPage();
	}

	@Override
	public int getPageCount() {
		return 1;
	}

	@Override
	public CodecPage getPage(int pageNumber) {
		return page;
	}

	@Override
	public CodecPageInfo getPageInfo(int pageNumber) {
		ViewerActivity va = ViewerActivity.i();
		int w = va==null?0:va.getView().getView().getWidth();
		int h = va==null?0:va.getView().getView().getHeight();
		return new CodecPageInfo(w, h);
	}

	private void setFindIsUp(final WebView webView) {
		try{
			Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
			m.invoke(webView, true);
		} catch(Exception ignored){ }
	}

	private void doSearch(final String pattern, final Semaphore s, final WebView webView) {
		si = webView.findAll(pattern);
		setFindIsUp(webView);
		if (s != null) s.release();
	}

	private void semAquire(final Semaphore s) {
		try { s.acquire(); } catch (InterruptedException ignored) { }
	}

	private boolean findInFile(final String file, final String pattern) {
		boolean ret = false;
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(file));
			scanner.useDelimiter("<.+?>");
			while (scanner.hasNext() && !ret) {
				String s = scanner.next();
				if (s.toLowerCase().contains(pattern.toLowerCase())) ret = true;
			}
		} catch (FileNotFoundException ignored) {
		} finally {
			if (scanner != null) scanner.close();
		}
		return ret;
	}

	@Override
	public List<? extends RectF> searchText(final int pageNumber, final String pattern) {
		final Semaphore s = new Semaphore(1);
		webView = null;
		if (ViewerActivity.i() != null) {
			IView temp = ViewerActivity.i().getView();
			if (temp != null && temp instanceof HTMLView) {
				webView = (HTMLView)temp;
				// If we have a webview make sure it is THIS doc.
				if (!webView.getLoadedUrl().endsWith(fileName)) webView = null;
			}
		}
		if (webView != null) {
			semAquire(s);
			if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
				doSearch(pattern, s, webView);
			} else {
				ViewerActivity.i().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						doSearch(pattern, s, webView);
					}
				});
			}
		} else {
			if (findInFile(fileName, pattern)) si = 1;
			else si = 0;
		}
		semAquire(s);
		List<RectF> res = new ArrayList<RectF>(si);
		for (int c = 0; c < si; c++) {
			res.add(new RectF());
		}
		return res;
	}

	@Override
	public void searchNext(final boolean forward) {
		if (webView != null) {
			if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
				webView.findNext(forward);
			} else {
				final Semaphore s = new Semaphore(1);
				semAquire(s);
				ViewerActivity.i().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						webView.findNext(forward);
						s.release();
					}
				});
				semAquire(s);
			}
		}
	}
}

package org.ebookdroid.ui.viewer.viewers;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

import java.util.concurrent.Semaphore;

/**
 * Wrapper to make a standard WebView work with ebookdroid.
 * Created by sstanf on 2/18/14.
 */
public class HTMLView extends WebView implements IView {
	protected final IActivityController base;
	private final LogContext LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), false);
	private Bitmap thumbnail = null;
	private final Semaphore sem = new Semaphore(1);
	private boolean layed = false;
	private boolean loaded = false;
	private String loadedUrl = "";

	private void makeThumb() {
		if (thumbnail != null) return;
		if (!layed || !loaded) return;
		thumbnail = Bitmap.createBitmap(getMeasuredWidth(),
				getMeasuredHeight(), Bitmap.Config.ARGB_8888);

		Canvas bigcanvas = new Canvas(thumbnail);
		draw(bigcanvas);
	}

	private void init() {
		try { sem.acquire(); } catch (InterruptedException e) { LCTX.e("Thread interrupted!", e); }

		setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				loaded = true;
				if (layed) sem.release();
			}
		});
	}

	public HTMLView(final IActivityController baseActivity) {
		super(baseActivity.getContext());
		base = baseActivity;
		init();
	}

	public HTMLView(final Context ctx) {
		super(ctx);
		base = null;
		init();
	}

	@Override
	public void loadUrl(String url) {
		super.loadUrl(url);
		loadedUrl = url;
	}

	public String getLoadedUrl() {
		return loadedUrl;
	}

	public void skippedLayout() {
		layed = true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.view.View#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected final void onLayout(final boolean layoutChanged, final int left, final int top, final int right,
								  final int bottom) {
		super.onLayout(layoutChanged, left, top, right, bottom);
		layed = true;
		if (loaded) sem.release();
	}

	public Bitmap getThumbnail() {
		makeThumb();
		return thumbnail;
	}

	@Override
	public View getView() {
		return this;
	}

	@Override
	public IActivityController getBase() {
		return base;
	}

	@Override
	public void invalidateScroll() {

	}

	@Override
	public void invalidateScroll(float newZoom, float oldZoom) {

	}

	@Override
	public void startPageScroll(int dx, int dy) {

	}

	@Override
	public void startFling(float vX, float vY, Rect limits) {

	}

	@Override
	public void continueScroll() {

	}

	@Override
	public void forceFinishScroll() {

	}

	@Override
	public void _scrollTo(int x, int y) {

	}

	@Override
	public void onScrollChange(int curX, int curY, int oldX, int oldY) {

	}

	@Override
	public RectF getViewRect() {
		return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
	}

	@Override
	public void changeLayoutLock(boolean lock) {

	}

	@Override
	public boolean isLayoutLocked() {
		return false;
	}

	@Override
	public void waitForInitialization() {
		try { sem.acquire(); } catch (InterruptedException e) { LCTX.e("Thread interrupted!", e); }
		sem.release();
		// Wait a second and let the webview stabilize.
		try { Thread.sleep(1000); } catch (InterruptedException e) { LCTX.e("Thread interrupted!", e); }
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public float getScrollScaleRatio() {
		return 0;
	}

	@Override
	public void stopScroller() {

	}

	@Override
	public void redrawView() {

	}

	@Override
	public void redrawView(ViewState viewState) {

	}

	@Override
	public PointF getBase(RectF viewRect) {
		return new PointF(viewRect.left, viewRect.top);
	}

	@Override
	public void checkFullScreenMode() {

	}

	@Override
	public boolean isScrollFinished() {
		return true;
	}
}

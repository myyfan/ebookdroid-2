package org.ebookdroid.core.codec;

import android.graphics.RectF;
import org.ebookdroid.common.bitmaps.IBitmapCore;
import org.ebookdroid.core.ViewState;

import java.util.List;

public interface CodecPage {

	int getWidth();

	int getHeight();

	IBitmapCore renderBitmap(ViewState viewState, int width, int height, RectF pageSliceBounds);

	List<PageLink> getPageLinks();

	List<PageTextBox> getPageText();

	List<? extends RectF> searchText(final String pattern);

	void recycle();

	boolean isRecycled();
}

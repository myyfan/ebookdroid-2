package org.ebookdroid.droids.html.codec;

import android.graphics.*;
import org.ebookdroid.common.bitmaps.BitmapCore;
import org.ebookdroid.common.bitmaps.IBitmapCore;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.ebookdroid.ui.viewer.viewers.HTMLView;

/**
 * Allow display of HTML documents (they are single page docs).
 * Created by sstanf on 2/18/14.
 */
public class HtmlPage extends AbstractCodecPage {
	@Override
	public int getWidth() {
		ViewerActivity va = ViewerActivity.i();
		return va==null?0:va.getView().getView().getWidth();
	}

	@Override
	public int getHeight() {
		ViewerActivity va = ViewerActivity.i();
		return va==null?0:va.getView().getView().getHeight();
	}

	private static Bitmap scale(Bitmap image, int targetWidth, int targetHeight) {
		// Scale by halves else your thumbnails resemble pixellated poop.
		int ih = image.getHeight();
		int iw = image.getWidth();
		int h = ih;
		int w = iw;
		if (targetHeight > h || targetWidth > w) return image;
		Bitmap ret;
		Bitmap tmp2 = image;
		do {
			if (w > targetWidth || h > targetHeight) {
				w /= 2;
				h /= 2;
				if (w < targetWidth || h < targetHeight) {
					w = targetWidth;
					h = targetHeight;
				}
			}
			Bitmap tmp = Bitmap.createScaledBitmap(tmp2, w, h, true);
			if ((tmp2 != image) && (tmp2 != tmp)) tmp2.recycle();
			ret = tmp2 = tmp;
		} while ((ret != null) && (w != targetWidth || h != targetHeight));
		return ret;
	}

	@Override
	public IBitmapCore renderBitmap(ViewState viewState, int width, int height, RectF pageSliceBounds) {
		IBitmapCore buf = null;
		ViewerActivity va = ViewerActivity.i();
		if (va != null && va.getView() != null && (va.getView() instanceof HTMLView)) {
			Bitmap thumb = ((HTMLView)va.getView()).getThumbnail();
			buf = new BitmapCore(scale(thumb, width, height));
		}
		return buf;
	}

	@Override
	public void recycle() {

	}

	@Override
	public boolean isRecycled() {
		return false;
	}
}

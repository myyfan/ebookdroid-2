package org.ebookdroid.common.bitmaps;

import org.ebookdroid.common.settings.books.BookSettings;

import java.nio.ByteBuffer;

/**
 * Allow use of native ebookdroid bitmaps OR regular Bitmaps.
 * Created by sstanf on 2/21/14.
 */
public interface IBitmapCore {
	boolean isRecycled();

	public void recycle();

	public void release();  // Nothing to see here....

	public void applyEffects(final BookSettings bs);

	public void copyPixelsFrom(final IBitmapCore src, final int left, final int top, final int width,
							   final int height);

	public ByteBuffer getPixels();

	public int getWidth();

	public int getHeight();

	public IBitmapRef toBitmap();

	public void invert();

	public void contrast(final int contrast);

	public void gamma(final int gamma);

	public void exposure(final int exposure);

	public void autoLevels();

	public void eraseColor(final int color);

}

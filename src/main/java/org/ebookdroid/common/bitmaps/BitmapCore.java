package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import org.ebookdroid.common.settings.books.BookSettings;

import java.nio.ByteBuffer;

/**
 * Wraps a normal android Bitmap to be used by ebookdroid.
 * Created by sstanf on 2/21/14.
 */
public class BitmapCore implements IBitmapCore, IBitmapRef {
	private final Bitmap b;

	public BitmapCore(Bitmap bitmap) {
		b = bitmap;
	}

	@Override
	public Canvas getCanvas() {
		return new Canvas(b);
	}

	@Override
	public Bitmap getBitmap() {
		return b;
	}

	@Override
	public boolean isRecycled() {
		return b.isRecycled();
	}

	@Override
	public void recycle() {
		b.recycle();
	}

	@Override
	public void release() {
		recycle();
	}

	@Override
	public void applyEffects(BookSettings bs) {
		// NO-OP
	}

	@Override
	public void copyPixelsFrom(IBitmapCore src, int left, int top, int width, int height) {
		// NO-OP XXX - do this?
	}

	@Override
	public ByteBuffer getPixels() {
		if (b == null) return null;
		int size     = b.getRowBytes() * b.getHeight();
		ByteBuffer ret = ByteBuffer.allocate(size);
		b.copyPixelsToBuffer(ret);
		return ret;
	}

	@Override
	public int getWidth() {
		return b.getWidth();
	}

	@Override
	public int getHeight() {
		return b.getHeight();
	}

	@Override
	public IBitmapRef toBitmap() {
		return this;
	}

	@Override
	public void invert() {
		// NO-OP
	}

	@Override
	public void contrast(int contrast) {
		// NO-OP
	}

	@Override
	public void gamma(int gamma) {
		// NO-OP
	}

	@Override
	public void exposure(int exposure) {
		// NO-OP
	}

	@Override
	public void autoLevels() {
		// NO-OP
	}

	@Override
	public void eraseColor(int color) {
		// NO-OP
	}
}

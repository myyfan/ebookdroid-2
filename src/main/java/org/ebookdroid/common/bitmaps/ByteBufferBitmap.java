package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Rect;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.definitions.AppPreferences;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ByteBufferBitmap implements IBitmapCore {

	private static final AtomicInteger SEQ = new AtomicInteger();

	public final int id = SEQ.incrementAndGet();
	final AtomicBoolean used = new AtomicBoolean(true);
	long gen;

	volatile ByteBuffer pixels;
	final int size;
	int width;
	int height;

	ByteBufferBitmap(final int width, final int height) {
		this.width = width;
		this.height = height;
		this.size = 4 * width * height;
		this.pixels = create(size).order(ByteOrder.nativeOrder());
		this.pixels.rewind();
	}

	@Override
	protected void finalize() throws Throwable {
		recycle();
		super.finalize();
	}

	@Override
	public void recycle() {
		ByteBuffer buf = pixels;
		pixels = null;
		free(buf);
	}

	@Override
	public void release() {
		ByteBufferManager.release(this);
	}

	@Override
	public boolean isRecycled() {
		return pixels == null;
	}

	public static ByteBufferBitmap get(final Bitmap bmp) {
		if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
			throw new IllegalArgumentException("Wrong bitmap config: " + bmp.getConfig());
		}
		final ByteBufferBitmap b = ByteBufferManager.getBitmap(bmp.getWidth(), bmp.getHeight());
		bmp.copyPixelsToBuffer(b.pixels);
		return b;
	}

	public static ByteBufferBitmap get(final Bitmap bitmap, final Rect srcRect) {
		final ByteBufferBitmap full = get(bitmap);

		final int srcWidth = srcRect.width();
		final int srcHeight = srcRect.height();

		if (full.width == srcWidth && full.height == srcHeight) {
			return full;
		}

		final ByteBufferBitmap part = ByteBufferManager.getBitmap(srcWidth, srcHeight);
		part.copyPixelsFrom(full, srcRect.left, srcRect.top, part.width, part.height);

		ByteBufferManager.release(full);

		return part;
	}

	@Override
	public void applyEffects(final BookSettings bs) {
		final boolean correctContrast = bs.contrast != AppPreferences.CONTRAST.defValue;
		final boolean correctGamma = bs.gamma != AppPreferences.GAMMA.defValue;
		final boolean correctExposure = bs.exposure != AppPreferences.EXPOSURE.defValue;

		if (correctContrast || correctGamma || correctExposure || bs.autoLevels) {
			if (correctGamma) {
				gamma(bs.gamma);
			}
			if (correctContrast) {
				contrast(bs.contrast);
			}
			if (correctExposure) {
				exposure(bs.exposure - AppPreferences.EXPOSURE.defValue);
			}
			if (bs.autoLevels) {
				autoLevels();
			}
		}
	}

	@Override
	public void copyPixelsFrom(final IBitmapCore src, final int left, final int top, final int width,
							   final int height) {
		if (width > this.width) {
			throw new IllegalArgumentException("width > this.width: " + width + ", " + this.width);
		}
		if (height > this.height) {
			throw new IllegalArgumentException("height > this.height: " + height + ", " + this.height);
		}
		if (left + width > src.getWidth()) {
			throw new IllegalArgumentException("left + width > src.width: " + left + ", " + width + ", " + src.getWidth());
		}
		if (top + height > src.getHeight()) {
			throw new IllegalArgumentException("top + height > src.height: " + top + ", " + height + ", " + src.getHeight());
		}
		nativeFillRect(src.getPixels(), src.getWidth(), this.pixels, this.width, left, top, width, height);
	}

	@Override
	public ByteBuffer getPixels() {
		return pixels;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public IBitmapRef toBitmap() {
		final IBitmapRef bitmap = BitmapManager.getBitmap("RawBitmap", width, height, Bitmap.Config.ARGB_8888);
		pixels.rewind();
		bitmap.getBitmap().copyPixelsFromBuffer(pixels);
		return bitmap;
	}

	@SuppressWarnings("unused")
	public void fillAlpha(final int v) {
		nativeFillAlpha(pixels, width, height, v);
	}

	@Override
	public void invert() {
		nativeInvert(pixels, width, height);
	}

	@SuppressWarnings("unused")
	public int getAvgLum() {
		return nativeAvgLum(pixels, width, height);
	}

	@Override
	public void contrast(final int contrast) {
		nativeContrast(pixels, width, height, contrast * 256 / 100);
	}

	@Override
	public void gamma(final int gamma) {
		nativeGamma(pixels, width, height, gamma);
	}

	@Override
	public void exposure(final int exposure) {
		nativeExposure(pixels, width, height, exposure * 128 / 100);
	}

	@Override
	public void autoLevels() {
		nativeAutoLevels2(pixels, width, height);
	}

	@Override
	public void eraseColor(final int color) {
		nativeEraseColor(pixels, width, height, color);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id + ", width=" + width + ", height=" + height + ", size=" + size
				+ "]";
	}

	private static native void nativeInvert(ByteBuffer src, int width, int height);

	private static native void nativeFillAlpha(ByteBuffer src, int width, int height, int value);

	private static native void nativeEraseColor(ByteBuffer src, int width, int height, int color);

	/* contrast value 256 - normal */
	private static native void nativeContrast(ByteBuffer src, int width, int height, int contrast);

	/* gamma value 100 - normal */
	private static native void nativeGamma(ByteBuffer src, int width, int height, int gamma);

	/* Exposure correction values -128...+128 */
	private static native void nativeExposure(ByteBuffer src, int width, int height, int exposure);

	@SuppressWarnings("unused")
	private static native void nativeAutoLevels(ByteBuffer src, int width, int height);

	private static native void nativeAutoLevels2(ByteBuffer src, int width, int height);

	private static native int nativeAvgLum(ByteBuffer src, int width, int height);

	private static native void nativeFillRect(ByteBuffer src, int srcWidth, ByteBuffer dst, int dstWidth, int x,
											  int y, int width, int height);

	private static native ByteBuffer create(int size);

	private static native void free(ByteBuffer buf);
}

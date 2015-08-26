package org.ebookdroid.core;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import org.ebookdroid.common.bitmaps.GLBitmaps;
import org.ebookdroid.common.bitmaps.IBitmapCore;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.ui.viewer.IActivityController;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.MathUtils;
import org.emdev.utils.MatrixUtils;

import java.util.List;

public class Page {

	static final LogContext LCTX = LogManager.root().lctx("Page", false);

	public final PageIndex index;
	public final PageType type;
	public final CodecPageInfo cpi;

	final IActivityController base;
	public final PageTree nodes;

	RectF bounds;
	int aspectRatio;
	boolean recycled;
	float storedZoom;
	RectF zoomedBounds;

	List<PageLink> links;

	public Page(final IActivityController base, final PageIndex index, final PageType pt, final CodecPageInfo cpi) {
		this.base = base;
		this.index = index;
		this.cpi = cpi;
		this.type = pt != null ? pt : PageType.FULL_PAGE;
		this.bounds = new RectF(0, 0, cpi.width / type.getWidthScale(), cpi.height);

		setAspectRatio(cpi);

		nodes = new PageTree(this);
	}

	public void recycle(final List<GLBitmaps> bitmapsToRecycle) {
		recycled = true;
		nodes.recycleAll(bitmapsToRecycle, true);
	}

	public float getAspectRatio() {
		return aspectRatio / 128.0f;
	}

	private boolean setAspectRatio(final float aspectRatio) {
		final int newAspectRatio = (int) Math.floor(aspectRatio * 128);
		if (this.aspectRatio != newAspectRatio) {
			this.aspectRatio = newAspectRatio;
			return true;
		}
		return false;
	}

	public boolean setAspectRatio(final CodecPageInfo page) {
		return page != null && this.setAspectRatio(page.width / type.getWidthScale(), page.height);
	}

	public boolean setAspectRatio(final float width, final float height) {
		return setAspectRatio(width / height);
	}

	public void setBounds(final RectF pageBounds) {
		storedZoom = 0.0f;
		zoomedBounds = null;
		bounds = pageBounds;
	}

	@SuppressWarnings("unused")
	public void setBounds(final float l, final float t, final float r, final float b) {
		if (bounds == null) {
			bounds = new RectF(l, t, r, b);
		} else {
			bounds.set(l, t, r, b);
		}
	}

	public boolean shouldCrop() {
		final BookSettings bs = base.getBookSettings();
		return nodes.root.hasManualCropping() || (bs != null && bs.cropPages);
	}

	public RectF getCropping() {
		return shouldCrop() ? nodes.root.getCropping() : null;
	}

	public RectF getCropping(PageTreeNode node) {
		return shouldCrop() ? node.getCropping() : null;
	}

	protected void updateAspectRatio() {
		final RectF cropping = getCropping();
		if (cropping != null) {
			final float pageWidth = cpi.width * cropping.width();
			final float pageHeight = cpi.height * cropping.height();
			setAspectRatio(pageWidth, pageHeight);
		} else {
			setAspectRatio(cpi);
		}
	}

	public RectF getBounds(final float zoom) {
		RectF bounds = new RectF();
		getBounds(zoom, bounds);
		return bounds;
	}

	public void getBounds(final float zoom, RectF target) {
		MathUtils.zoom(bounds, zoom, target);

		final BookSettings bs = base.getBookSettings();
		if (bs != null && bs.viewMode == DocumentViewMode.SINGLE_PAGE && bounds.left > 0) {
			target.offset((bounds.left + bounds.right) * (1 - zoom) / 2, 0);
		}
	}

	@SuppressWarnings("unused")
	public float getTargetRectScale() {
		return type.getWidthScale();
	}

	@Override
	public String toString() {
		return "Page" +
			"[" +
			"index=" + index + ", " +
			"bounds=" + bounds + ", " +
			"aspectRatio=" + aspectRatio + ", " +
			"type=" + type.name() + "]";
	}

	public static RectF getTargetRect(final PageType pageType, final RectF pageBounds, final RectF normalizedRect) {
		final Matrix tmpMatrix = MatrixUtils.get();

		tmpMatrix.postScale(pageBounds.width() * pageType.getWidthScale(), pageBounds.height());
		tmpMatrix.postTranslate(pageBounds.left - pageBounds.width() * pageType.getLeftPos(), pageBounds.top);

		final RectF targetRectF = new RectF();
		tmpMatrix.mapRect(targetRectF, normalizedRect);

		MathUtils.floor(targetRectF);

		return targetRectF;
	}

	public RectF getLinkSourceRect(final RectF pageBounds, final PageLink link) {
		if (link == null || link.sourceRect == null) {
			return null;
		}
		return getPageRegion(pageBounds, new RectF(link.sourceRect));
	}

	public RectF getPageRegion(final RectF pageBounds, final RectF sourceRect) {
		final RectF cb = getCropping();
		if (cb != null) {
			final Matrix m = MatrixUtils.get();
			final RectF psb = nodes.root.pageSliceBounds;
			m.postTranslate(psb.left - cb.left, psb.top - cb.top);
			m.postScale(psb.width() / cb.width(), psb.height() / cb.height());
			m.mapRect(sourceRect);
		}

		if (type == PageType.LEFT_PAGE && sourceRect.left >= 0.5f) {
			return null;
		}

		if (type == PageType.RIGHT_PAGE && sourceRect.right < 0.5f) {
			return null;
		}

		return getTargetRect(type, pageBounds, sourceRect);
	}

	protected RectF getColumn(final PointF pos) {
		final DecodeService ds = base.getDecodeService();
		final IBitmapCore pageImage = ds.createPageThumbnail(PageCropper.BMP_SIZE, PageCropper.BMP_SIZE,
				index.docIndex, type.getInitialRect());

		final RectF column = PageCropper.getColumn(pageImage, pos.x, pos.y);
		pageImage.release();

		return column;
	}
}

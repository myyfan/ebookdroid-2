package org.ebookdroid.droids.html.codec;

import android.graphics.RectF;
import org.ebookdroid.common.bitmaps.IBitmapCore;
import org.ebookdroid.common.bitmaps.IBitmapRef;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageTreeNode;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import java.util.List;

/**
 * Just for HTML docs...
 * Created by sstanf on 3/19/14.
 */
public class HtmlDecodeService implements DecodeService {
	private final HtmlContext codecContext;
	private HtmlDocument document;

	public HtmlDecodeService(final CodecContext codecContext) {
		// Sending in anything else is a FAIL so let it crash it here...
		this.codecContext = (HtmlContext)codecContext;
	}

	@Override
	public void open(String fileName, String password) {
		document = (HtmlDocument)codecContext.openDocument(fileName, password);
	}

	@Override
	public void decodePage(ViewState viewState, PageTreeNode node) {

	}

	@Override
	public void searchText(Page page, String pattern, SearchCallback callback) {
		List<? extends RectF> regions = document.searchText(0, pattern);
		callback.searchComplete(page, regions);
	}

	@Override
	public void searchNext(boolean forward) {
		document.searchNext(forward);
	}

	@Override
	public void stopSearch(String pattern) {

	}

	@Override
	public void stopDecoding(PageTreeNode node, String reason) {

	}

	@Override
	public int getPageCount() {
		return document.getPageCount();
	}

	@Override
	public List<OutlineLink> getOutline() {
		return document.getOutline();
	}

	@Override
	public CodecPageInfo getUnifiedPageInfo() {
		return document != null ? document.getUnifiedPageInfo() : null;
	}

	@Override
	public CodecPageInfo getPageInfo(int pageIndex) {
		return document != null ? document.getPageInfo(pageIndex) : null;
	}

	@Override
	public void recycle() {

	}

	@Override
	public void updateViewState(ViewState viewState) {

	}

	@Override
	public IBitmapRef createThumbnail(boolean useEmbeddedIfAvailable, int width, int height, int pageNo, RectF region) {
		IBitmapCore b = createPageThumbnail(width, height, pageNo, region);
		return b==null?null:b.toBitmap();
	}

	@Override
	public IBitmapCore createPageThumbnail(int width, int height, int pageNo, RectF region) {
		if (document == null) {
			return null;
		}
		final CodecPage page = document.getPage(pageNo);
		IBitmapCore retB = page.renderBitmap(null, width, height, region);
		return retB==null?null:retB;
	}

	@Override
	public boolean isFeatureSupported(int feature) {
		return codecContext.isFeatureSupported(feature);
	}
}

package org.ebookdroid.droids.html.codec;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

/**
 * Allow use of HMTL documents.
 * Created by sstanf on 2/11/14.
 */
public class HtmlContext extends AbstractCodecContext {
	public static final LogContext LCTX = LogManager.root().lctx("HTML");

	public static final int HTML_FEATURES = FEATURE_DOCUMENT_TEXT_SEARCH | FEATURE_DOCUMENT_MANAGE_SEARCH;

	public HtmlContext() {
		super(HTML_FEATURES);
	}

	@Override
	public CodecDocument openDocument(String fileName, String password) {
		return new HtmlDocument(this, fileName);
	}
}

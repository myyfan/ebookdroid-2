package org.ebookdroid.ui.library.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import org.ebookdroid.CodecType;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.SearchModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.ebookdroid.ui.viewer.stubs.ViewContollerStub;
import org.ebookdroid.ui.viewer.stubs.ViewStub;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.IActionParameter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wraps up a search of a document file.  The search method returns true when
 * the first instance is found.
 *
 * Created by sstanf on 11/6/13.
 */
public class DocSearch
		implements SearchModel.ProgressCallback, DialogInterface.OnCancelListener, IActivityController {
	private final String fileName;
	private final DocumentModel documentModel;
	private final SearchModel searchModel;
	private final BookSettings bookSettings;
	private final LogContext LCTX;
	private final Activity ctx;
	private final AtomicReference<IViewController> ctrl;

	private final AtomicBoolean continueFlag = new AtomicBoolean(true);
	private String pattern = "";

	public DocSearch(Activity ctx, String fileName) {
		this.fileName = fileName;
		searchModel = new SearchModel(this);
		this.ctx = ctx;
		bookSettings = SettingsManager.create(0, fileName, true, null);
		ctrl = new AtomicReference<IViewController>(ViewContollerStub.STUB);
		LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("");

		CodecType codecType = CodecType.getByUri(fileName);
		if (codecType == null) {
			throw new RuntimeException("No codec for: "+fileName);
		}
		documentModel = new DocumentModel(codecType);
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		documentModel.decodeService.stopSearch(pattern);
		continueFlag.set(false);
	}

	public boolean search(String pattern) {
		Page targetPage;
		documentModel.open(fileName, "");
		documentModel.initPages(this, null);
		this.pattern = pattern;
		try {
			searchModel.setPattern(pattern);

			final RectF current = searchModel.moveToNext(this);//forward ? searchModel.moveToNext(this) : searchModel.moveToPrev(this);
			targetPage = searchModel.getCurrentPage();
			if (LCTX.isDebugEnabled()) {
				LCTX.d("DocSearch.search(): " + targetPage + " " + current);
			}
			return current != null;

		} catch (final Throwable th) {
			th.printStackTrace();
		} finally {
			documentModel.recycle();
		}
		return false;
	}


	// Methods to work with SearchModel, many are unimplemented because
	// SearchModel does not need them and implementations will be useless (and
	// in some cases difficult).

	@Override
	public Context getContext() {
		return ctx;
	}

	@Override
	public Activity getActivity() {
		return ctx;
	}

	@Override
	public BookSettings getBookSettings() {
		return bookSettings;
	}

	@Override
	public DecodeService getDecodeService() {
		return documentModel.decodeService;
	}

	@Override
	public DocumentModel getDocumentModel() {
		return documentModel;
	}

	@Override
	public SearchModel getSearchModel() {
		return searchModel;
	}

	@Override
	public IView getView() {
		return ViewStub.STUB;
	}

	@Override
	public IViewController getDocumentController() {
		return ctrl.get();
	}

	@Override
	public IActionController<?> getActionController() {
		return null;
	}

	@Override
	public ZoomModel getZoomModel() {
		return null;
	}

	@Override
	public DecodingProgressModel getDecodingProgressModel() {
		return null;
	}

	@Override
	public void jumpToPage(int viewIndex, float offsetX, float offsetY, boolean addToHistory) {

	}

	@Override
	public void runOnUiThread(Runnable r) {

	}

	@Override
	public IActionController<?> getParent() {
		return null;
	}

	@Override
	public ViewerActivity getManagedComponent() {
		return null;
	}

	@Override
	public void setManagedComponent(ViewerActivity viewerActivity) {

	}

	@Override
	public ActionEx getAction(int id) {
		return null;
	}

	@Override
	public ActionEx getOrCreateAction(int id) {
		return null;
	}

	@Override
	public ActionEx createAction(int id, IActionParameter... parameters) {
		return null;
	}


	// Methods for SearchModel.ProgressCallback

	@Override
	public void searchStarted(int pageIndex) {
	}

	@Override
	public void searchFinished(int pageIndex) {
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void setTotalFound(int total) {

	}

	@Override
	public void setCurrentItem(int currentItem) {

	}
}

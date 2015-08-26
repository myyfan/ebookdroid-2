package org.ebookdroid.ui.viewer;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.common.touch.TouchManagerView;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.EventGotoPage;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.ui.viewer.adapters.OutlineAdapter;
import org.ebookdroid.ui.viewer.stubs.ViewStub;
import org.ebookdroid.ui.viewer.viewers.GLView;
import org.ebookdroid.ui.viewer.viewers.HTMLView;
import org.ebookdroid.ui.viewer.views.ManualCropView;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.SearchControls;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.gl.GLConfiguration;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;
import transapps.courseware.RecordStat;

import java.io.File;
import java.util.List;

public class ViewerActivity extends AbstractActionActivity<ViewerActivity, ViewerActivityController> {

	public static final DisplayMetrics DM = new DisplayMetrics();

	IView view;

	private Toast pageNumberToast;

	private Toast zoomToast;

	private PageViewZoomControls zoomControls;

	private SearchControls searchControls;

	private TouchManagerView touchView;

	private boolean menuClosedCalled;

	private ManualCropView cropControls;

	private DrawerLayout drawer;
	private ActionBarDrawerToggle drawerToggle;
	private View drawerList;
	private boolean outlineSetup = false;

	private static ViewerActivity instance;
	public static ViewerActivity i() {
		return instance;
	}

	public IView getView() {
		return view;
	}


	/**
	 * Instantiates a new base viewer activity.
	 */
	public ViewerActivity() {
		super(false, ON_CREATE, ON_RESUME, ON_PAUSE, ON_DESTROY, ON_POST_CREATE);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#createController()
	 */
	@Override
	protected ViewerActivityController createController() {
		return new ViewerActivityController(this);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		if (LCTX.isDebugEnabled()) {
			LCTX.d("onNewIntent(): " + intent);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#onCreateImpl(android.os.Bundle)
	 */
	@Override
	protected void onCreateImpl(final Bundle savedInstanceState) {
		instance = this;
		getWindowManager().getDefaultDisplay().getMetrics(DM);
		LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

		setContentView(R.layout.viewer);

		final FrameLayout frameLayout = (FrameLayout)findViewById(R.id.content_frame);
		final LinearLayout ll = new LinearLayout(this);
		ll.setVisibility(View.GONE);

		view = ViewStub.STUB;

		try {
			GLConfiguration.checkConfiguration();
			if (getController().getCodecType() == CodecType.HTML) {
				view = new HTMLView(getController());
				if (getIntent() != null && getIntent().getData() != null) {
					((WebView)view).loadUrl(getIntent().getData().toString());
				}
			} else {
				view = new GLView(getController());
			}
			this.registerForContextMenu(view.getView());

			LayoutUtils.fillInParent(frameLayout, view.getView());

			frameLayout.addView(view.getView());

			frameLayout.addView(ll);
			frameLayout.addView(getZoomControls());
			frameLayout.addView(getManualCropControls());
			frameLayout.addView(getSearchControls());
			frameLayout.addView(getTouchView());

			Intent i = getIntent();
			if (i != null && i.getStringExtra("searchTerm") != null) {
				getSearchControls().setTerm(i.getStringExtra("searchTerm"));
			}
		} catch (final Throwable th) {
			final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());
			builder.setTitle(R.string.error_dlg_title);
			builder.setMessage(th.getMessage());
			builder.setPositiveButton(R.string.error_close, R.id.mainmenu_close);
			builder.show();
		}

		drawer = (DrawerLayout)findViewById(R.id.drawer_layout);

		drawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				drawer,                /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,     /* "open drawer" description */
				R.string.drawer_close    /* "close drawer" description */
		) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
				ll.setVisibility(View.GONE);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				supportInvalidateOptionsMenu();
				ll.setVisibility(View.VISIBLE);
				setupOutline();
			}
		};

		drawerToggle.setDrawerIndicatorEnabled(true);
		// Set the drawer toggle as the DrawerListener
		drawer.setDrawerListener(drawerToggle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		drawerList = findViewById(R.id.left_drawer);
	}

	private void setupOutline() {
		if (outlineSetup) return;
		outlineSetup = true;
		final List<OutlineLink> outline = controller.getDocumentModel().decodeService.getOutline();
		if ((outline != null) && (outline.size() > 0)) {
			final ListView listView = new ListView(this);//(ListView)findViewById(R.id.navmenu_outline);
			listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			listView.setBackgroundColor(Color.TRANSPARENT);

			final BookSettings bs = controller.getBookSettings();
			OutlineLink current = null;
			if (bs != null) {
				final int currentIndex = bs.currentPage.docIndex;
				for (final OutlineLink item : outline) {
					int targetIndex = item.targetPage - 1;
					if (targetIndex <= currentIndex) {
						if (targetIndex >= 0) {
							current = item;
						}
					} else {
						break;
					}
				}
			}

			final OutlineAdapter adapter = new OutlineAdapter(this, controller, outline, current);

			listView.setAdapter(adapter);
			final ActionController<ViewerActivity> actions = new ActionController<ViewerActivity>(controller, this);
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					actions.getOrCreateAction(R.id.actions_gotoOutlineItem).onItemClick(parent, view, position, id);
					drawer.closeDrawer(drawerList);
				}
			});

			if (current != null) {
				int pos = adapter.getItemPosition(current);
				if (pos != -1) {
					listView.setSelection(pos);
				}
			}
			((LinearLayout)drawerList).addView(listView);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#onResumeImpl()
	 */
	@Override
	protected void onResumeImpl() {
		IUIManager.instance.onResume(this);
		File f = new File(getController().getBookSettings().fileName);
		Intent intent = getIntent();

		int pageIndex = 0;
		BookSettings settings = getController().getBookSettings();
		if (intent != null && intent.hasExtra("POSITION_EXTRA")) {
			pageIndex = (int)intent.getLongExtra("POSITION_EXTRA", 0);
			settings.currentPage = new PageIndex(pageIndex, pageIndex);
			SettingsManager.applyBookSettingsChanges(null, settings);
		}
		RecordStat.recordStat(getApplicationContext(), Uri.fromFile(f), new Long(pageIndex), RecordStat.StatType.START_VIEWING);
		instance = this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#onPauseImpl(boolean)
	 */
	@Override
	protected void onPauseImpl(final boolean finishing) {
		IUIManager.instance.onPause(this);
		BookSettings bookSettings = getController().getBookSettings();
		File f = new File(bookSettings.fileName);
		int pageIndex = bookSettings.currentPage.docIndex; // TODO should this be the docIndex or the viewIndex.
		RecordStat.recordStat(getApplicationContext(), Uri.fromFile(f), new Long(pageIndex), RecordStat.StatType.STOP_VIEWING);
		instance = null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#onDestroyImpl(boolean)
	 */
	@Override
	protected void onDestroyImpl(final boolean finishing) {
		view.onDestroy();
		instance = null;
	}

	@Override
	protected void onPostCreateImpl(final Bundle savedInstanceState) {
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected boolean onMenuItemSelected(final MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		return drawerToggle.onOptionsItemSelected(item) || super.onMenuItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(final boolean hasFocus) {
		if (hasFocus && this.view != null) {
			IUIManager.instance.setFullScreenMode(this, this.view.getView(), AppSettings.current().fullScreen);
		}
	}

	public TouchManagerView getTouchView() {
		if (touchView == null) {
			touchView = new TouchManagerView(getController());
		}
		return touchView;
	}

	public void currentPageChanged(final String pageText, final String bookTitle) {
		if (LengthUtils.isEmpty(pageText)) {
			return;
		}

		final AppSettings app = AppSettings.current();
		if (IUIManager.instance.isTitleVisible(this) && app.pageInTitle) {
			getSupportActionBar().setTitle("(" + pageText + ") " + bookTitle);
			return;
		}

		if (app.pageNumberToastPosition == ToastPosition.Invisible) {
			return;
		}
		if (pageNumberToast != null) {
			pageNumberToast.setText(pageText);
		} else {
			pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
		}

		pageNumberToast.setGravity(app.pageNumberToastPosition.position, 0, 0);
		pageNumberToast.show();
	}

	public void zoomChanged(final float zoom) {
		if (getZoomControls().isShown()) {
			return;
		}

		final AppSettings app = AppSettings.current();

		if (app.zoomToastPosition == ToastPosition.Invisible) {
			return;
		}

		final String zoomText = String.format("%.2f", zoom) + "x";

		if (zoomToast != null) {
			zoomToast.setText(zoomText);
		} else {
			zoomToast = Toast.makeText(this, zoomText, Toast.LENGTH_SHORT);
		}

		zoomToast.setGravity(app.zoomToastPosition.position, 0, 0);
		zoomToast.show();
	}

	public PageViewZoomControls getZoomControls() {
		if (zoomControls == null) {
			zoomControls = new PageViewZoomControls(this, getController().getZoomModel());
			zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
		}
		return zoomControls;
	}

	public SearchControls getSearchControls() {
		if (searchControls == null) {
			searchControls = new SearchControls(this);
		}
		return searchControls;
	}

	public ManualCropView getManualCropControls() {
		if (cropControls == null) {
			cropControls = new ManualCropView(getController());
		}
		return cropControls;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View,
	 * android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
									final ContextMenu.ContextMenuInfo menuInfo) {
		menu.clear();
		menu.setHeaderTitle(R.string.app_name);
		menu.setHeaderIcon(R.drawable.application_icon);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu_context, menu);
		updateMenuItems(menu);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.clear();

		final MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.mainmenu, menu);

		return true;
	}

	protected boolean hasNormalMenu() {
		return IUIManager.instance.isTabletUi(this) || AppSettings.current().showTitle;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
	 */
	@Override
	public boolean onMenuOpened(final int featureId, final Menu menu) {
		view.changeLayoutLock(true);
		IUIManager.instance.onMenuOpened(this);
		return super.onMenuOpened(featureId, menu);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.emdev.ui.AbstractActionActivity#updateMenuItems(android.view.Menu)
	 */
	@Override
	protected void updateMenuItems(final Menu menu) {
		final AppSettings as = AppSettings.current();

		ActionMenuHelper.setMenuItemChecked(menu, as.fullScreen, R.id.mainmenu_fullscreen);

		ActionMenuHelper.setMenuItemChecked(menu, as.showTitle, R.id.mainmenu_showtitle);

		ActionMenuHelper
				.setMenuItemChecked(menu, getZoomControls().getVisibility() == View.VISIBLE, R.id.mainmenu_zoom);

		final BookSettings bs = getController().getBookSettings();
		if (bs == null) {
			return;
		}

		ActionMenuHelper.setMenuItemChecked(menu, bs.rotation == BookRotationType.PORTRAIT,
				R.id.mainmenu_force_portrait);
		ActionMenuHelper.setMenuItemChecked(menu, bs.rotation == BookRotationType.LANDSCAPE,
				R.id.mainmenu_force_landscape);
		ActionMenuHelper.setMenuItemChecked(menu, bs.nightMode, R.id.mainmenu_nightmode);
		ActionMenuHelper.setMenuItemChecked(menu, bs.cropPages, R.id.mainmenu_croppages);
		ActionMenuHelper.setMenuItemChecked(menu, bs.splitPages, R.id.mainmenu_splitpages,
				R.drawable.viewer_menu_split_pages, R.drawable.viewer_menu_split_pages_off);

		final DecodeService ds = getController().getDecodeService();

		final boolean cropSupported = ds.isFeatureSupported(CodecFeatures.FEATURE_CROP_SUPPORT);
		ActionMenuHelper.setMenuItemVisible(menu, cropSupported, R.id.mainmenu_croppages);
		ActionMenuHelper.setMenuItemVisible(menu, cropSupported, R.id.mainmenu_crop);

		final boolean splitSupported = ds.isFeatureSupported(CodecFeatures.FEATURE_SPLIT_SUPPORT);
		ActionMenuHelper.setMenuItemVisible(menu, splitSupported, R.id.mainmenu_splitpages);

		final MenuItem navMenu = menu.findItem(R.id.mainmenu_nav_menu);
		if (navMenu != null) {
			final SubMenu subMenu = navMenu.getSubMenu();
			if (subMenu != null) {
				subMenu.removeGroup(R.id.actions_goToBookmarkGroup);
				if (AppSettings.current().showBookmarksInMenu && LengthUtils.isNotEmpty(bs.bookmarks)) {
					for (final Bookmark b : bs.bookmarks) {
						addBookmarkMenuItem(subMenu, b);
					}
				}
			}
		}

	}

	protected void addBookmarkMenuItem(final Menu menu, final Bookmark b) {
		final MenuItem bmi = menu.add(R.id.actions_goToBookmarkGroup, R.id.actions_goToBookmark, Menu.NONE, b.name);
		bmi.setIcon(R.drawable.viewer_menu_bookmark);
		ActionMenuHelper.setMenuItemExtra(bmi, "bookmark", b);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onPanelClosed(int, android.view.Menu)
	 */
	@Override
	public void onPanelClosed(final int featureId, final Menu menu) {
		menuClosedCalled = false;
		super.onPanelClosed(featureId, menu);
		if (!menuClosedCalled) {
			onOptionsMenuClosed(menu);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onOptionsMenuClosed(android.view.Menu)
	 */
	@Override
	public void onOptionsMenuClosed(final Menu menu) {
		menuClosedCalled = true;
		IUIManager.instance.onMenuClosed(this);
		view.changeLayoutLock(false);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#dispatchKeyEvent(android.view.KeyEvent)
	 */
	@Override
	public final boolean dispatchKeyEvent(final KeyEvent event) {
		view.checkFullScreenMode();
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU) {
			if (!hasNormalMenu()) {
				getController().getOrCreateAction(R.id.actions_openOptionsMenu).run();
				return true;
			}
		}
		if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
			if (drawer.isDrawerOpen(drawerList)) {
				drawer.closeDrawer(drawerList);
				return true;
			}
		}

		return getController().dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
	}

	public void showToastText(final int duration, final int resId, final Object... args) {
		Toast.makeText(this, getResources().getString(resId, args), duration).show();
	}

	public void goNavMenu(View v) {
		switch (v.getId())
		{
			case (R.id.mainmenu_search):
				controller.toggleControls(controller.getAction(R.id.mainmenu_search));
				break;
			case (R.id.mainmenu_bookmark):
				controller.showBookmarkDialog(controller.getAction(R.id.mainmenu_bookmark));
				break;
			case (R.id.mainmenu_goto_page):
				controller.showGotoDialog(controller.getAction(R.id.mainmenu_goto_page));
				break;
		}
		drawer.closeDrawer(drawerList);
	}
}

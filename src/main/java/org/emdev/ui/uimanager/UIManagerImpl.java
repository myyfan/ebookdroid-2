package org.emdev.ui.uimanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;

import java.util.HashMap;
import java.util.Map;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

// Target level 16 even though it may run on older devices.  The missing flags
// should be ignored since the calls to use them are also missing and the
// thrown exception is caught.
@TargetApi(16)
public class UIManagerImpl implements IUIManager {

	protected static final int STANDARD_SYS_UI_FLAGS =
					View.SYSTEM_UI_FLAG_LOW_PROFILE |
					View.SYSTEM_UI_FLAG_FULLSCREEN |
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

	private static final int EXT_SYS_UI_FLAGS =
					View.SYSTEM_UI_FLAG_LOW_PROFILE |
					View.SYSTEM_UI_FLAG_FULLSCREEN |
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;


	protected static final Map<ComponentName, Data> data = new HashMap<ComponentName, Data>() {

		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 742779545837272718L;

		@Override
		public Data get(final Object key) {
			Data existing = super.get(key);
			if (existing == null) {
				existing = new Data();
				put((ComponentName) key, existing);
			}
			return existing;
		}

	};

	@Override
	public void setTitleVisible(final Activity activity, final boolean visible) {
		try {
			if (activity instanceof ActionBarActivity) {
				ActionBarActivity a = (ActionBarActivity) activity;
				if (visible) {
					a.getSupportActionBar().show();
				} else {
					a.getSupportActionBar().hide();
				}
				a.supportInvalidateOptionsMenu();
			}
			data.get(activity.getComponentName()).titleVisible = visible;
		} catch (final Throwable th) {
			LCTX.e("Error on requestFeature call: " + th.getMessage());
		}
	}

	@Override
	public boolean isTitleVisible(final Activity activity) {
		return data.get(activity.getComponentName()).titleVisible;
	}

	@Override
	public void setFullScreenMode(final Activity activity, final View view, final boolean fullScreen) {
		data.get(activity.getComponentName()).statusBarHidden = fullScreen;
		final Window w = activity.getWindow();

		if (!isTabletUi(activity)) {
			if (fullScreen) {
				w.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
			} else {
				w.clearFlags(FLAG_FULLSCREEN);
			}
		}

		try {
			if (view != null) {
				if (fullScreen) {
					view.setSystemUiVisibility(getHideSysUIFlags(activity));
				} else {
					view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
				}
			}
		} catch (NoSuchMethodError ex) {
			// Running on old Android so ignore this...
		}
	}

	protected int getHideSysUIFlags(final Activity activity) {
		return isTabletUi(activity) ? STANDARD_SYS_UI_FLAGS : EXT_SYS_UI_FLAGS;
	}

	@Override
	public void openOptionsMenu(final Activity activity, final View view) {
		if (data.get(activity.getComponentName()).titleVisible) {
			activity.openOptionsMenu();
		} else {
			view.showContextMenu();
		}
	}

	@Override
	public void invalidateOptionsMenu(final Activity activity) {
		if (activity instanceof ActionBarActivity) {
			try {
				((ActionBarActivity) activity).supportInvalidateOptionsMenu();
			} catch (Throwable th) {
				// This might get called before the menu exists, this is BAD
				// so ignore exception.  Seems to happen when preferences are clear.
			}
		}
	}

	@Override
	public void onMenuOpened(final Activity activity) {
		if (!isTabletUi(activity)) {
			if (data.get(activity.getComponentName()).statusBarHidden) {
				activity.getWindow().clearFlags(FLAG_FULLSCREEN);
			}
		}
	}

	@Override
	public void onMenuClosed(final Activity activity) {
		if (!isTabletUi(activity)) {
			if (data.get(activity.getComponentName()).statusBarHidden) {
				activity.getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
			}
		}
	}

	@Override
	public void onPause(final Activity activity) {
	}

	@Override
	public void onResume(final Activity activity) {
	}

	@Override
	public void onDestroy(final Activity activity) {
	}

	@Override
	public boolean isTabletUi(final Activity activity) {
		final Configuration c = activity.getResources().getConfiguration();
		return 0 != (Configuration.SCREENLAYOUT_SIZE_XLARGE & c.screenLayout);
	}

	private static class Data {
		boolean titleVisible = true;
		boolean statusBarHidden = false;
	}
}

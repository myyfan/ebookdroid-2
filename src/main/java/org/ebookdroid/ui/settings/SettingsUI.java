package org.ebookdroid.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.io.File;

public class SettingsUI {

	public static void showBookSettings(final Context context, final String fileName) {
		final Intent intent = new Intent(context, BookSettingsActivity.class);
		intent.setData(Uri.fromFile(new File(fileName)));
		context.startActivity(intent);
	}

	public static void showAppSettings(final Context context, final String fileName) {
		final Class<?> activityClass = (Build.VERSION.SDK_INT <= 10) ? SettingsActivity.class
				: FragmentedSettingsActivity.class;
		final Intent intent = new Intent(context, activityClass);
		if (fileName != null) {
			intent.setData(Uri.fromFile(new File(fileName)));
		}
		context.startActivity(intent);
	}
}

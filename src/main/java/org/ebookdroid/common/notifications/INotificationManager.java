package org.ebookdroid.common.notifications;

import android.content.Intent;
import android.os.Build;

public interface INotificationManager {

	INotificationManager instance = new ModernNotificationManager();

	int notify(final Integer id, final CharSequence title, final CharSequence message, final Intent intent);

	int notify(final CharSequence title, final CharSequence message, final Intent intent);

	int notify(final CharSequence message);

	int notify(final int titleId, final CharSequence message, final Intent intent);

	int notify(final int titleId, final int messageId);

	int notify(final int messageId);

}

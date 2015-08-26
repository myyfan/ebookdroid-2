package org.ebookdroid.common.settings.base;

import android.content.SharedPreferences;
import org.ebookdroid.CodecType;
import org.emdev.common.filesystem.FileExtensionFilter;
import org.emdev.common.settings.base.JsonObjectPreferenceDefinition;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class FileTypeFilterPreferenceDefinition extends JsonObjectPreferenceDefinition {

	public FileTypeFilterPreferenceDefinition(final int keyRes) {
		super(keyRes);
	}

	public FileExtensionFilter getFilter(final SharedPreferences prefs) {
		final JSONObject obj = getPreferenceValue(prefs);
		final Set<String> res = new HashSet<String>();

		for (final String ex : CodecType.getAllExtensions()) {
			if (!obj.has(ex) || obj.optBoolean(ex)) {
				res.add(ex);
			}
		}

		return new FileExtensionFilter(res);
	}
}

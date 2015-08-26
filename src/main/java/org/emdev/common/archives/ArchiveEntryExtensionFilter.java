package org.emdev.common.archives;

import org.emdev.common.filesystem.FileExtensionFilter;

import java.util.Set;

public class ArchiveEntryExtensionFilter extends FileExtensionFilter {

	public ArchiveEntryExtensionFilter(final Set<String> extensions) {
		super(extensions);
	}

	public ArchiveEntryExtensionFilter(final String... extensions) {
		super(extensions);
	}

	public final boolean accept(final ArchiveEntry archiveEntry) {
		return acceptImpl(archiveEntry.getName().toLowerCase());
	}
}

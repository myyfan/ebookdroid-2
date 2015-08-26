package org.emdev.common.archives;

import org.emdev.common.filesystem.FilePrefixFilter;

import java.util.Set;

public class ArchiveEntryPrefixFilter extends FilePrefixFilter {

	public ArchiveEntryPrefixFilter(final Set<String> prefixes) {
		super(prefixes);
	}

	public ArchiveEntryPrefixFilter(final String... prefixes) {
		super(prefixes);
	}

	public final boolean accept(final ArchiveEntry archiveEntry) {
		return acceptImpl(archiveEntry.getName().toLowerCase());
	}
}

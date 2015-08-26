package org.ebookdroid.droids.cbx.codec;


import org.emdev.common.archives.ArchiveEntry;
import org.emdev.common.archives.ArchiveFile;

import java.io.File;
import java.io.IOException;

public interface CbxArchiveFactory<ArchiveEntryType extends ArchiveEntry> {

	ArchiveFile<ArchiveEntryType> createArchive(final File file, final String password) throws IOException;
}

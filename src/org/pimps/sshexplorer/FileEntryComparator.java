package org.pimps.sshexplorer;

import java.util.Comparator;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public class FileEntryComparator implements Comparator<FileEntry> {

	@Override
	public int compare(FileEntry a, FileEntry b) {
		if (a.dir && !b.dir)
			return -1;
		if (!a.dir && b.dir)
			return 1;
		return a.name.compareTo(b.name);
	}

}

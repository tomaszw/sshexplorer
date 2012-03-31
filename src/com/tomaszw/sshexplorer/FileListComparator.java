package com.tomaszw.sshexplorer;

import java.util.Comparator;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public class FileListComparator implements Comparator<LsEntry> {

	@Override
	public int compare(LsEntry a, LsEntry b) {
		if (a.getAttrs().isDir() && !b.getAttrs().isDir())
			return -1;
		if (!a.getAttrs().isDir() && b.getAttrs().isDir())
			return 1;
		
		return a.getFilename().compareTo(b.getFilename());
	}

}

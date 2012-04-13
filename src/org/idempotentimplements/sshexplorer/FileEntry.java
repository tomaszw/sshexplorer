package org.idempotentimplements.sshexplorer;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public class FileEntry {
    public String name;
    public String path;
    public boolean dir, link;
    public int perms;
    public long size;
    
    public static FileEntry fromLsEntry(String path, LsEntry ls) {
        FileEntry e = new FileEntry();
        e.name = ls.getFilename();
        e.path = path;
        e.dir = ls.getAttrs().isDir();
        e.link = ls.getAttrs().isLink();
        e.perms = ls.getAttrs().getPermissions();
        e.size = ls.getAttrs().getSize();
        return e;
    }
    
    public String fullname() {
        return path + "/" + name;   
    }
}

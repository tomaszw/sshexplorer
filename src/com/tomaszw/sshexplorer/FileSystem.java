package com.tomaszw.sshexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileSystem {
    public List<FileEntry> entries(String path) throws IOException;
    public InputStream input(String path) throws IOException;
    public OutputStream output(String path) throws IOException;
}

package org.idempotentimplements.sshexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.idempotentimplements.sshexplorer.stream.ScpInputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SSHFileSystem implements FileSystem {
    private Session m_session;
    private ChannelSftp m_sftpChannel;

    public SSHFileSystem(Session s) throws JSchException {
        m_session = s;
        Channel channel = m_session.openChannel("sftp");
        channel.connect();
        m_sftpChannel = (ChannelSftp) channel;

    }

    @Override
    public String upPath(String path) throws IOException {
        // TODO Auto-generated method stub
        return normPath(path + "/..");
    }

    @Override
    public synchronized String normPath(String path) throws IOException {
        // TODO Auto-generated method stub
        try {
            if (path == null || path.trim().equals("")) {
                String home = m_sftpChannel.getHome();
                path = home;
                App.d("home=" + home);
            }
            
            App.d("normPath " + path + " len=" + path.length());
            m_sftpChannel.cd(path);
            return m_sftpChannel.pwd();
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public synchronized List<FileEntry> entries(String path) throws IOException {
        if (path.equals(""))
            path = ".";
        // TODO Auto-generated method stub
        ArrayList<FileEntry> values = new ArrayList<FileEntry>();
        Vector v;
        try {
            v = m_sftpChannel.ls(path);
        } catch (SftpException ex) {
            throw (new IOException(ex));
        }
        for (Object o : v) {
            if (o instanceof LsEntry) {
                LsEntry e = (LsEntry) o;
                if (e.getFilename().equals(".") || e.getFilename().equals("..")) {
                    continue;
                }

                String epath = path.equals(".") ? "" : path;
                values.add(FileEntry.fromLsEntry(path, e));
            }
        }
        return values;
    }

    @Override
    public InputStream input(String path) throws IOException {
        return new ScpInputStream(m_session, path);
    }

    @Override
    public OutputStream output(String path) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}

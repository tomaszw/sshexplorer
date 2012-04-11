package com.tomaszw.sshexplorer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpInputStream extends InputStream {
    private Session m_session;
    private ChannelSftp m_channel;
    private byte[] m_ch;
    private String m_path;
    private InputStream m_in;
    private OutputStream m_out;
    private GetThread m_getThread;
    
    public static class Header {
        public long sz;
    }

    
    class TheOutput extends PipedOutputStream {
        public TheOutput(PipedInputStream in) throws IOException {
            super(in);
        }

        @Override
        public void write(byte[] buffer, int offset, int count)
                throws IOException {
            // TODO Auto-generated method stub
            //Log.d(App.TAG, "write " + count);
            super.write(buffer, offset, count);
        }
    }

    class GetThread extends Thread {
        public GetThread() {
        }
        
        public void run() {
            try {
                m_channel.get(m_path, m_out);
            } catch (SftpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public SftpInputStream(Session s, String path) throws IOException {
        m_session = s;
        m_path = path;
        try {
            m_channel = (ChannelSftp) m_session.openChannel("sftp");
            m_channel.setBulkRequests(256);
            PipedInputStream in = new PipedInputStream(65536);
            m_in = in;
            m_out = new BufferedOutputStream(new TheOutput(in), 65536);
            m_channel.connect();
            m_ch = new byte[1];
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        m_channel.disconnect();
        m_in.close();
        m_out.close();
    }

    @Override
    public int read() throws IOException {
        // TODO Auto-generated method stub
        int r = read(m_ch, 0, 1);
        if (r < 0)
            return r;
        return m_ch[0];
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (m_getThread == null) {
            m_getThread = new GetThread();
            m_getThread.start();
        }
        return m_in.read(buffer, offset, length);
    }
}

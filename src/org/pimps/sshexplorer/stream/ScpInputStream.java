package org.pimps.sshexplorer.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pimps.sshexplorer.App;

import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ScpInputStream extends InputStream implements ProvidesStreamSize {
    private Session m_session;
    private OutputStream m_out;
    private InputStream m_in;
    private ChannelExec m_channel;
    private Header m_header;
    private byte[] m_ch;
    private int m_phase = 0;
    private long m_totalRead = 0;
    private String m_path;

    public static class Header {
        public long sz;
    }

    private String escapePath(String p) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < p.length(); ++i) {
            switch (p.charAt(i)) {
            case '"':
                b.append("\\\"");
                break;
            default:
                b.append(p.charAt(i));
                break;
            }
        }
        return b.toString();
    }

    public ScpInputStream(Session s, String path) throws IOException {
        m_session = s;
        m_path = path;
        try {
            m_channel = (ChannelExec) m_session.openChannel("exec");
            String cmd = "scp -f -- \"" + escapePath(path) + "\"";
            App.d("sending command: " + cmd);
            m_channel.setCommand(cmd);
            // m_channel.set
            m_out = m_channel.getOutputStream();
            m_in = m_channel.getInputStream();
            m_channel.connect();
            m_ch = new byte[1];
            assertHeader();
            
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        try {
            m_channel.disconnect();
        } catch (Throwable t) {
        }
        try {
            m_in.close();
        } catch (Throwable t) {
        }
        try {
            m_out.close();
        } catch (Throwable t) {
        }
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
        assertHeader();
        if (m_phase == 0) {
            send0();
            ++m_phase;
        }
        if (length > m_header.sz - m_totalRead) {
            length = (int) (m_header.sz - m_totalRead);
        }
        int r = m_in.read(buffer, offset, length);
        // Log.d(App.TAG, "read " + r);
        if (r > 0) {
            m_totalRead += r;
            if (m_totalRead >= m_header.sz) {
                // past end
                if (0 == checkAck(m_in)) {
                    send0();
                    // all ok
                }

                ++m_phase;
            }
        } else if (r < 0) {
            // error
            checkAck(m_in);
            send0();
        }
        return r;
    }

    private void send0() throws IOException {
        m_ch[0] = 0;
        m_out.write(m_ch, 0, 1);
        m_out.flush();
    }

    public long streamSize() {
        try {
            assertHeader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return m_header.sz;
    }

    private void assertHeader() throws IOException {
        if (m_header != null)
            return;
        m_header = readHeader(m_in, m_out);
    }

    private Header readHeader(InputStream in, OutputStream out)
            throws IOException {
        byte buf[] = new byte[256];
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        int c = checkAck(in);
        if (c != 'C') {
            throw new IOException("Invalid header 1 " + c);
        }

        // read '0644 '
        if (readN(in, buf, 0, 5) < 0) {
            throw new IOException("Invalid header 2");
        }

        long filesize = 0L;
        while (true) {
            if (in.read(buf, 0, 1) < 0) {
                throw new IOException("Invalid header 3");
            }
            if (buf[0] == ' ')
                break;
            filesize = filesize * 10L + (long) (buf[0] - '0');
        }

        String file = null;
        for (int i = 0;; i++) {
            if (in.read(buf, i, 1) < 0) {
                throw new IOException("Invalid header 4");
            }
            if (buf[i] == (byte) 0x0a) {
                file = new String(buf, 0, i);
                break;
            }
        }
        Header h = new Header();
        h.sz = filesize;
        return h;
    }

    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0)
            return b;
        if (b == -1)
            return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                throw new IOException(sb.toString());
            }
            if (b == 2) { // fatal error
                throw new IOException(sb.toString());
            }
        }
        if (b != 0) {
            Log.e(App.TAG, m_path + " ack=" + b);
        }
        return b;
    }

    private static int readN(InputStream in, byte[] buf, int off, int sz)
            throws IOException {
        int orgSz = sz;
        while (sz > 0) {
            int r = in.read(buf, off, sz);
            if (r < 0)
                return r;
            sz -= r;
            off += r;
        }
        return orgSz;
    }

}

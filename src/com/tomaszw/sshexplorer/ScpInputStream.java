package com.tomaszw.sshexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ScpInputStream extends InputStream implements KnownSize {
    private Session m_session;
    private OutputStream m_out;
    private InputStream m_in;
    private ChannelExec m_channel;
    private Header m_header;
    private byte[] m_ch;
    private int m_phase = 0;

    public static class Header {
        public long sz;
    }

    public ScpInputStream(Session s, String path) throws IOException {
        m_session = s;
        try {
            m_channel = (ChannelExec) m_session.openChannel("exec");
            m_channel.setCommand("scp -f " + path);
            m_out = m_channel.getOutputStream();
            m_in = m_channel.getInputStream();
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
        assertHeader();
        if (m_phase == 0) {
            m_ch[0] = 0;
            m_out.write(m_ch, 0, 1);
            m_out.flush();
            ++m_phase;
        }
        int r = m_in.read(buffer, offset, length);
        return r;
    }

    public long knownSize() {
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

    private static Header readHeader(InputStream in, OutputStream out)
            throws IOException {
        byte buf[] = new byte[256];
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        int c = checkAck(in);
        if (c != 'C') {
            throw new IOException("Invalid header");
        }

        // read '0644 '
        if (readN(in, buf, 0, 5) < 0) {
            throw new IOException("Invalid header");
        }

        long filesize = 0L;
        while (true) {
            if (in.read(buf, 0, 1) < 0) {
                throw new IOException("Invalid header");
            }
            if (buf[0] == ' ')
                break;
            filesize = filesize * 10L + (long) (buf[0] - '0');
        }

        String file = null;
        for (int i = 0;; i++) {
            if (in.read(buf, i, 1) < 0) {
                throw new IOException("Invalid header");
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

    private static int checkAck(InputStream in) throws IOException {
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
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
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

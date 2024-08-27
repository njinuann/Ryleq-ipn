package com.starise.ipn.bean;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Collections;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = toByteArray(requestInputStream);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new CachedBodyServletInputStream(byteArrayInputStream);
    }

    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream byteArrayInputStream;

        public CachedBodyServletInputStream(ByteArrayInputStream byteArrayInputStream) {
            this.byteArrayInputStream = byteArrayInputStream;
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }
    }
}


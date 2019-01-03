

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SimpleServer {

    static final String DATE_FORMAT_GMT = " EEE, dd MMM yyyy hh:mm:ss 'GMT'";
    static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    static final String HTTP_CACHE_CONTROL = "Cache-Control";
    static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";
    static final String HTTP_CONTENT_TYPE = "Content-Type";
    static final String HTTP_DATE = "Date";
    static final int MILLIS_PER_SECOND = 1000;
    static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60; //     60,000
    static final int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;   //  3,600,000
    static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;      // 86,400,000
    static final String UTF_8 = "UTF-8";
    final String HTTP_CONTENT_LENGTH = "Content-Length";
    private final ServerSocket mServerSocket;
    private final String mURL;
    private int mPort;
    private String mStaticDirectory;
    private Thread mThread;

    public SimpleServer(int port, String hostName) throws IOException {

        mPort = port;
        InetAddress address = InetAddress.getByName(hostName);
        byte[] bytes = address.getAddress();

        mServerSocket = new ServerSocket(mPort, 0, InetAddress.getByAddress(bytes));
        mServerSocket.setSoTimeout(MILLIS_PER_SECOND * 20);
        mPort = mServerSocket.getLocalPort();
        mURL = "http://" + mServerSocket.getInetAddress().getHostAddress() + ":" + mPort;
        startServer();
    }

    public String getURL() {
        return mURL;
    }

    private int lookup(byte[] content, byte[] pattern, int startIndex) {

        int l1 = content.length;
        int l2 = pattern.length;

        for (int i = startIndex; i < l1 - l2 + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < l2; ++j) {
                if (content[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    private List<String> parseHeaders(byte[] buffer) {
        List<String> headers = new ArrayList<>();
        int len = buffer.length;
        int offset = 0;
        boolean skip = false;
        for (int i = 0; i < len; i++) {
            if (!skip && buffer[i] == ':') {
                headers.add(toString(Arrays.copyOfRange(buffer, offset, i)));
                offset = i + 1;
                skip = true;
            }
            if (buffer[i] == '\r') {
                while (buffer[offset] == ' ') {
                    offset++;
                }
                headers.add(toString(Arrays.copyOfRange(buffer, offset, i)));
                offset = i + 2;
                skip = false;
            }
        }
        if (offset < len) {
            headers.add(toString(Arrays.copyOfRange(buffer, offset, len)));
        }
        return headers;
    }

    private String[] parseURL(byte[] buffer) {


        int p1 = 0, p2 = 0, p3 = 0;

        for (int i = 0; i < buffer.length; i++) {
            if (p1 == 0) {
                if (buffer[i] == '/')
                    p1 = i + 1;
            } else if (p3 == 0 && buffer[i] == ' ') {
                p3 = i;
            }
            if (p2 == 0 && buffer[i] == '?') {
                p2 = i;
            }
        }
        String method = null;
        if (p1 > 0) {
            method = toString(Arrays.copyOfRange(buffer, 0, p1 - 1)).trim();
        }
        String url = null;
        String parameter = null;
        if (p2 != 0) {

            url = toString(Arrays.copyOfRange(buffer, p1, p2));
            parameter = toString(Arrays.copyOfRange(buffer, p2, p3));
        } else {

            url = toString(Arrays.copyOfRange(buffer, p1, p3));

        }

        return new String[]{
                method,
                url,
                parameter
        };
    }

    private void processRequest(Socket socket) {

        try {
            byte[][] status = sliceURL(socket);
            if (status == null || status.length < 1) {
                send(socket, 404);
                return;
            }

            String[] u = parseURL(status[0]);
            if (u[1].length() == 0/* / */) {
                send(socket, 200, new File(mStaticDirectory, "index.html"));
                return;
            } else if (u[1].lastIndexOf('.') != -1) {
                File file = new File(mStaticDirectory, u[1]);
                if (file.isFile()) {
                    send(socket, 200, file);
                } else {
                    send(socket, 404);
                }
            } else if (u[1].equals("upload")) {
                processUploadFile(socket, status[1]);
                send(socket, 404);
            } else {
                d("URI: " + u[1]);
            }


            //d(toString(status[1]));

            //parseHeader(socket);
        } catch (Exception e) {
            e(e);
        }

        closeQuietly(socket);
    }

    private void processUploadFile(Socket socket, byte[] bytes) throws IOException {
        System.out.println("[processUploadFile] => ");
        byte[][] header = sliceHeader(socket, bytes);
        List<String> headers = parseHeaders(header[0]);
        String boundary = null;
        for (int i = 0; i < headers.size(); i += 2) {
            if (headers.get(i).equalsIgnoreCase(HTTP_CONTENT_TYPE))
                if (headers.get(i + 1).startsWith("multipart/form-data;")) {
                    boundary = substringAfter(headers.get(i + 1), "boundary=");
                    System.out.println(boundary);

                } else {
                    send(socket, 404);
                    return;
                }
        }
        if (boundary == null) {
            send(socket, 404);
            return;
        }
        InputStream is = socket.getInputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int len = -1;
        byte[] content = null;


        if (header[2][0] == 0) {
            content = header[1];

        } else {
            while ((len = is.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {

                content = addAll(header[1], buffer);
            }
        }


        byte[] b1 = boundary.getBytes(UTF_8);
        byte[] b2 = new byte[]{'\r', '\n'};
        byte[] b3 = new byte[]{'\r', '\n', '\r', '\n'};

        len = content.length;
        boolean hit = false;
        int offset = 0;
        for (int i = 0; i < len; i++) {
            int p1 = lookup(content, b1, offset);
            if (p1 == -1) break;
            int startIndex = p1 + b1.length + 2;
            int p2 = lookup(content, b2, startIndex);
            String fileName = new String(Arrays.copyOfRange(content, startIndex, p2));
            fileName = substringAfter(fileName, "filename=");
            int p3 = lookup(content, b3, p2);
            int p4 = lookup(content, b1, p3);
            i = p4 + b1.length;
            offset = i;
            System.out.println(content.length + " " + i);
            while (content[p4] != '\n') {
                p4--;
            }
            writeBytes(trim(fileName, new char[]{'\"'}), Arrays.copyOfRange(content, p3 + 4, p4));
            System.out.println(fileName);

        }
    }

    static String trim(String s, char[] chars) {
        int startIndex = 0;
        int endIndex = s.length() - 1;
        boolean startFound = false;

        while (startIndex <= endIndex) {
            int index = (!startFound) ? startIndex : endIndex;
            boolean match = false;
            for (char c : chars) {
                if (c == s.charAt(index)) {
                    match = true;
                    break;
                }
            }

            if (!startFound) {
                if (!match)
                    startFound = true;
                else
                    startIndex += 1;
            } else {
                if (!match)
                    break;
                else
                    endIndex -= 1;
            }
        }

        return s.substring(startIndex, endIndex + 1);
    }

    private String responseHeader(int statusCode, List<String> headers) {

        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 ")
                .append(statusCode)
                .append(' ')
                .append(getDefaultReason(statusCode))
                .append("\r\n");

        if (headers != null) {
            int len = headers.size();
            if (len != 0) {
                assert (len % 2 == 0);
                len = len - 1;
                for (int i = 0; i < len; i++) {
                    sb.append(headers.get(i)).append(": ").append(headers.get(++i)).append("\r\n");
                }
            }
        }

        sb.append("\r\n");
        return sb.toString();
    }

    private void send(Socket socket, int statusCode) {
        send(socket, statusCode, null, null);
    }

    private void send(Socket socket, int statusCode, File file) {
        try {
            OutputStream os = socket.getOutputStream();
            List<String> headers = new ArrayList<>();

            String extension = substringAfterLast(file.getName(), '.').toLowerCase();
            if (extension != null) {
                switch (extension) {
                    case ".html":
                        headers.add(HTTP_CONTENT_TYPE);
                        headers.add("text/html; charset=utf-8");
                        headers.add(HTTP_CACHE_CONTROL);
                        headers.add("no-cache");
                        break;

                    case ".js":
                        headers.add(HTTP_CONTENT_TYPE);
                        headers.add("text/javascript; charset=UTF-8");
                        headers.add(HTTP_CACHE_CONTROL);
                        headers.add("public, max-age=31536000, stale-while-revalidate=2592000");
                        break;
                    case ".css":
                        headers.add(HTTP_CONTENT_TYPE);
                        headers.add("text/css");
                        headers.add(HTTP_CACHE_CONTROL);
                        headers.add("public, max-age=31536000, stale-while-revalidate=2592000");
                        break;
                }
            }
            headers.add(HTTP_DATE);
            headers.add(new SimpleDateFormat(DATE_FORMAT_GMT, Locale.US).format(file.lastModified()));
            headers.add(HTTP_CONTENT_LENGTH);
            headers.add(Long.toString(file.length()));

            byte[] header = responseHeader(statusCode, headers).getBytes(UTF_8);
            os.write(header, 0, header.length);

            FileInputStream is = new FileInputStream(file);

            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];

            int len;
            while ((len = is.read(bytes, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (Exception e) {
            e(e);
            send(socket, 500);
        } finally {
            closeQuietly(socket);
        }
    }

    private void send(Socket socket, int statusCode, List<String> headers, byte[]... bytes) {

        try {
            OutputStream os = socket.getOutputStream();

            byte[] header = responseHeader(statusCode, headers).getBytes(UTF_8);
            os.write(header, 0, header.length);
            if (bytes != null) {
                for (int i = 0; i < bytes.length; i++) {
                    os.write(bytes[i], 0, bytes[i].length);
                }
            }
            os.flush();
        } catch (Exception e) {
            e(e);
        } finally {
            closeQuietly(socket);
        }
    }

    private void sendFile(Socket socket, String type, String fileName) {

        File file = new File(mStaticDirectory, fileName);
        try {
            FileInputStream is = new FileInputStream(file);
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];

            int len;
            while ((len = is.read(bytes, 0, DEFAULT_BUFFER_SIZE)) != -1) {

            }

        } catch (Exception e) {
            e(e);
            send(socket, 500);
        } finally {
            closeQuietly(socket);
        }
    }

    public void setStaticDirectory(String staticDirectory) {
        mStaticDirectory = staticDirectory;
    }

    private byte[][] sliceHeader(Socket socket, byte[] bytes) throws IOException {
        InputStream is = socket.getInputStream();
        int bufferSize = 1024 * 8;
        int len;
        byte[] buffer = new byte[bufferSize];
        if ((len = is.read(buffer, 0, bufferSize)) != -1) {

            for (int i = 0; i < len; i++) {
                if (i + 3 < len && buffer[i] == '\r'
                        && buffer[i + 1] == '\n'
                        && buffer[i + 2] == '\r'
                        && buffer[i + 3] == '\n') {

                    byte[] buf1 = Arrays.copyOfRange(buffer, 0, i);
                    byte[] buf2 = null;
                    if (len - i > 4) {
                        buf2 = Arrays.copyOfRange(buffer, i + 4, len);
                    }
                    byte[][] result = new byte[][]{
                            addAll(bytes, buf1),
                            buf2,
                            len < bufferSize ? new byte[]{0} : new byte[]{1}
                    };
                    return result;
                }
            }
        }
        return null;
    }

    private byte[][] sliceURL(Socket socket) throws IOException {

        InputStream is = socket.getInputStream();

        int bufferSize = 256;
        byte[] buffer = new byte[bufferSize];
        int len;
        if ((len = is.read(buffer, 0, bufferSize)) != -1) {

            for (int i = 0; i < len; i++) {
                if (i + 1 < len && buffer[i] == '\r' && buffer[i + 1] == '\n') {

                    byte[] buf1 = Arrays.copyOfRange(buffer, 0, i);
                    byte[] buf2 = null;
                    if (len - i > 2) {
                        buf2 = Arrays.copyOfRange(buffer, i + 2, len);
                    }
                    byte[][] result = new byte[][]{
                            buf1,
                            buf2
                    };
                    return result;
                }
            }
        }
        return null;
    }

    private void startServer() {
        mThread = new Thread(() -> {
            while (true) {
                try {

                    Socket socket = mServerSocket.accept();
                    processRequest(socket);

                } catch (SocketTimeoutException ignore) {
                } catch (IOException e) {
                    e(e);
                }
            }
        });
        mThread.setDaemon(true);
        mThread.start();
    }

    private void writeBytes(String fileName, byte[] buffer) throws IOException {
        File file = new File("c:\\", fileName);
        FileOutputStream os = new FileOutputStream(file);
        os.write(buffer, 0, buffer.length);
        closeQuietly(os);
    }

    static byte[] addAll(final byte[] array1, final byte... array2) {
        if (array1 == null) {
            return array2.clone();
        } else if (array2 == null) {
            return array1.clone();
        }
        final byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {

        }
    }

    static void d(String message) {
        System.out.println("[D]: " + message);
    }

    static void e(Exception e) {
        System.out.println("[E]: " + e);
    }

    static void e(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            sb.append(o).append(' ');
        }
        System.out.println("[E]: " + sb.toString());
    }

    static String getDefaultReason(int statusCode) {
        switch (statusCode) {
            case 100:
                return "Continue";
            case 101:
                return "Switching Protocols";
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 202:
                return "Accepted";
            case 203:
                return "Non-Authoritative Information";
            case 204:
                return "No Content";
            case 205:
                return "Reset Content";
            case 206:
                return "Partial Content";
            case 300:
                return "Multiple Choices";
            case 301:
                return "Moved Permanently";
            case 302:
                return "Found";
            case 303:
                return "See Other";
            case 304:
                return "Not Modified";
            case 305:
                return "Use Proxy";
            case 307:
                return "Temporary Redirect";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 402:
                return "Payment Required";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 406:
                return "Not Acceptable";
            case 407:
                return "Proxy Authentication Required";
            case 408:
                return "Request Time-out";
            case 409:
                return "Conflict";
            case 410:
                return "Gone";
            case 411:
                return "Length Required";
            case 412:
                return "Precondition Failed";
            case 413:
                return "Request Entity Too Large";
            case 414:
                return "Request-URI Too Large";
            case 415:
                return "Unsupported Media Type";
            case 416:
                return "Requested range not satisfiable";
            case 417:
                return "Expectation Failed";
            case 500:
                return "Internal Server Error";
            case 501:
                return "Not Implemented";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Time-out";
            case 505:
                return "HTTP Version not supported";
            default: {
                int errorClass = statusCode / 100;
                switch (errorClass) {
                    case 1:
                        return "Informational";
                    case 2:
                        return "Success";
                    case 3:
                        return "Redirection";
                    case 4:
                        return "Client Error";
                    case 5:
                        return "Server Error";
                    default:
                        return null;
                }
            }
        }
    }

    static String substringAfter(String s, String delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return null;
        else return s.substring(index + delimiter.length());
    }

    static String substringAfterLast(String s, char delimiter) {
        int index = s.lastIndexOf(delimiter);
        if (index == -1) return null;
        else return s.substring(index);
    }

    static String substringBefore(String s, char delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return null;
        else return s.substring(0, index);
    }

    static String toString(byte[] buffer) {
        try {
            return new String(buffer, UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
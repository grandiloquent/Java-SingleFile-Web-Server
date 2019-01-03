
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class SimpleServer {

    static final int MILLIS_PER_SECOND = 1000;
    static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60; //     60,000
    static final int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;   //  3,600,000
    static final int MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;      // 86,400,000
    static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    static final String UTF_8 = "UTF-8";

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

    private String[] parseURL(byte[] buffer) {

        d(toString(buffer));

        int p1 = 0, p2 = 0, p3 = 0;

        for (int i = 0; i < buffer.length; i++) {
            if (p1 == 0 && buffer[i] == '/') {

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
            method = toString(Arrays.copyOfRange(buffer, 0, p1));
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
            if (u[0].length() == 0/* / */) {
                return;
            }
            //            byte[][] header = sliceHeader(socket, status[1]);
//            d(toString(header[0]));

            //d(toString(status[1]));

            //parseHeader(socket);
        } catch (Exception e) {
            e(e);
        }

        closeQuietly(socket);
    }

    private void send(Socket socket, int statusCode) {
        send(socket, statusCode, null, null);
    }

    private void send(Socket socket, int statusCode, String[] headers, byte[]... bytes) {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 ")
                .append(statusCode)
                .append(' ')
                .append(getDefaultReason(statusCode))
                .append("\r\n");

        if (headers != null) {
            assert (headers.length % 2 == 0);

            for (int i = 0; i < headers.length; i++) {

            }
        }

        sb.append("\r\n");
        try {
            OutputStream os = socket.getOutputStream();

            byte[] header = sb.toString().getBytes(UTF_8);
            os.write(header, 0, header.length);
            if (bytes != null) {
                for (int i = 0; i < bytes.length; i++) {
                    os.write(bytes[i], 0, bytes[i].length);
                }
            }
            os.flush();
        } catch (Exception e) {

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
                            buf2
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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
    static final String UTF_8 = "UTF-8";

    private final ServerSocket mServerSocket;
    private final String mURL;
    private int mPort;
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

    private void processRequest(Socket socket) {
      
        try {
            byte[][] status = parseURL(socket);
            if (status != null) {
                byte[] s1 = status[0];
                if (s1 != null) {
                    d(new String(s1, UTF_8));
                    d(new String(status[1], UTF_8));
                }
            }
            byte[][] header = parseHeader(socket, status[1]);
            d(toString(header[0]));

            //d(toString(status[1]));

            //parseHeader(socket);
        } catch (Exception e) {
            e(e);
        }

        closeQuietly(socket);
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

    static void e(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            sb.append(o).append(' ');
        }
        System.out.println("[E]: " + sb.toString());
    }

    static void e(Exception e) {
        System.out.println("[E]: " + e);
    }

    static byte[][] parseHeader(Socket socket, byte[] bytes) throws IOException {
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
                        buf2 = Arrays.copyOfRange(buffer, i + 2, len);
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

    static byte[][] parseURL(Socket socket) throws IOException {

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

    static String substringBefore(String s, char delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return null;
        else return s.substring(0, index);
    }


    static String toString(byte[] buffer) throws UnsupportedEncodingException {
        return new String(buffer, UTF_8);
    }
}

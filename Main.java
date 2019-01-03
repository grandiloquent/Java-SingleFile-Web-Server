import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] arg) {


        try {
            SimpleServer simpleServer = new SimpleServer(8090, "localhost");
            simpleServer.setStaticDirectory("\\static");
            System.out.println(simpleServer.getURL());
        } catch (IOException e) {
            e.printStackTrace();
            SimpleServer.e(e);
        }
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

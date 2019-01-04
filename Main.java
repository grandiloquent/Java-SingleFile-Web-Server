
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] arg) {



        try {
            SimpleServer simpleServer = new SimpleServer(8090, "localhost");
            simpleServer.setStaticDirectory("C:\\Users\\psycho\\IdeaProjects\\Console\\src\\static");
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

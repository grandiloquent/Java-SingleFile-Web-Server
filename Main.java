
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] arg) {


        try {
            // 设置监听地址和端口
            SimpleServer simpleServer = new SimpleServer(8090, "localhost");
            // 设置静态文件所在目录
            simpleServer.setStaticDirectory("C:\\Users\\psycho\\IdeaProjects\\Console\\src\\static");
            // 设置音频文件所在目录
            simpleServer.setVideoDirectory(new String[]{
                    "C:\\Users\\psycho\\Desktop\\Downloads"
            });
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

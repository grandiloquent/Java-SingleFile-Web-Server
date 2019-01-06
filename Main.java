import java.io.IOException;

public class Main {

    public static void main(String[] arg) {


        // 设置监听地址和端口
        SimpleServer simpleServer =
                new SimpleServer.Builder("localhost", 8090)
                        .setStaticDirectory("C:\\Users\\psycho\\IdeaProjects\\Console\\src\\static")
                        .setVideoDirectory(new String[]{
                                "C:\\Users\\psycho\\Desktop\\Downloads"
                        })
                        .setUploadDirectory("c:\\Upload")
                        .build();
        if (simpleServer == null) {

            System.exit(1);
        }
        System.out.println(simpleServer.getURL());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

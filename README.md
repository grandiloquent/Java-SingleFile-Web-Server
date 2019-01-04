# Single File Web Server In Java

单文件 Java Web 服务器

一个简单的网页服务器，支持播放视频和上传文件，可用于安卓项目。


```java
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
``` 

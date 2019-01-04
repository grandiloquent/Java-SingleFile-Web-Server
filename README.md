# Single File Web Server In Java


```java
        try {
            SimpleServer simpleServer = new SimpleServer(8090, "localhost");
            simpleServer.setStaticDirectory("C:\\Users\\psycho\\IdeaProjects\\Console\\src\\static");
            simpleServer.setVideoDirectory(new String[]{
                    "C:\\Users\\psycho\\Desktop\\Downloads"
            });
            System.out.println(simpleServer.getURL());
        } catch (IOException e) {
            e.printStackTrace();
            SimpleServer.e(e);
        }
``` 

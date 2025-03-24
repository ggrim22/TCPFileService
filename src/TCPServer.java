import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3000));
        ExecutorService es = Executors.newFixedThreadPool(4);
        Thread inputThread = getThread(listenChannel, es);
        inputThread.start();

        // Main loop for handling client connections
        while (listenChannel.isOpen()) {
            try {
                SocketChannel clientChannel = listenChannel.accept();
                if (clientChannel != null) {
                    es.submit(new handleCommandTask(clientChannel, es));
                }
            } catch (Exception e) {
                if (!listenChannel.isOpen()) {
                    System.out.println("Server socket closed, stopping server.");
                    break;
                }
                e.printStackTrace();
            }
        }

        System.out.println("Server has stopped.");
    }

    private static Thread getThread(ServerSocketChannel listenChannel, ExecutorService es) {
        Scanner scanner = new Scanner(System.in);

        // Start a separate thread to monitor user input for "Q"
        Thread inputThread = new Thread(() -> {
            System.out.println("Type 'Q' to stop the server.");
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("Q")) {
                    System.out.println("Shutting down server...");
                    try {
                        listenChannel.close();  // Close server socket to stop accepting new connections
                        es.shutdown();  // Shutdown executor service
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;  // Exit the thread
                }
            }
        });
        return inputThread;
    }

    public static void handleCommand(SocketChannel clientChannel, ExecutorService es) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try{
            int bytesRead = clientChannel.read(buffer);
            buffer.flip();
            byte[] receivedBytes = new byte[bytesRead];
            buffer.get(receivedBytes);
            String receivedData = new String(receivedBytes).trim();

            String [] dataArray = receivedData.split(":", 3);
            String command = dataArray[0];
            String fileName = dataArray.length > 1 ? dataArray[1] : "";
            String newFileName = dataArray.length  > 2 ? dataArray[2] : "";

            switch (command){
                case "A":
                    listFiles(clientChannel);
                    break;
                case "B":
                    deleteFile(clientChannel,fileName);
                    break;
                case "C":
                    renameFile(clientChannel, fileName, newFileName);
                    break;
                case "D":
                    //downloadFile(clientChannel, fileName);
                    es.submit(new DownloadTask(clientChannel, fileName));
                    break;
                case "E":
                    //uploadFile(clientChannel, fileName);
                    es.submit(new UploadTask(clientChannel, fileName));
                    break;
            }
            //clientChannel.close();
        }catch (IOException e){
            System.err.println("Error handling command: " + e.getMessage());
            e.printStackTrace();
        }

    }


    static class UploadTask implements Runnable{
        SocketChannel serveChannel;
        String fileName;
        public UploadTask(SocketChannel serveChannel, String fileName){
            this.serveChannel = serveChannel;
            this.fileName = fileName;
        }
        public void run(){
            try {
                uploadFile(serveChannel, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class DownloadTask implements Runnable{
        SocketChannel serveChannel;
        String fileName;
        public DownloadTask(SocketChannel serveChannel, String fileName){
            this.serveChannel = serveChannel;
            this.fileName = fileName;
        }
        public void run(){
            try {
                downloadFile(serveChannel, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class handleCommandTask implements Runnable{
        SocketChannel clientChannel;
        ExecutorService es;
        public handleCommandTask(SocketChannel clientChannel, ExecutorService es){
            this.clientChannel = clientChannel;
            this.es = es;
        }
        public void run(){
            try {
                handleCommand(clientChannel, es);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void downloadFile(SocketChannel serveChannel, String fileName) throws IOException {
        try{
                File file = new File("ServerFiles/" + fileName);
                if(!file.exists()){
                    System.out.println("File doesn't exist");
                }else{
                    FileInputStream fs = new FileInputStream(file);
                    FileChannel fc = fs.getChannel();
                    ByteBuffer fileContent = ByteBuffer.allocate(1024);
                    int byteRead = 0;
                    do{
                        byteRead = fc.read(fileContent);
                        fileContent.flip();
                        serveChannel.write(fileContent);
                        fileContent.clear();
                    }while(byteRead >= 0);
                    fs.close();
                }
                serveChannel.close();
        }catch (Exception e){
            String response = "Failed to download";
            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
            serveChannel.write(buffer);
            serveChannel.close();
        }

    }


    public static void listFiles(SocketChannel serveChannel) throws IOException {
        StringBuilder files = new StringBuilder();
        File folder = new File("ServerFiles/");
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null){
            for(File file : listOfFiles){
                if(!file.getName().equals(".DS_Store")){
                    files.append(file.getName()).append("\n");
                }
            }
        }
        ByteBuffer buffer = ByteBuffer.wrap(files.toString().getBytes());
        serveChannel.write(buffer);
        serveChannel.close();
    }

    public static void uploadFile(SocketChannel serveChannel, String fileName) throws IOException {
        FileOutputStream fo = new FileOutputStream("ServerFiles/" + fileName);
        FileChannel fileChannel = fo.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String response;

        try{
            while(serveChannel.read(buffer) > 0){
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
            fo.close();
            response = fileName + " has been successfully uploaded!";
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            serveChannel.write(responseBuffer);
            serveChannel.close();
        }catch(Exception e){
            response = fileName + " failed to upload";
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            serveChannel.write(responseBuffer);
            serveChannel.close();
        }

    }

    public static void deleteFile(SocketChannel serveChannel, String fileName) throws IOException {
        File file = new File("ServerFiles/" + fileName);
        String success = "You have deleted" + fileName;
        String fail = "File deletion failed";
        if(file.delete()){
            ByteBuffer successBuffer = ByteBuffer.wrap(success.getBytes());
            serveChannel.write(successBuffer);
        }else{
            ByteBuffer failBuffer = ByteBuffer.wrap(fail.getBytes());
            serveChannel.write(failBuffer);
        }
        serveChannel.close();
    }

    public static void renameFile(SocketChannel serveChannel, String originalFileName, String newFileName) throws IOException {
        File originalFile = new File("ServerFiles/" + originalFileName);
        File newFile = new File("ServerFiles/" + newFileName);
        String success = "You have renamed " + originalFileName + " to " + newFileName;
        String fail = "File deletion failed";
        if(originalFile.renameTo(newFile)){
            ByteBuffer successBuffer = ByteBuffer.wrap(success.getBytes());
            serveChannel.write(successBuffer);
        }else{
            ByteBuffer failBuffer = ByteBuffer.wrap(fail.getBytes());
            serveChannel.write(failBuffer);
        }
        serveChannel.close();
    }
}

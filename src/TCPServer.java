import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TCPServer {
    public static void main(String[] args) throws Exception{
        ServerSocketChannel listenChannel = ServerSocketChannel.open();

        listenChannel.bind(new InetSocketAddress(3000));

        while(true){
            SocketChannel clientChannel = listenChannel.accept();
            handleCommand(clientChannel);
        }
    }

    public static void handleCommand(SocketChannel clientChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);
        buffer.flip();
        byte[] receivedBytes = new byte[bytesRead];
        buffer.get(receivedBytes);
        String receivedData = new String(receivedBytes).trim();

        String [] dataArray = receivedData.split(" ", 3);
        String command = dataArray[0];
        String fileName = dataArray.length > 1 ? dataArray[1] : "";
        String newFileName = dataArray.length  > 2 ? dataArray[2] : "";

        switch (command){
            case "A":
                listFiles(clientChannel);
            case "B":
                deleteFile(clientChannel,fileName);
            case "C":
                renameFile(clientChannel, fileName, newFileName);
            case "D":
                downloadFile(clientChannel, fileName);
                break;
            case "E":
                uploadFile(clientChannel, fileName);
        }
    }

    public static void downloadFile(SocketChannel serveChannel, String fileName) throws IOException {
        while(true){
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
        String success = "You have deleted " + fileName;
        String fail = "File deletion failed";
        if(file.delete()){
            ByteBuffer successBuffer = ByteBuffer.wrap(success.getBytes());
            serveChannel.write(successBuffer);
            serveChannel.close();
        }else{
            ByteBuffer failBuffer = ByteBuffer.wrap(fail.getBytes());
            serveChannel.write(failBuffer);
            serveChannel.close();
        }
    }

    public static void renameFile(SocketChannel serveChannel, String originalFileName, String newFileName) throws IOException {
        File originalFile = new File("ServerFiles/" + originalFileName);
        File newFile = new File("ServerFiles/" + newFileName);
        String success = "You have renamed " + originalFileName + " to " + newFileName;
        String fail = "File deletion failed";
        if(originalFile.renameTo(newFile)){
            ByteBuffer successBuffer = ByteBuffer.wrap(success.getBytes());
            serveChannel.write(successBuffer);
            serveChannel.close();
        }else{
            ByteBuffer failBuffer = ByteBuffer.wrap(fail.getBytes());
            serveChannel.write(failBuffer);
            serveChannel.close();
        }
    }
}

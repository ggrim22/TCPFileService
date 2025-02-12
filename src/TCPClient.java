import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Please provide <ServerIP> and <ServerPort>");
        }
        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String command = "";
        while(!(command.equals("Q"))){
            System.out.println("Please type the following letter that corresponds to the command you want:");
            System.out.println("A: List all files\nB: Delete a file\nC: Rename a file\nD: Download a file\nE: Upload a file\nQ: Quit");

            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();

            switch (command) {
                case "A":
                    listFiles(serverIP, serverPort);
                    break;
                case "B":
                    System.out.println("Please type in the file name you want to delete: ");
                    String deleteFile = scanner.nextLine();
                    deleteFile(serverIP, serverPort, deleteFile);
                    break;
                case "C":
                    System.out.println("Please type in a file name of the file you want to change: ");
                    String originalFileName = scanner.nextLine();
                    System.out.println("Please type in a new file name: ");
                    String newFileName = scanner.nextLine();
                    renameFile(serverIP, serverPort, originalFileName, newFileName);
                    break;
                case "D":
                    System.out.println("Please type in a file name: ");
                    String fileName = scanner.nextLine();
                    downloadFile(serverIP, serverPort, fileName);
                    break;
                case "E":
                    System.out.println("Please type in the file you want to upload: ");
                    String uploadFile = scanner.nextLine();
                    uploadFile(serverIP, serverPort, uploadFile);
                    break;
                case "Q":
                    System.out.println("You have quit the program, goodbye...");
                    break;
                default:
                    System.out.println("Invalid command. Please enter a valid option: <A, B, C, D, E, Q>");
            }
        }

    }

    public static void downloadFile(String serverAddress, int serverPort, String fileName) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverAddress, serverPort));
        String request = "D:" + fileName + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);

        channel.shutdownOutput();
        FileOutputStream fo = new FileOutputStream("ClientFiles/" + fileName, true);
        FileChannel fc = fo.getChannel();
        ByteBuffer fileContent = ByteBuffer.allocate(1024);
        try {
            while (channel.read(fileContent) >= 0) {
                fileContent.flip();
                fc.write(fileContent);
                fileContent.clear();
            }
            fo.close();
            channel.close();
        } catch (Exception e) {
            System.out.println("File failed to download");
        }
    }

    public static void listFiles(String serverAddress, int serverPort) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverAddress, serverPort));
        String request = "A:";
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);
        channel.shutdownOutput();

        ByteBuffer replyBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(replyBuffer);
        channel.close();
        replyBuffer.flip();
        byte[] a = new byte[bytesRead];
        replyBuffer.get(a);
        System.out.println(new String(a));
    }

    public static void uploadFile(String serverAddress, int serverPort, String fileName) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverAddress, serverPort));
        File file = new File("ClientFiles/" + fileName);
        String request = "E:" + fileName + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);

        FileInputStream fileInput = new FileInputStream(file);
        FileChannel fileChannel = fileInput.getChannel();
        ByteBuffer fileBuffer = ByteBuffer.allocate(1024);

        while(fileChannel.read(fileBuffer) > 0){
            fileBuffer.flip();
            channel.write(fileBuffer);
            fileBuffer.clear();
        }

        channel.shutdownOutput();
        fileChannel.close();
        fileInput.close();

        ByteBuffer replyBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(replyBuffer);
        channel.close();
        replyBuffer.flip();
        byte[] a = new byte[bytesRead];
        replyBuffer.get(a);
        System.out.println(new String(a));

        channel.close();
    }

    public static void deleteFile(String serverAddress, int serverPort, String fileName) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverAddress, serverPort));
        String request = "B:" + fileName + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);
        channel.shutdownOutput();

        ByteBuffer replyBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(replyBuffer);
        channel.close();
        replyBuffer.flip();
        byte[] a = new byte[bytesRead];
        replyBuffer.get(a);
        System.out.println(new String(a));
    }

    public static void renameFile(String serverAddress, int serverPort, String originalFileName, String newFileName) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverAddress, serverPort));
        String request = "C:" + originalFileName + ":" + newFileName + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);
        channel.shutdownOutput();

        ByteBuffer replyBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(replyBuffer);
        channel.close();
        replyBuffer.flip();
        byte[] a = new byte[bytesRead];
        replyBuffer.get(a);
        System.out.println(new String(a));
    }
}

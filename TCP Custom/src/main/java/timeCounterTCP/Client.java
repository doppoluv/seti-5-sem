package timeCounterTCP;

import timeCounterTCP.filetransfer.FileTransferProto.FileInfo;
import timeCounterTCP.filetransfer.FileTransferProto.FileChunk;
import java.io.*;
import java.net.*;

public class Client {
    private static final int CHUNK_SIZE = 4096;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Использование: java Client <путь_к_файлу> <хост> <порт>");
            System.exit(1);
        }

        String filePath = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("Файл не существует: " + filePath);
            System.exit(1);
        }

        String filename = file.getName();
        long fileSize = file.length();

        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(file)) {

            FileInfo fileInfo = FileInfo.newBuilder()
                                        .setFilename(filename)
                                        .setSize(fileSize)
                                        .build();
            byte[] fileInfoBytes = fileInfo.toByteArray();
            dos.writeInt(fileInfoBytes.length);
            dos.write(fileInfoBytes);
            dos.flush();

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer, 0, CHUNK_SIZE)) != -1) {
                FileChunk chunk = FileChunk.newBuilder()
                                           .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                                           .build();
                byte[] chunkBytes = chunk.toByteArray();
                dos.writeInt(chunkBytes.length);
                dos.write(chunkBytes);
                dos.flush();
            }

            String response = dis.readUTF();
            if ("OK".equals(response)) {
                System.out.println("Передача файла успешно завершена.");
            } else {
                System.out.println("Передача файла не удалась.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }
    }
}
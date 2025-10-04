package timeCounterTCP;

import timeCounterTCP.filetransfer.FileTransferProto.*;
import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Использование: java Server <порт>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);

            File uploadsDir = new File("uploads");
            uploadsDir.mkdirs();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, uploadsDir)).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final File uploadsDir;

        public ClientHandler(Socket socket, File uploadsDir) {
            this.socket = socket;
            this.uploadsDir = uploadsDir;
        }
        
        private String getUniqueFilename(String filename, File uploadsDir) {
            File file = new File(uploadsDir, filename);
            if (!file.exists()) {
                return filename;
            }

            String nameWithoutExtension;
            String extension = "";
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
                nameWithoutExtension = filename.substring(0, lastDotIndex);
                extension = filename.substring(lastDotIndex);
            } else {
                nameWithoutExtension = filename;
            }

            int counter = 1;
            String newFilename = filename;
            while (file.exists()) {
                newFilename = String.format("%s(%d)%s", nameWithoutExtension, counter, extension);
                file = new File(uploadsDir, newFilename);
                counter++;
            }

            return newFilename;
        }

        @Override
        public void run() {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                int fileInfoLength = dis.readInt();
                byte[] fileInfoBytes = new byte[fileInfoLength];
                dis.readFully(fileInfoBytes);
                FileInfo fileInfo = FileInfo.parseFrom(fileInfoBytes);
                String filename = fileInfo.getFilename();
                long fileSize = fileInfo.getSize();

                String safeFilename = new File(filename).getName();
                String uniqueFilename = getUniqueFilename(safeFilename, uploadsDir);
                File destFile = new File(uploadsDir, uniqueFilename);

                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    long totalBytes = 0;
                    long startTime = System.currentTimeMillis();
                    long lastPrintTime = startTime;
                    long lastBytes = 0;
                    boolean printed = false;

                    while (totalBytes < fileSize) {
                        int chunkLength = dis.readInt();
                        byte[] chunkBytes = new byte[chunkLength];
                        dis.readFully(chunkBytes);
                        FileChunk chunk = FileChunk.parseFrom(chunkBytes);
                        byte[] data = chunk.getData().toByteArray();
                        fos.write(data);
                        totalBytes += data.length;

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastPrintTime >= 3000) {
                            double timeDiffSec = (currentTime - lastPrintTime) / 1000.0;
                            double instSpeed = (totalBytes - lastBytes) / timeDiffSec / 1024.0 / 1024.0;
                            double avgTimeSec = (currentTime - startTime) / 1000.0;
                            double avgSpeed = totalBytes / avgTimeSec / 1024.0 / 1024.0;
                            System.out.printf("Клиент %s:\n     Мгновенная скорость: %.2f МБ/с\n     Средняя скорость: %.2f МБ/с%n",
                                              clientAddress, instSpeed, avgSpeed);
                            lastPrintTime = currentTime;
                            lastBytes = totalBytes;
                            printed = true;
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    double totalTimeSec = (endTime - startTime) / 1000.0;
                    double speed = totalBytes / totalTimeSec / 1024.0 / 1024.0;
                    // System.out.println(totalTimeSec);
                    if (!printed) {
                        System.out.printf("Клиент %s:\n     Мгновенная скорость: %.2f МБ/с     Средняя скорость: %.2f МБ/с%n",
                                          clientAddress, speed, speed);
                    }

                    FileResponse.Builder responseBuilder = FileResponse.newBuilder();
                    if (totalBytes == fileSize) {
                        responseBuilder.setStatus("OK").setSavedFilename(uniqueFilename);
                    } else {
                        responseBuilder.setStatus("ERROR").setSavedFilename("");
                        destFile.delete();
                    }
                    FileResponse response = responseBuilder.build();
                    byte[] responseBytes = response.toByteArray();
                    dos.writeInt(responseBytes.length);
                    dos.write(responseBytes);
                    dos.flush();
                }
            } catch (IOException e) {
                System.err.println("Ошибка обработки клиента " + clientAddress + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
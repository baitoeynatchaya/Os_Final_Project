 
import java.io.*;
import java.nio.channels.*;
import java.net.*;
 
public class Server {
 
    private File[] files;
    private int clientNo;
    private final String folder = "C:\\Users\\Natchaya Chantem\\OneDrive - Silpakorn University\\Desktop\\os_final_PQ";
    public static final int PORT = 3300;
    public static final int DOWNLOAD_PORT = 3301;
    public static final int Channel_PORT = 3302;
    public static final String IpAddress = "172.18.113.78";
    public static final int threadNo = 10;
 
    private void startServer() {
        files = new File(folder).listFiles();
 
        new Thread(() -> {
            try {
                System.out.println("Server is listening on port" + PORT);
                ServerSocket serverSocket = new ServerSocket(PORT);
                ServerSocket uploadServerSocket = new ServerSocket(DOWNLOAD_PORT);
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(IpAddress,Channel_PORT));
                while (true) {
                    Socket socket = serverSocket.accept();
                    DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                    DataOutputStream outputFromClient = new DataOutputStream(socket.getOutputStream());
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    new Thread(new ClientThread(socket, ++clientNo, uploadServerSocket,inputFromClient,outputFromClient,socketChannel)).start();
                }
            } catch (IOException ex) {
            }
        }).start();
    }
 
    class ClientThread implements Runnable {
 
        private final Socket socket;
        private final int no;
        private ServerSocket uploadServer = null;
        DataInputStream inputFromClient;
        DataOutputStream outputToClient;
        SocketChannel socketChannel;
 
        public ClientThread(Socket socket, int no, ServerSocket uploadServer,DataInputStream inputFromClient,DataOutputStream outputToClient,SocketChannel socketChannel) {
            this.socket = socket;
            this.no = no;
            this.uploadServer = uploadServer;
            this.inputFromClient = inputFromClient;
            this.outputToClient = outputToClient;
            this.socketChannel = socketChannel;
        }
 
        @Override
        public void run() {
            try {
                System.out.println("Client " + clientNo + " is connected in port " + PORT);
 
                outputToClient.writeInt(files.length);
 
                for (int i = 0; i < files.length; i++) {
                    outputToClient.writeUTF(files[i].getName());
                }
 
                while (true) {
                    try {
                        int fileIndex = inputFromClient.readInt();
                        System.out.println(files[fileIndex].getName());
                        int category = inputFromClient.readInt();
                        if (category == 1){
                            multithread(fileIndex);
                        }
                        else if (category == 2){
                            outputToClient.writeLong(files[fileIndex].length());
                            zeroCopy(fileIndex);
                        }
                    } catch (IOException ex) {
                    }
                }
 
            } catch (IOException ex) {
            }
        }
 
        private void multithread(int fileIndex) {
            try {
                for (int i = 0; i < threadNo; i++) {
                    Socket uploadSocket = uploadServer.accept();
                    long size = files[fileIndex].length() / threadNo;
                    long start = i * size;
                    int index = i;
                    new Thread(() -> {
                        try {
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(files[fileIndex].getAbsolutePath()));
                            DataOutputStream outputToClient = new DataOutputStream(uploadSocket.getOutputStream());
 
                            outputToClient.writeLong(start);
 
                            byte[] buffer = new byte[1024];
                            int read;
                            long currentRead = 0;
 
                            bufferedInputStream.skip(start);
 
                            while ((read = bufferedInputStream.read(buffer)) != -1 && currentRead < size) {
                                outputToClient.write(buffer, 0, read);
                                currentRead += read;
                            }
 
                            bufferedInputStream.close();
                            outputToClient.close();
                            socket.close();
 
                            System.out.println("Thread " + (index + 1) + " send file successfully");
                        } catch (IOException ex) {
                        }
                    }).start();
                }
            } catch (IOException ex) {
            }
        }
        public void zeroCopy(int fileIndex){
            FileChannel source = null;
            System.out.println("start ZeroCopy");
            long size = files[fileIndex].length();
            try{
                source = new FileInputStream(files[fileIndex].getAbsolutePath()).getChannel();
                long currentRead = 0;
                long read;
                while(currentRead < size && (read = source.transferTo(currentRead, size - currentRead, socketChannel)) != -1)
                    currentRead += read;
                System.out.println("Finish ZeroCopy");
            }
            catch (IOException ex){
            }
            finally{
                try{
                    if(source != null)
                        source.close();
                } catch (IOException ex){
                }
            }
        }
    }
 
    public static void main(String[] args) {
        new Server().startServer();
    }
}

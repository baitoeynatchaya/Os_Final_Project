import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
 
public class Client2 { 
    private Socket socket;
    private DataInputStream inputFromServer;
    private DataOutputStream outputToServer;
    private SocketChannel socketChannel;
    private String[] files;
    private String folder = "/home/natchaya/Desktop/";
    private String ipAddress = "172.16.20.93";
    private int port = 3300;
    private int download_port = 3301;
    private int channel_port = 3302;
    private int threadNo = 2;
 
    public void startClient() throws InterruptedException {
        try {
            socket = new Socket(ipAddress, port);
            inputFromServer = new DataInputStream(socket.getInputStream());
            outputToServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(ipAddress, channel_port));
 
            int fileLength = inputFromServer.readInt();
            files = new String[fileLength];
 
            for(int i=0; i<fileLength; i++) {
                files[i] = inputFromServer.readUTF();
            }
 
            printFiles();
 
        } catch(IOException ex) {}
    }    
 
    private void printFiles() throws InterruptedException {
        Scanner scan = new Scanner(System.in);
        try {
            while(true) {
                for(int i=0; i<files.length; i++) {
                    System.out.println("[" + (i+1) + "] " + files[i]);
                } 
 
                System.out.print("Enter the number of file to download: ");
                int fileIndex = scan.nextInt();
                if (fileIndex >= 1 && fileIndex <= files.length) {
                    outputToServer.writeInt(fileIndex-1); 
                    int category = 0;
                    while(true) {
                        System.out.print("Enter the number 1:multithread 2:zeroCopy --> ");
                        category = scan.nextInt();
                        if (category < 1 || category > 2){
                            continue;
                        }
                        if (category == 1) {
                            outputToServer.writeInt(category);
                            multithread(files[fileIndex-1]);
                        }
                        else if (category == 2) {
                            outputToServer.writeInt(category);
                            long size = inputFromServer.readLong();
                            zeroCopy(files[fileIndex-1],size);
                        }
                        break;
                    }
                    break;
                } else {
                    System.out.println("Error, please select file index again");
                } 
            }
        } catch(IOException ex) {}
    }
 
    private void multithread(String fileName) throws InterruptedException { 
        CountDownLatch countdown = new CountDownLatch(threadNo);
        long startprocess = System.currentTimeMillis();
        for (int i=0; i<threadNo; i++) {
            int index = i;
            new Thread(new ClientThread(countdown,index,fileName)).start();
        }
        countdown.await();
            
            long finish = System.currentTimeMillis();
            long timeused = finish - startprocess;
            System.out.println("FinishProcess" + " " + timeused);
    }
    
    public final void zeroCopy(String fileName,long size){
        FileChannel destination = null;
        System.out.println("Start ZeroCopy Download");
        try{
            long starttime = System.currentTimeMillis();
            destination = new FileOutputStream(folder + "/" + fileName).getChannel();
            long currentRead = 0;
            long read;
            while(currentRead < size && (read = destination.transferFrom(socketChannel, currentRead, size-currentRead)) != -1)
                currentRead += read;
            System.out.println("ZeroCopy Success");
            long endtime = System.currentTimeMillis();
                    long used = endtime - starttime;
                   System.out.println("timeused" + " " + used);
        } catch (IOException e){}
        finally{
            try{
                if(destination != null)
                    destination.close();
            } catch (IOException e){}
        }
    }
    class ClientThread extends Thread{
        CountDownLatch countdown;
        int index;
        String fileName;
        
        
        public ClientThread(CountDownLatch countdown,int index,String fileName){
            this.countdown = countdown;
            this.index= index;
            this.fileName = fileName;
        }
        @Override
        public void run(){
            try {
                    long starttime = System.currentTimeMillis();
                    Socket downloadSocket = new Socket(ipAddress, download_port);
                    DataInputStream dataFromServer = new DataInputStream(downloadSocket.getInputStream());
                    long start = dataFromServer.readLong();
 
                    RandomAccessFile raf = new RandomAccessFile(folder+"/"+fileName, "rwd");
                    raf.seek(start);
 
                    byte[] buffer = new byte[1024];
                    int read = 0;
 
                    while((read = dataFromServer.read(buffer)) != -1) {
                        raf.write(buffer, 0, read);
                    }
 
                    raf.close();
                    dataFromServer.close();
                    downloadSocket.close();
 
                    System.out.println("Thread "+ (index+1) + " download successfully");
                    long endtime = System.currentTimeMillis();
                    long used = endtime - starttime;
                   System.out.println("timeused" + " " + used);
                   countdown.countDown();
                    
                } catch (IOException ex) {}
        }
    }
 
    public static void main(String[] args) throws InterruptedException {
        new Client2().startClient();
    }
}
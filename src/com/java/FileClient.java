package com.java;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client
 *
 * @author JR Chan
 */
public class FileClient {

    static final String HOST = "127.0.0.1";
    static final int TcpPort = 2021;
    static final int UdpPort = 2020;
    Socket socketT = new Socket();
    DatagramSocket socketU;

    public FileClient() throws IOException {
        socketT.connect(new InetSocketAddress(HOST, TcpPort));
        socketU = new DatagramSocket(UdpPort);
    }

    private void send() {
        try {
            //客户端输入流，接收服务器消息
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    socketT.getInputStream()));
            //客户端输出流，向服务器发消息
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    socketT.getOutputStream()));
            //装饰输出流，及时刷新
            PrintWriter pw = new PrintWriter(bw, true);
            //读取当前工作目录
            System.out.println(br.readLine());

            //用户输入
            Scanner in = new Scanner(System.in);
            in.useDelimiter("\n");  //使得 scanner 只用回车作为分割
            String msg; //接受用户信息

            while ((msg = in.next()) != null) {
//                System.out.println("command begin");

                //退出
                if (msg.trim().equals("bye")) {
                    break;
                }

                //发送给服务器端
                pw.println(msg);

                //get 命令接收
                if (msg.trim().startsWith("get ")) {
                    String tmp = br.readLine();
                    if (tmp.equals("unknown file")) {
                        System.out.println("unknown file");
                    } else {
                        //size of the file (Byte)
                        System.out.println(tmp);
                        long space = Long.parseLong(tmp);   //total size
                        long times = space / 512 + 1;   //circulate times
                        for (int i = 0; i < times; i++) {
                            DatagramPacket datagramPacket = new DatagramPacket(new byte[512], 512);
                            socketU.receive(datagramPacket);
                            byte[] part = datagramPacket.getData();


                        }
                    }
                }
                String res;

                while (!(res = br.readLine()).equals("end up this command")) {
                    System.out.println(res); //逐行输出服务器返回的消息
                }
//                System.out.println("command done");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Socket实现了AutoCloseable 实际上可以只用 try-with-resources 结构
        finally {
            try {
                socketT.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            FileClient fc = new FileClient();
            fc.send();
        } catch (IOException e) {
            System.out.println("no connection, please retry");
        }
    }

}

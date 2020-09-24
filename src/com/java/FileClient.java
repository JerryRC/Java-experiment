package com.java;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Client
 *
 * @author JR Chan
 */
public class FileClient {

    static final String HOST = "127.0.0.1";
    static final int TcpPort = 2021;
    Socket socketT;
    DatagramSocket socketU;
    BufferedReader br;
    BufferedWriter bw;
    PrintWriter pw;
    Scanner in;

    /**
     * 构造函数
     *
     * @throws IOException socket创建失败
     */
    public FileClient() throws IOException {
        socketT = new Socket();
        socketT.connect(new InetSocketAddress(HOST, TcpPort));
        socketU = new DatagramSocket();        //随机UDP端口
        initStream();
    }

    /**
     * 初始化输入输出流对象方法
     *
     * @throws IOException 流错误
     */
    public void initStream() throws IOException {
        //客户端输入流，接收服务器消息
        br = new BufferedReader(new InputStreamReader(
                socketT.getInputStream()));
        //客户端输出流，向服务器发消息
        bw = new BufferedWriter(new OutputStreamWriter(
                socketT.getOutputStream()));
        //装饰输出流，及时刷新
        pw = new PrintWriter(bw, true);
        //用户输入流
        in = new Scanner(System.in);
        in.useDelimiter("\n");  //使得 scanner 只用回车作为分割
    }

    /**
     * 得到准备接受命令之后的处理
     *
     * @param tmp 得到的文件名以及大小信息
     * @throws IOException 文件创建失败以及无效socket的错误
     */
    private void downloadFile(String tmp) throws IOException {
        System.out.println(tmp);
        StringTokenizer st = new StringTokenizer(tmp, ";");

        String name = st.nextToken();   //name of the file
        FileOutputStream fileOutputStream = new FileOutputStream("./" + name);

        String size = st.nextToken();   //size of the file (Byte)
        long space = Long.parseLong(size);   //total size

        double times = Math.ceil((float) space / 512);   //circulate times
        byte[][] buf = new byte[(int) times][513];  //file stream buffer
        int last_length = 0;    //the last datagram's length

        //write to the buf before creating the file
        for (int i = 0; i < (int) times; i++) {
            DatagramPacket datagramPacket = new DatagramPacket(new byte[513], 513);
            socketU.receive(datagramPacket);
            byte[] part = datagramPacket.getData();
            //put the part into the corresponding numbered buffer
            buf[part[0]] = part;
            last_length = datagramPacket.getLength();
        }

        //creating the file from buffer
        for (int i = 0; i < (int) times - 1; i++) {
            fileOutputStream.write(buf[i], 1, 512);
        }
        for (int i = 1; i < last_length; i++) {
            fileOutputStream.write(buf[(int) times - 1][i]);
        }
        fileOutputStream.close();
    }

    /**
     * 客户端发送命令
     */
    private void send() {
        try {
            //读取当前工作目录
            System.out.println(br.readLine());
            //把自身的 udp 端口号告知服务器
            pw.println(socketU.getLocalPort());
            //接受用户信息
            String msg;
            while ((msg = in.next()) != null) {
                //发送指令给服务器端
                pw.println(msg);
                //处理退出指令
                if (msg.trim().equals("bye")) {
                    break;
                }
                try {
                    //处理get指令
                    if (msg.trim().startsWith("get ")) {
                        String tmp = br.readLine();
                        if (tmp.equals("unknown file")) {
                            System.out.println("unknown file");
                        } else {
                            downloadFile(tmp);
                        }
                    }

                    //处理除了get外的其他指令返回值
                    String res;
                    while (!(res = br.readLine()).equals("end up this command")) {
                        System.out.println(res); //逐行输出服务器返回的消息
                    }
                } catch (IOException e) {
                    //服务器超时，或者服务器异常退出
                    System.out.println("Sorry, the server has closed the connection.");
                    break;
                }
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
            e.printStackTrace();
            System.out.println("no connection, please retry");
        }
    }

}

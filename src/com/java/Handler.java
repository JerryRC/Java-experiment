package com.java;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * 辅助 Handler 每个线程的实际策略
 *
 * @author JR Chan
 */
public class Handler implements Runnable {
    private final Socket socket;            //线程池传过来的参数Socket
    private final DatagramSocket serverSocketU;   //服务器DatagramSocket
    private final SocketAddress socketAddress;    //UDP 的发送目的地址
    private String currentPath;     //每次新连接都跳转到服务器指定目录
    BufferedReader br;
    BufferedWriter bw;
    PrintWriter pw;

    /**
     * 构造函数 创建 Handler
     *
     * @param cwd    当前工作路径
     * @param socket 线程池传过来的Tcp socket
     * @throws SocketException socket错误
     */
    public Handler(final String cwd, Socket socket, DatagramSocket serverSocketU) throws IOException {
        this.currentPath = cwd;
        this.socket = socket;
        this.serverSocketU = serverSocketU;
        System.out.println("<" + socket.getInetAddress() + "："
                + socket.getPort() + "> 连接成功");

        initStream();   //初始化流

        pw.println("CWD: " + currentPath);  //告知客户端当前工作路径

        String clientPort = br.readLine();     //客户端的随机端口号
        InetAddress clientIP = socket.getInetAddress(); //客户端 IP
        socketAddress = new InetSocketAddress(
                clientIP, Integer.parseInt(clientPort));    //初始化Udp的地址信息 未做非法地址保护
    }

    /**
     * 初始化输入输出流对象方法
     *
     * @throws IOException 流错误
     */
    public void initStream() throws IOException {
        //输入流，读取客户端信息
        br = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        //输出流，向客户端写信息
        bw = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream()));
        //装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
        pw = new PrintWriter(bw, true);
    }

    /**
     * 每个线程的实际执行策略
     */
    @Override
    public void run() {
        try {
            String info; //接收用户输入的信息
            while ((info = br.readLine()) != null) {
                info = info.trim(); //去掉首尾空格
                System.out.println("user's command: " + info); //输出用户发送的消息

                //打印目录
                if (info.equals("ls")) {
                    pw.println(dirList(currentPath));
                }
                //跳转目录
                else if (info.startsWith("cd ") || info.startsWith("cd..")) {
                    String dir = info.substring(2).trim();  //切片去空格
                    String result = moveCwd(dir, currentPath);
                    if (result.equals("unknown dir")) {
                        pw.println(result);
                    } else {
                        currentPath = result;   //跳转成功则更新当前目录
                        pw.println(result + " > OK");
                    }
                }
                //获取文件
                else if (info.startsWith("get ")) {
                    String file = info.substring(3).trim(); //切片去空格
                    String result = getFile(file, currentPath);
                    if (result.equals("unknown file")) {
                        pw.println(result);
                    } else {
                        sentFile(result);
                    }
                }
                //退出
                else if (info.equals("bye")) {
                    System.out.println("<" + socket.getInetAddress() + "："
                            + socket.getPort() + "> 已退出");
                    break;
                }
                //无法解析指令
                else {
                    pw.println("unknown cmd");
                }
                //再加一行标识让 client 端的 readLine() 函数能退出阻塞
                pw.println("end up this command");
            }
        }
        //避免异常后直接结束服务器端
        catch (IOException e) {
            System.out.println("no connection yet");
        } catch (InterruptedException e) {
            System.out.println("interrupt while sleeping");
        }
        // Socket实现了AutoCloseable 实际上可以只用 try-with-resources 结构
        finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理 ls 命令
     *
     * @param currentPath 给定的文件目录
     * @return 文件结构的字符串
     */
    private String dirList(String currentPath) {
        StringBuilder result = new StringBuilder();
        File file = new File(currentPath);

        File[] list = file.listFiles();  //获取当前目录下所有文件
        if (list != null) {    //如果给的路径不对 会NullPointer
            for (File f : list) {   //依次循环打印文件  有可能给的不是目录
                String tmp;
                if (f.isFile()) {
                    tmp = "<file>  " + f.getName() + "    " + f.length() + " Bytes\n";
                } else {
                    tmp = "<dir>   " + f.getName() + "\n";
                }
                result.append(tmp);
            }
        } else {
            result.append("current path is not a directory");
        }
        return result.toString();
    }

    /**
     * 处理 cd 命令
     * 这里先当成相对路径处理，若出错再当成绝对路径
     * 因为如果直接new File(..) 会被当成java文件根目录下的 \.. 仍有可能是有效路径
     *
     * @param dir         要跳转的目标目录
     * @param currentPath 当前工作路径
     * @return 跳转情况
     * @throws IOException null
     */
    private String moveCwd(String dir, String currentPath) throws IOException {
        //处理相对路径命令  这里可以处理正反斜杠 多级跳转等问题  ..\..   \\..  ./
        File file = new File(currentPath + "\\" + dir);
        //处理绝对路径问题
        if (!file.exists()) {
            file = new File(dir);
        }

        if (file.isDirectory()) {  //是目录 返回跳转成功
            return file.getCanonicalPath();
        } else {    //不是目录或者路径不存在
            return "unknown dir";
        }
    }

    /**
     * 处理 get 命令
     *
     * @param filename    目标文件名
     * @param currentPath 当前工作目录
     * @return 文件获取情况
     * @throws IOException null
     */
    private String getFile(String filename, String currentPath) throws IOException {
        //处理相对路径命令  这里可以处理正反斜杠 多级跳转等问题  ..\..   \\..  ./
        File file = new File(currentPath + "\\" + filename);
        //处理绝对路径问题
        if (!file.exists()) {
            file = new File(filename);
        }

        if (file.isFile()) {  //是文件的话返回绝对路径
            return file.getCanonicalPath();
        } else {  //不是文件
            return "unknown file";
        }
    }

    /**
     * 实现文件的UDP传输
     *
     * @param path get命令得到的绝对路径
     * @throws IOException          文件创建错误
     * @throws InterruptedException sleep时发生中断错误
     */
    private void sentFile(String path) throws IOException, InterruptedException {
        //得到文件名
        StringTokenizer st = new StringTokenizer(path, "\\");
        String name = "";
        while (st.hasMoreTokens()) {
            name = st.nextToken();
        }
        //new出文件
        File tmp = new File(path);
        String space = String.valueOf(tmp.length());
        //把文件名及大小发给客户端
        pw.println(name + ";" + space);

        //文件转换成 byte[] 输出
        FileInputStream fis = new FileInputStream(tmp);
        byte[] part = new byte[513];

        //纠错编号
        int index = 0;
        int size;
        while ((size = fis.read(part, 1, 512)) != -1) {
            //加入标号
            part[0] = (byte) index;
            index += 1;
            DatagramPacket datagramPacket = new DatagramPacket(part,
                    size + 1, socketAddress);   //创建 Udp 包裹
            serverSocketU.send(datagramPacket);
            TimeUnit.MICROSECONDS.sleep(1); //限制传输速度
        }
        fis.close();
    }
}

package com.java;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * Server
 *
 * @author JR Chan
 */
public class FileServer {
    ServerSocket serverSocketT;
    DatagramSocket serverSocketU; // 服务器DatagramSocket

    /**
     * 构造函数 创建 serverSocket
     *
     * @throws IOException 生成 socket 时可能抛出IOException
     */
    public FileServer() throws IOException {
        int tcpPort = 2021;
        //Tcp 端口
        serverSocketT = new ServerSocket(tcpPort, 2);
        serverSocketT.setSoTimeout(3000);
        //随机端口
        serverSocketU = new DatagramSocket();
        System.out.println("服务器启动。");
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
//            System.out.println(currentPath + "\\" + dir + "try space");
            file = new File(dir);
        }

        if (file.isDirectory()) {  //是目录 返回跳转成功
//            System.out.println(file.getCanonicalPath() + "try space");
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
     * server的主功能函数
     *
     * @param cwd 用户定义的服务器开始路径
     */
    private void service(final String cwd) {
        Socket socket = null;
        //暂时只让服务器仅能连续超时100次
        int count = 0;
        while (count < 100) {
            try {
                //每次新连接都跳转到服务器指定目录
                String currentPath = cwd.trim();
//                System.out.println(cwd + "  " + currentPath.getCanonicalPath());

                socket = serverSocketT.accept();    //等待连接，创建套接字，抛出 timeout
                count = 0;  //超时置零

                int udpPort = 2020; //题目要求端口
                InetAddress clientIP = socket.getInetAddress(); //客户端 IP
                SocketAddress socketAddress = new InetSocketAddress(clientIP, udpPort);

                System.out.println("<" + socket.getInetAddress() + "："
                        + socket.getPort() + "> 连接成功");

                //输入流，读取客户端信息
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                //输出流，向客户端写信息
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream()));
                //装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
                PrintWriter pw = new PrintWriter(bw, true);

                bw.write("CWD: " + currentPath);       //告知 client 当前工作路径
                bw.newLine();
                bw.flush();

                String info; //接收用户输入的信息

                while ((info = br.readLine()) != null) {

                    info = info.trim(); //去掉首尾空格
                    System.out.println("user's command: " + info); //输出用户发送的消息

                    if (info.equals("ls")) {    //打印目录
                        pw.println(dirList(currentPath));
                    } else if (info.startsWith("cd ") || info.startsWith("cd..")) { //跳转目录
                        String dir = info.substring(2).trim();  //切片，去空格
//                        System.out.println(dir + " " + currentPath);

                        String result = moveCwd(dir, currentPath);
                        //判断是否成功
                        if (result.equals("unknown dir")) {
                            pw.println(result);
                        } else {
                            currentPath = result;
                            pw.println(result + " > OK");
                        }
                    } else if (info.startsWith("get ")) {
                        String file = info.substring(3).trim(); //切片，去空格
                        String result = getFile(file, currentPath);
                        if (result.equals("unknown file")) {
                            pw.println(result);
                        } else {
                            //得到文件名
                            StringTokenizer st = new StringTokenizer(result, "\\");
                            String name = "";
                            while (st.hasMoreTokens()) {
                                name = st.nextToken();
                            }

                            File tmp = new File(result);
                            String space = String.valueOf(tmp.length());
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
                                //创建 Udp 包裹
                                DatagramPacket datagramPacket = new DatagramPacket(part,
                                        size + 1, socketAddress);
                                serverSocketU.send(datagramPacket);
                                TimeUnit.MICROSECONDS.sleep(1); //限制传输速度
                            }

                            fis.close();
                        }
                    } else if (info.equals("bye")) { //如果用户输入“bye”就退出
                        break;
                    } else {
                        pw.println("unknown cmd"); //无法解析指令
                    }
                    //再加一行标识让 client 端的 readLine() 函数能退出阻塞
                    bw.write("end up this command");
                    bw.newLine();
                    bw.flush();
                }
            }
            //避免异常后直接结束服务器端
            catch (IOException e) {
                System.out.println("no connection yet");
                count += 1;
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
    }

    public static void main(String[] args) throws IOException {
        // receive a param
        if (args.length == 0) {
            System.out.println("usage: java com.java.FileServer <dir>");
        } else {
            FileServer fs = new FileServer();
            System.out.println("<当前路径：" + args[0] + ">");
            File checkCwd = new File(args[0]);
            if (checkCwd.isDirectory()) {
                fs.service(args[0]);   //启动单线程服务
            } else {
                System.out.println("该参数不是有效路径，服务器启动失败。");
            }
        }
    }

}

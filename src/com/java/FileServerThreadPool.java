package com.java;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多线程服务器端
 *
 * @author JR Chan
 */
public class FileServerThreadPool {
    ServerSocket serverSocketT;     //TCP Socket
    DatagramSocket serverSocketU;   //UDP Socket
    ExecutorService executorService; // 线程池
    final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

    /**
     * 构造函数
     *
     * @throws IOException socket创建错误
     */
    public FileServerThreadPool() throws IOException {
        int tcpPort = 2021;
        int udpPort = 2020;
        serverSocketT = new ServerSocket(tcpPort, 10);
        serverSocketT.setSoTimeout(10 * 1000);
        serverSocketU = new DatagramSocket(udpPort);
        // 创建线程池，Runtime的availableProcessors()方法返回当前系统可用处理器的数目，由JVM根据系统的情况来决定线程的数量
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors() * POOL_SIZE);
        System.out.println("服务器启动。");
    }

    /**
     * 主函数
     *
     * @param args 第一个位置为传入的服务器初始地址
     * @throws IOException IO异常
     */
    public static void main(String[] args) throws IOException {
        //需要一个路径参数
        if (args.length == 0) {
            System.out.println("usage: java com.java.FileServer <dir>");
        } else {
            System.out.println("<当前路径：" + args[0] + ">");
            //检查传入参数是否为合法路径
            File checkCwd = new File(args[0]);
            if (checkCwd.isDirectory()) {
                FileServerThreadPool fs = new FileServerThreadPool();
                fs.service(args[0]);   //启动多线程服务
            } else {
                System.out.println("该参数不是有效路径，服务器启动失败。");
            }
        }
    }

    /**
     * 通过线程池管理整个服务
     *
     * @param cwd 服务器当前路径
     */
    public void service(final String cwd) {
        Socket socket;
        //暂时只让服务器仅能连续超时100次
        int count = 0;
        while (count < 100) {
            try {
                String currentPath = cwd.trim();    //每次新连接都跳转到服务器指定目录 arg[0]
                socket = serverSocketT.accept();    //等待用户连接
                socket.setSoTimeout(60 * 1000);     //客户端长时间未响应
                count = 0;  //连续超时次数置零
                executorService.execute(new Handler(currentPath, socket, serverSocketU)); // 把执行交给线程池来维护
            } catch (IOException e) {
                System.out.println("have not got a new connection yet");
                count += 1;
            }
        }
    }
}

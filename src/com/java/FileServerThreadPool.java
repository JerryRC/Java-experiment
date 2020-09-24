package com.java;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author JR Chan
 */
public class FileServerThreadPool {
    ServerSocket serverSocketT;
    ExecutorService executorService; // 线程池
    final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

    /**
     * 构造函数
     *
     * @throws IOException socket创建错误
     */
    public FileServerThreadPool() throws IOException {
        int tcpPort = 2021;
        //Tcp 端口
        serverSocketT = new ServerSocket(tcpPort, 2);
        serverSocketT.setSoTimeout(10 * 1000);
        // 创建线程池
        // Runtime的availableProcessors()方法返回当前系统可用处理器的数目
        // 由JVM根据系统的情况来决定线程的数量
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors() * POOL_SIZE);
        System.out.println("服务器启动。");
    }

    public static void main(String[] args) throws IOException {
        // receive a param
        if (args.length == 0) {
            System.out.println("usage: java com.java.FileServer <dir>");
        } else {
            FileServerThreadPool fs = new FileServerThreadPool();
            System.out.println("<当前路径：" + args[0] + ">");
            File checkCwd = new File(args[0]);
            if (checkCwd.isDirectory()) {
                fs.service(args[0]);   //启动多线程服务
            } else {
                System.out.println("该参数不是有效路径，服务器启动失败。");
            }
        }
    }

    /**
     * @param cwd 服务器当前路径
     */
    public void service(final String cwd) {
        Socket socket;
        //暂时只让服务器仅能连续超时100次
        int count = 0;
        while (count < 100) {
            try {
                String currentPath = cwd.trim();    //每次新连接都跳转到服务器指定目录
                socket = serverSocketT.accept();    //等待用户连接
                socket.setSoTimeout(30 * 1000);     //客户端长时间未响应
                count = 0;  //连续超时次数置零
                executorService.execute(new Handler(currentPath, socket)); // 把执行交给线程池来维护
            } catch (IOException e) {
                System.out.println("no connection yet");
                count += 1;
            }
        }
    }
}

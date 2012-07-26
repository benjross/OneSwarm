package edu.washington.cs.oneswarm.test.integration.oop;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;
import edu.washington.cs.oneswarm.test.util.LocalProcessesTestBase;

public class LogReportingExperimentTest extends LocalProcessesTestBase {
    public static Logger logger = Logger.getLogger(LogReportingExperimentTest.class.getName());

    @Test
    public void test() {
        ExperimentalHarnessManager manager = ExperimentalHarnessManager.get();
        manager.enqueue(new String[] {
                "inject edu.washington.cs.oneswarm.test.integration.oop.LogReportingExperiment",
                "sow_log " + LogReportingExperimentTest.class.getName()
        });
        logger.info("This message should be saved.");
        
        String url = "http://127.0.0.1:";
        final StringWriter output = new StringWriter();
        LogReceiver lr = null;
        try {
            lr = new LogReceiver(output);
            url += lr.serversocket.getLocalPort();
            lr.start();
        } catch (IOException e) {
            fail("Listener creation error");
        }
        manager.enqueue(new String[] {
           "reap_log " + url + " instancename"
        });

        try {
            lr.join(1000);
         } catch(InterruptedException e) {
            fail("Receiver timeout");
        }
        
        String log = output.toString();
        Assert.assertTrue(log.contains("This message should be saved."));
        Assert.assertTrue(log.contains("instancename"));
    }

    private class LogReceiver extends Thread {
        ServerSocket serversocket;
        StringWriter output;
        
        public LogReceiver(StringWriter output) throws IOException {
            serversocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            this.output = output;
            
            setName("LogReportingExperimentTest receiver");
        }
        
        public void run() {
             Socket socket = null;
               try {
                   serversocket.setSoTimeout(1000);
                    try {
                        socket = serversocket.accept();
                        socket.setSoTimeout(1000);
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        fail("socket timeout");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("Socket IO error.");
                }
             try {
                 try {
                     IOUtils.copy(socket.getInputStream(), output);
                 } catch(SocketTimeoutException e) {
                     logger.info("read " + output.toString().length() + "bytes");
                 }
                
                    PrintStream out = new PrintStream(socket.getOutputStream());
                    out.print("HTTP/1.1 200 OK\r\n\r\n");
                    out.print("ok\r\n");
                    socket.close();
            } catch (IOException e) {
                fail("Socket read exception");
            }
        }
    }
}

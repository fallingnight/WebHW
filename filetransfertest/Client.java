import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socke
       
        blic class Client {
          public static void ma
              String hostName = "12
             
                 Socket clientSocket = null;
                  try {
                     clientSocket = new Socket(hostName, portNumber);
                     InputStream ist = clientSocket.get
                             byte[] data = new byte[1024];
                     ist.read(data, 0, 1024);
                        System.o
                     
                     System.out.println(filen
                      File 
                     OutputStream ost = new FileO u tputStream(outFile)
                     int te
                  while ((temp 
                         ost.wri
         
               ist.close();
            ost.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    
    final String command = args[0];
    
    switch (command) {
      case "init" -> {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");
    
        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      case "cat-file" -> {
        if (args.length < 3) {
          System.out.println("Usage: cat-file <type> <object>");
          return;
        }

        if (!args[1].equals("-p")) {
          System.out.println("Unsupported option: " + args[1]);
          return;
        }

        final String objectSHA = args[2];
        final String dir = objectSHA.substring(0, 2);
        final String fileName = objectSHA.substring(2);
        final String blobObjectPath = ".git/objects/" + dir + "/" + fileName;
        final File blobObjectFile = new File(blobObjectPath);
        if (!blobObjectFile.exists()) {
          System.out.println("Object not found: " + objectSHA);
          return;
        }

        final Inflater inflater = new Inflater();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        try {
          inflater.setInput(Files.readAllBytes(blobObjectFile.toPath()));
          while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
          }
          inflater.end();

          byte[] outputBytes = outputStream.toByteArray();
          int nullByteIndex = 0;
          for (int i = 0; i < outputBytes.length; i++) {
            if (outputBytes[i] == 0) {
              nullByteIndex = i;
              break;
            }
          }

          String header = new String(outputBytes, 0, nullByteIndex);
          if (!header.startsWith("blob")) {
            System.out.println("Unsupported object type: " + header);
            return;
          }
          
          String content = new String(outputBytes, nullByteIndex + 1, outputBytes.length - nullByteIndex - 1);
          System.out.println(content);
        }
        catch (IOException e) {
          System.out.println("Error reading object: " + e.getMessage());
        } catch (Exception e) {
          System.out.println("Error decompressing object: " + e.getMessage());
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }
}

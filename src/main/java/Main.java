import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.Inflater;
import java.util.zip.Deflater;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class TreeEntry {
  String mode;
  String type;
  String sha;
  String name;

  TreeEntry(String mode, String type, String sha, String name) {
    this.mode = mode;
    this.type = type;
    this.sha = sha;
    this.name = name;
  }
}

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
          return;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      case "cat-file" -> {
        if (args.length < 3) {
          System.out.println("Usage: cat-file -p <object>");
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
          System.out.print(content);
          return;
        }
        catch (IOException e) {
          System.out.println("Error reading object: " + e.getMessage());
        } catch (Exception e) {
          System.out.println("Error decompressing object: " + e.getMessage());
        }
      }
      case "hash-object" -> {
        if (args.length < 3) {
          System.out.println("Usage: hash-object -w <file>");
          return;
        }

        if (!args[1].equals("-w")) {
          System.out.println("Unsupported option: " + args[1]);
          return;
        }

        final String filePath = args[2];
        final File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
          System.out.println("File not found: " + filePath);
          return;
        }

        try {
          byte[] content = Files.readAllBytes(file.toPath());

          String header = "blob " + content.length + "\0";
          byte[] headerBytes = header.getBytes("UTF-8");

          byte[] blob = new byte[headerBytes.length + content.length];
          System.arraycopy(headerBytes, 0, blob, 0, headerBytes.length);
          System.arraycopy(content, 0, blob, headerBytes.length, content.length);

          MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
          byte[] hash = sha1.digest(blob);
          StringBuilder hexHash = new StringBuilder();
          for (byte b : hash) {
            hexHash.append(String.format("%02x", b));
          }

          String hashStr = hexHash.toString();
          String dir = hashStr.substring(0, 2);
          String fileName = hashStr.substring(2);

          Deflater deflater = new Deflater();
          deflater.setInput(blob);
          deflater.finish();

          ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            compressedOutput.write(buffer, 0, count);
          }
          deflater.end();
          byte[] compressedBlob = compressedOutput.toByteArray();

          File objectDir = new File(".git/objects/" + dir);
          objectDir.mkdirs();

          File objectFile = new File(objectDir, fileName);
          if (!objectFile.exists()) {
            Files.write(objectFile.toPath(), compressedBlob);
          }

          System.out.print(hashStr);
          return;
        }
        catch (IOException e) {
          System.out.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
          System.out.println("Error hashing object: " + e.getMessage());
        }
      }
      case "ls-tree" -> {
        if (args.length < 2) {
          System.out.println("Usage: ls-tree [--name-only] <tree_sha>");
          return;
        }

        boolean nameOnly = false;
        final String sha;
        
        if (args[1].equals("--name-only")) {
          if (args.length < 3) {
            System.out.println("Usage: ls-tree --name-only <tree_sha>");
            return;
          }
          nameOnly = true;
          sha = args[2];
        } else {
          sha = args[1];
        }

        final String dir = sha.substring(0, 2);
        final String file = sha.substring(2);
        final File objectFile = new File(".git/objects/" + dir + "/" + file);
        if (!objectFile.exists()) {
          System.out.println("Object not found: " + sha);
          return;
        }

        try {
          Inflater inflater = new Inflater();
          byte[] compressedData = Files.readAllBytes(objectFile.toPath());
          inflater.setInput(compressedData);
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            out.write(buffer, 0, count);
          }
          inflater.end();
          byte[] decompressed = out.toByteArray();

          int nullIndex = 0;
          while (decompressed[nullIndex] != 0) nullIndex++;
          String header = new String(decompressed, 0, nullIndex);
          if (!header.startsWith("tree")) {
            System.out.println("Not a tree object");
            return;
          }

          ArrayList<TreeEntry> entries = new ArrayList<>();
          int i = nullIndex + 1;
          while (i < decompressed.length) {
            int modeStart = i;
            while (decompressed[i] != ' ') i++;
            String mode = new String(decompressed, modeStart, i - modeStart);

            int nameStart = ++i;
            while (decompressed[i] != 0) i++;
            String name = new String(decompressed, nameStart, i - nameStart);
            
            i++;
            byte[] shaBytes = new byte[20];
            System.arraycopy(decompressed, i, shaBytes, 0, 20);
            i += 20;

            StringBuilder hexSha = new StringBuilder();
            for (byte b : shaBytes) {
              hexSha.append(String.format("%02x", b));
            }

            if (nameOnly) {
              System.out.println(name);
            } else {
              String type = switch (mode) {
                case "40000" -> "tree";
                case "100644", "100755", "120000" -> "blob";
                default -> "unknown";
              };
              entries.add(new TreeEntry(mode, type, hexSha.toString(), name));
            }
          }

          if (!nameOnly) {
            Collections.sort(entries, Comparator.comparing(e -> e.name));
            for (TreeEntry entry : entries) {
              System.out.printf("%s %s %s\t%s\n", entry.mode, entry.type, entry.sha, entry.name);
            }
          }
          return;
        } catch (Exception e) {
          System.out.println("Error reading tree object: " + e.getMessage());
        }
      }
      case "write-tree" -> {
        if (args.length > 1) {
          System.out.println("Usage: write-tree");
          return;
        }

        try {
          File rootDir = new File(".");
          ArrayList<TreeEntry> entries = new ArrayList<>();

          File[] children = rootDir.listFiles();
          if (children != null) {
            for (File child : children) {
              if (child.getName().equals(".git")) {
                continue;
              }

              if (child.isFile()) {
                byte[] content = Files.readAllBytes(child.toPath());
                String header = "blob " + content.length + "\0";
                byte[] headerBytes = header.getBytes("UTF-8");
                byte[] blob = new byte[headerBytes.length + content.length];
                System.arraycopy(headerBytes, 0, blob, 0, headerBytes.length);
                System.arraycopy(content, 0, blob, headerBytes.length, content.length);

                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                byte[] hash = sha1.digest(blob);
                StringBuilder hexHash = new StringBuilder();
                for (byte b : hash) {
                  hexHash.append(String.format("%02x", b));
                }
                String hashStr = hexHash.toString();
                String dir = hashStr.substring(0, 2);
                String fileName = hashStr.substring(2);

                File objectDir = new File(".git/objects/" + dir);
                objectDir.mkdirs();
                File objectFile = new File(objectDir, fileName);
                if (!objectFile.exists()) {
                  Deflater deflater = new Deflater();
                  deflater.setInput(blob);
                  deflater.finish();
                  ByteArrayOutputStream out = new ByteArrayOutputStream();
                  byte[] buffer = new byte[1024];
                  while (!deflater.finished()) {
                    int count = deflater.deflate(buffer);
                    out.write(buffer, 0, count);
                  }
                  deflater.end();
                  Files.write(objectFile.toPath(), out.toByteArray());
                }

                entries.add(new TreeEntry("100644", "blob", hashStr, child.getName()));
              }
            }
          }

          Collections.sort(entries, Comparator.comparing(e -> e.name));

          ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
          for (TreeEntry entry : entries) {
            treeContent.write((entry.mode + " " + entry.name).getBytes("UTF-8"));
            treeContent.write(0);
            for (int i = 0; i < entry.sha.length(); i += 2) {
              treeContent.write(Integer.parseInt(entry.sha.substring(i, i + 2), 16));
            }
          }

          byte[] treeData = treeContent.toByteArray();
          String header = "tree " + treeData.length + "\0";
          byte[] headerBytes = header.getBytes("UTF-8");

          byte[] treeObject = new byte[headerBytes.length + treeData.length];
          System.arraycopy(headerBytes, 0, treeObject, 0, headerBytes.length);
          System.arraycopy(treeData, 0, treeObject, headerBytes.length, treeData.length);

          MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
          byte[] treeHash = sha1.digest(treeObject);
          StringBuilder hexTreeHash = new StringBuilder();
          for (byte b : treeHash) {
            hexTreeHash.append(String.format("%02x", b));
          }
          String treeHashStr = hexTreeHash.toString();

          String treeDir = treeHashStr.substring(0, 2);
          String treeFile = treeHashStr.substring(2);
          File treeObjectDir = new File(".git/objects/" + treeDir);
          treeObjectDir.mkdirs();
          File treeObjectFile = new File(treeObjectDir, treeFile);
          if (!treeObjectFile.exists()) {
            Deflater deflater = new Deflater();
            deflater.setInput(treeObject);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
              int count = deflater.deflate(buffer);
              out.write(buffer, 0, count);
            }
            deflater.end();
            Files.write(treeObjectFile.toPath(), out.toByteArray());
          }

          System.out.println(treeHashStr);
          return;
        } catch (Exception e) {
          System.out.println("Error writing tree: " + e.getMessage());
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }
}

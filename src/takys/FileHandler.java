package takys;

import takys.Util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.List;

public class FileHandler {
      public static String filePath;


      public static void SaveFallen() {
            try {
                  File tmpDir = new File(Setup.dataFolder + "/");
                  if (!Files.exists(tmpDir.toPath(), new LinkOption[0])) {
                        tmpDir.mkdir();
                  }

                  ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filePath));
                  output.writeObject(Utilities.DeadPlayers);
                  output.close();
            } catch (IOException var) {
                  var.printStackTrace();
            }

      }

      public static void LoadFallen() {
            try {
                  ObjectInputStream input = null;
                  File tempFile = new File(filePath);
                  if (tempFile.exists()) {
                        input = new ObjectInputStream(new FileInputStream(filePath));
                        Utilities.DeadPlayers.addAll((List)input.readObject());
                  }

                  if (input != null) {
                        input.close();
                  }
            } catch (ClassNotFoundException | IOException var) {
                  var.printStackTrace();
            }

      }

      static {
            filePath = Setup.dataFolder + "/Dead Players.data";
      }
}

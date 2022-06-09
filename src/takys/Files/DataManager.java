package takys.Files;

import takys.Objects.PlayerObj;
import takys.Setup;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class DataManager {

    private final static Path path = new File(Setup.instance.getDataFolder() + "/DeadPlayers.data").toPath();
    public static ArrayList<PlayerObj> LoadObjectsFromFile() {

        ArrayList<PlayerObj> tempList = new ArrayList<>();

        try {
            Object obj;
            FileInputStream file = new FileInputStream(String.valueOf(path));
            ObjectInputStream input = new ObjectInputStream(file);

            while ((obj = input.readObject()) != null) {
                PlayerObj pObj = (PlayerObj) obj;
                tempList.add(pObj);
            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        }
        return tempList;
    }

    public static void EraseFileContents() {
        try {
            PrintWriter pw = new PrintWriter((String.valueOf(path)));
            pw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void WriteObjectToFile(Object serObj) {
        if (!Files.exists(path)) {
            try {
                new File(String.valueOf(path)).createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            FileOutputStream fileOut = new FileOutputStream(String.valueOf(path));
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(serObj);
            objectOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
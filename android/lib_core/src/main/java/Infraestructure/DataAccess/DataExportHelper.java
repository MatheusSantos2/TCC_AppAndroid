package Infraestructure.DataAccess;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import Variables.Constants;

public class DataExportHelper {

    private final String folderPath = new Constants().FolderPath;
    private static Context context;
    private File folder;
    private File zipFile;

    public DataExportHelper(Context context){
        this.context = context;
        folder = new File(context.getExternalFilesDir(null),folderPath);
        folder.mkdirs();
    }

    public void export()
    {
        exportDatabase();
        exportFolderToZip();
        shareFile();
    }

    private void exportDatabase() {
        try
        {
            File dbFile = context.getDatabasePath("Monitoring.db");
            File exportFile = new File(folder, "Monitoring.db");
            copyFile(dbFile, exportFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private void exportFolderToZip() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String zipFileName = "export_" + timeStamp + ".zip";
        zipFile = new File(context.getExternalFilesDir(null), zipFileName);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addFilesToZip(folder, "", zipOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addFilesToZip(File file, String parentPath, ZipOutputStream zipOutputStream) throws IOException {
        if (file.isDirectory()) {
            String filePath = parentPath + file.getName() + "/";
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    addFilesToZip(subFile, filePath, zipOutputStream);
                }
            }
        }else {
            byte[] buffer = new byte[1024];
            FileInputStream fileInputStream = new FileInputStream(file);
            String filePath = parentPath + file.getName();
            zipOutputStream.putNextEntry(new ZipEntry(filePath));
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }
            zipOutputStream.closeEntry();
            fileInputStream.close();
        }
    }

    private void shareFile()
    {
        Uri fileUri = FileProvider.getUriForFile(context, "com.example.myapp.fileprovider", zipFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Compartilhar pasta exportada"));
    }
}

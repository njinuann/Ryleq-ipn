package com.starise.ipn.Util;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class LogFile {
    public InputStream getInputStreamFromBytes(byte[] bytes)
    {
        return new ByteArrayInputStream(bytes);
    }

    public InputStream getInputStreamFromFile(String filePath) throws Exception
    {
        return new FileInputStream(new File(filePath));
    }

    public void saveBytesToFile(byte[] myBytes, String directory, String fileName) throws Exception
    {
        createDirectory(directory);
        FileOutputStream fout = new FileOutputStream(directory + File.separator + fileName, false);
        fout.write(myBytes);
        try
        {
            fout.close();
        }
        catch (Exception ex)
        {
            ex = null;
        }
    }

    public byte[] getFileBytes(String fileUrl) throws Exception
    {
        File file = new File(fileUrl);
        byte[] fileBytes = new byte[(int) file.length()];
        FileInputStream fin = new FileInputStream(file);
        fin.read(fileBytes);
        try
        {
            fin.close();
        }
        catch (Exception ex)
        {
            ex = null;
        }
        return fileBytes;
    }

    public void write(String fileName, String contentToWrite) throws Exception
    {
        try
        {
            makeDirectory(new File(fileName).getParent());
        }
        catch (Exception ex)
        {
            ex = null;
        }
        write(new File(fileName), contentToWrite);
    }

    public void append(String fileName, String contentToWrite) throws Exception
    {
        try
        {
            makeDirectory(new File(fileName).getParent());
        }
        catch (Exception ex)
        {
            ex = null;
        }
        append(new File(fileName), contentToWrite);
    }

    public void write(File file, String contentToWrite) throws Exception
    {
        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")))
        {
            out.write(contentToWrite.trim() + "\r\n");
        }
    }

    public void append(File file, String contentToWrite) throws Exception
    {
        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8")))
        {
            out.append(contentToWrite.trim() + "\r\n");
        }
    }

    public boolean moveFile(File sourceFile, String destinationDirectory) throws Exception
    {
        try
        {
            makeDirectory(destinationDirectory);
        }
        catch (Exception ex)
        {
            ex = null;
        }
        copy(sourceFile, destinationDirectory);
        return deleteFile(sourceFile);
    }

    public File[] getFiles(String dir) throws Exception
    {
        File directory = new File(dir);
        if (directory.exists() && directory.isDirectory())
        {
            return directory.listFiles();
        }
        return new File[0];
    }

    public void copy(File srcFile, String dstDir) throws Exception
    {
        String fExt = (srcFile.getName().indexOf(".") > 0) ? srcFile.getName().substring(srcFile.getName().lastIndexOf(".")) : "";
        String tempFileName = "".equals(fExt) ? srcFile.getName() : srcFile.getName().replace(fExt, ".mds");
        saveBytesToFile(getFileBytes(srcFile.getAbsolutePath()), dstDir, tempFileName);
        if (!"".equals(fExt))
        {
            File tempFile = new File(dstDir, tempFileName);
            tempFile.renameTo(new File(tempFile.getAbsolutePath().replace(".mds", fExt)));
        }
    }

    public boolean deleteFile(String path) throws Exception
    {
        return deleteFile(new File(path));
    }

    public boolean deleteFile(File file) throws Exception
    {
        if (!file.exists())
        {
            return true;
        }
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (File f : files)
            {
                return deleteFile(f);
            }
        }
        Files.delete(file.toPath());
        return true;
    }

    public boolean makeDirectory(String strDirectoy) throws Exception
    {
        if (!new File(strDirectoy).exists())
        {
            return (new File(strDirectoy)).mkdirs();
        }
        return true;
    }

    public void compressFileToGzip(File f) throws Exception
    {
        InputStream is = null;
        GZIPOutputStream os = null;
        try
        {
            int read;
            File zipFile = new File(f.getAbsolutePath() + ".gz");
            os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            is = new BufferedInputStream(new FileInputStream(f));
            byte[] buff = new byte[1048576];
            while ((read = is.read(buff)) > 0)
            {
                os.write(buff, 0, read);
            }
            os.finish();
            os.close();
            is.close();
        }
        catch (Exception ex)
        {
            if (os != null)
            {
                os.finish();
                os.close();
            }
            if (is != null)
            {
                is.close();
            }
            throw ex;
        }
    }

    public String readTextFile(File fileToRead) throws Exception
    {
        BufferedReader bis = null;
        StringBuilder buffer = new StringBuilder();
        try
        {
            bis = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead)));
            String line = "";
            while (line != null)
            {
                buffer.append(line).append("\r\n");
                line = bis.readLine();
            }
            try
            {
                bis.close();
            }
            catch (IOException ie)
            {
            }

            return buffer.toString();
        }
        catch (Exception ex)
        {
            try
            {
                if (bis != null)
                {
                    bis.close();
                }
            }
            catch (Exception e)
            {
                e = null;
            }
            throw ex;
        }
    }

    public void addFileToZipArchive(String fileName, InputStream fileInstream, ZipOutputStream out) throws IOException
    {
        BufferedInputStream bufferedFileInstream = new BufferedInputStream(fileInstream, 1048576);
        byte data[] = new byte[1048576];

        out.putNextEntry(new ZipEntry(fileName));
        int count;

        while ((count = bufferedFileInstream.read(data, 0, 1048576)) != -1)
        {
            out.write(data, 0, count);
        }
        try
        {
            bufferedFileInstream.close();
        }
        catch (Exception ex)
        {
            ex = null;
        }
    }

    public String readInputStream(InputStream inputStream) throws IOException
    {
        String line;
        StringBuilder buffer = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = in.readLine()) != null)
        {
            buffer.append(line);
        }
        return buffer.toString();
    }

    public void extractZipArchive(File zipFile, String destDirectory) throws Exception
    {
        createDirectory(destDirectory);
        ZipFile archive = new ZipFile(zipFile);
        Enumeration zipEntries = archive.entries();

        while (zipEntries.hasMoreElements())
        {
            ZipEntry zipEntryFile = (ZipEntry) zipEntries.nextElement();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destDirectory + File.separator + zipEntryFile.getName()));

            InputStream inputStream = archive.getInputStream(zipEntryFile);
            byte[] buffer = new byte[1048576];

            int len;
            while ((len = inputStream.read(buffer)) >= 0)
            {
                outputStream.write(buffer, 0, len);
            }

            try
            {
                inputStream.close();
                outputStream.close();
            }
            catch (Exception ex)
            {
                ex = null;
            }
        }
        try
        {
            archive.close();
        }
        catch (Exception ex)
        {
            ex = null;
        }
    }

    public void createDirectory(String directoryPath)
    {
        File f = new File(directoryPath);
        if (!f.exists())
        {
            f.mkdirs();
        }
    }
}


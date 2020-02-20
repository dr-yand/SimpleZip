package com.kritsin.simplezip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class SimpleZip {

    private static final byte[] localFileHeaderSign = new byte[]{0x50, 0x4b, 0x03, 0x04};

    private static final byte[] version = new byte[]{0x14, 0x00};
    private static final byte[] purposeBitFlag = new byte[]{0x02, 0x00};
    private static final byte[] compressionMethod = new byte[]{0x00, 0x00};

    private Map<String, Integer> crcMap = new HashMap<>();

    private Map<String, Integer> prepareCrc(String... files) throws IOException {
        Map<String, Integer> result = new HashMap<>();

        for (String file : files) {
            result.put(file, CrcUtil.getCrc(file));
        }

        return result;
    }

    public void archive(String destFile, String... files) throws IOException {

        crcMap = prepareCrc(files);

        FileOutputStream outputFile = new FileOutputStream(destFile);

        int[] offsets = new int[files.length];
        int offset = 0;

        byte[] b = new byte[65536];
        for (int i = 0; i < files.length; i++) {
            String s = files[i];
            byte[] fileData = writeLocalHeader(s);
            long fileSize = new File(s).length();
            offsets[i] = offset;
            offset += fileData.length + fileSize;
            outputFile.write(fileData);

            FileInputStream inputStream = new FileInputStream(s);
            int c;
            while ((c = inputStream.read(b)) != -1) {
                outputFile.write(b, 0, c);
            }

            inputStream.close();
        }
        //
        int centralDirSize = 0;
        int centralDirOffset = offset;
        for (int i = 0; i < files.length; i++) {
            String s = files[i];
            byte[] centralData = writeCentralHeader(s, offsets[i]);
            centralDirSize += centralData.length;

            outputFile.write(centralData);
        }
        //
        byte[] endData = writeEndHeader(centralDirSize, centralDirOffset, files.length);
        outputFile.write(endData);
        //

        outputFile.close();
    }

    private byte[] writeLocalHeader(String fullFileName) throws IOException {
        File file = new File(fullFileName);
        String fn = file.getName();

        byte[] date = Utils.toByteArray(Utils.getDate(file.lastModified()), 2);
        byte[] time = Utils.toByteArray(Utils.getTime(file.lastModified()), 2);
        byte[] crc = Utils.toByteArray(crcMap.get(fullFileName), 4);
        byte[] compressedSize = Utils.toByteArray(file.length(), 4);
        byte[] fileSize = Utils.toByteArray(file.length(), 4);

        byte[] filenameLength = Utils.toLittleEndianByteArray(fn.length(), 2);
        byte[] filename = fn.getBytes();

        byte[] extraLength = new byte[]{0x00, 0x00};
        byte[] extraField = new byte[]{};

//        byte[] data = Utils.readFile(fullFileName);

        byte[] result = Utils.makeArray(localFileHeaderSign, version, purposeBitFlag,
                compressionMethod, time, date, crc, compressedSize, fileSize,
                filenameLength, extraLength, filename, extraField);

        Utils.arraysCopy(result, localFileHeaderSign, version, purposeBitFlag,
                compressionMethod, time, date, crc, compressedSize, fileSize,
                filenameLength, extraLength, filename, extraField);

        return result;
    }

    private byte[] writeCentralHeader(String fullFileName, int offset) throws IOException {
        final byte[] centralFileHeaderSign = new byte[]{0x50, 0x4b, 0x01, 0x02};

        File file = new File(fullFileName);
        String fn = file.getName();

        byte[] date = Utils.toByteArray(Utils.getDate(file.lastModified()), 2);
        byte[] time = Utils.toByteArray(Utils.getTime(file.lastModified()), 2);
        byte[] crc = Utils.toByteArray(crcMap.get(fullFileName), 4);
        byte[] compressedSize = Utils.toByteArray(file.length(), 4);
        byte[] fileSize = Utils.toByteArray(file.length(), 4);

        byte[] filenameLength = Utils.toLittleEndianByteArray(fn.length(), 2);
        byte[] filename = fn.getBytes();

        byte[] extraLength = new byte[]{0x00, 0x00};
        byte[] extraField = new byte[]{};

        byte[] commentLength = new byte[]{0x00, 0x00};
        byte[] commentField = new byte[]{};

        byte[] diskNumber = new byte[]{0x00, 0x00};

        int internalFileAttr = 0;

        byte[] internalFileAttrs = Utils.toByteArray(internalFileAttr, 2);
        byte[] externalFileAttrs = new byte[]{0x00, 0x00, 0x00, 0x00};
        byte[] relativeOffsetAttrs = Utils.toByteArray(offset, 4);

        byte[] result = Utils.makeArray(centralFileHeaderSign, version, version,
                purposeBitFlag,
                compressionMethod, time, date, crc,
                compressedSize, fileSize,
                filenameLength, extraLength,
                commentLength,
                diskNumber, internalFileAttrs, externalFileAttrs, relativeOffsetAttrs,
                filename, extraField,
                commentField);

        Utils.arraysCopy(result, centralFileHeaderSign, version, version, purposeBitFlag, compressionMethod,
                time, date, crc, compressedSize, fileSize, filenameLength, extraLength, commentLength,
                diskNumber, internalFileAttrs, externalFileAttrs, relativeOffsetAttrs, filename, extraField, commentField);

        return result;
    }

    private byte[] writeEndHeader(int centralDir, int offsetCentral, int count) throws IOException {
        final byte[] endFileHeaderSign = new byte[]{0x50, 0x4b, 0x05, 0x06};

        byte[] numberOnDisk = new byte[]{0x00, 0x00};
        byte[] diskCentralDir = new byte[]{0x00, 0x00};
        byte[] numberCentralDisk = Utils.toByteArray(count, 2);
        byte[] totalNumberDir = Utils.toByteArray(count, 2);
        byte[] sizeCentralDir = Utils.toByteArray(centralDir, 4);
        byte[] offsetCentralDir = Utils.toByteArray(offsetCentral, 4);

        byte[] commentLength = new byte[]{0x00, 0x00};
        byte[] commentField = new byte[]{};

        byte[] result = Utils.makeArray(endFileHeaderSign, numberOnDisk, diskCentralDir, numberCentralDisk,
                totalNumberDir, sizeCentralDir, offsetCentralDir, commentLength, commentField);

        Utils.arraysCopy(result, endFileHeaderSign, numberOnDisk, diskCentralDir, numberCentralDisk, totalNumberDir,
                sizeCentralDir, offsetCentralDir, commentLength, commentField);

        return result;
    }
}

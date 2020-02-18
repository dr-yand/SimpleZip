package com.kritsin.simplezip;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Calendar;

public class Main {

    static final byte[] extendedFileHeaderSign = new byte[]{0x50, 0x4b, 0x07, 0x08};

    static final byte[] version = new byte[]{0x14, 0x00};
    static final byte[] purposeBitFlag = new byte[]{0x02, 0x00};
    static final byte[] compressionMethod = new byte[]{0x00, 0x00};

    public static void main(String[] args) {
        arc("d:\\logo-big.png");
    }

    static boolean isBinaryFile(File f) throws IOException {
        String type = Files.probeContentType(f.toPath());
        if (type == null || f.length() == 0) {
            return true;
        } else if (type.startsWith("text")) {
            return false;
        } else {
            return true;
        }
    }

    static long getDate(long modified) {
        long result = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(modified);

        long year = calendar.get(Calendar.YEAR);
        long month = calendar.get(Calendar.MONTH) + 1;
        long day = calendar.get(Calendar.DAY_OF_MONTH);

        result = (year - 1980) << 9;
        result = result | month << 5;
        result = result | day;

        return result;
    }

    static long getTime(long modified) {
        long result = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(modified);

        long hour = calendar.get(Calendar.HOUR_OF_DAY);
        long minute = calendar.get(Calendar.MINUTE);
        long second = calendar.get(Calendar.SECOND);

        result = hour << 11;
        result = result | minute << 5;
        result = result | (second / 2);

        return result;
    }

    static void arc(String... files) {
        String outputPath = "d:\\arc.zip";
        RandomAccessFile outputFile = null;

        try {
            outputFile = new RandomAccessFile(outputPath, "rw");

            int[] offsets = new int[files.length];
            int offset = 0;
            int i = 0;
            byte[] data = new byte[]{};
            for (String s : files) {
                byte[] fileData = writeLocalHeader(s);
                offsets[i] = offset;
                offset += fileData.length;
                i++;

                byte[] tempData = new byte[data.length + fileData.length];
                System.arraycopy(data, 0, tempData, 0, data.length);
                System.arraycopy(fileData, 0, tempData, data.length, fileData.length);
                data = tempData;
            }
            //
            i = 0;
            int centralDirSize = 0;
            int centralDirOffset = data.length;
            for (String s : files) {
                byte[] centralData = writeCentralHeader(s, offsets[i]);
                centralDirSize += centralData.length;
                i++;
                byte[] tempData = new byte[data.length + centralData.length];
                System.arraycopy(data, 0, tempData, 0, data.length);
                System.arraycopy(centralData, 0, tempData, data.length, centralData.length);
                data = tempData;
            }
            //
            byte[] endData = writeEndHeader(centralDirSize, centralDirOffset, files.length);
            byte[] tempData = new byte[data.length + endData.length];
            System.arraycopy(data, 0, tempData, 0, data.length);
            System.arraycopy(endData, 0, tempData, data.length, endData.length);
            data = tempData;
            //

            outputFile.seek(0);
            outputFile.write(data);
            outputFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static byte[] writeLocalHeader(String fullFileName) throws IOException {
        File file = new File(fullFileName);
        String fn = file.getName();

        final byte[] localFileHeaderSign = new byte[]{0x50, 0x4b, 0x03, 0x04};

        byte[] date = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(getDate(file.lastModified()))).toByteArray(), 2);
        byte[] time = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(getTime(file.lastModified()))).toByteArray(), 2);
        byte[] crc = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(CrcUtil.getCrc(fullFileName))).toByteArray(), 4);
        byte[] compressedSize = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(file.length())).toByteArray(), 4);
        byte[] fileSize = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(file.length())).toByteArray(), 4);

        byte[] filenameLength = Arrays.copyOf(BigInteger.valueOf(fn.length()).toByteArray(), 2);
//        byte[] filenameLength = intToBytes(file.length());
        byte[] filename = fn.getBytes();

        byte[] extraLength = new byte[]{0x00, 0x00};
        byte[] extraField = new byte[]{};

        byte[] data = CrcUtil.getFileData(fullFileName);

        byte[] result = new byte[localFileHeaderSign.length + version.length + purposeBitFlag.length
                + compressionMethod.length + time.length + date.length + crc.length
                + compressedSize.length + fileSize.length
                + filenameLength.length + extraLength.length
                + filename.length + extraField.length
                + data.length
                ];

        System.arraycopy(localFileHeaderSign, 0, result, 0, localFileHeaderSign.length);
        System.arraycopy(version, 0, result, 4, version.length);
        System.arraycopy(purposeBitFlag, 0, result, 6, purposeBitFlag.length);
        System.arraycopy(compressionMethod, 0, result, 8, compressionMethod.length);
        System.arraycopy(time, 0, result, 10, time.length);
        System.arraycopy(date, 0, result, 12, date.length);
        System.arraycopy(crc, 0, result, 14, crc.length);
        System.arraycopy(compressedSize, 0, result, 18, compressedSize.length);
        System.arraycopy(fileSize, 0, result, 22, fileSize.length);
        System.arraycopy(filenameLength, 0, result, 26, filenameLength.length);
        System.arraycopy(extraLength, 0, result, 28, extraLength.length);
        System.arraycopy(filename, 0, result, 30, filename.length);
        System.arraycopy(extraField, 0, result, 30 + filename.length, extraField.length);
        System.arraycopy(data, 0, result, 30 + filename.length + extraField.length, data.length);

        return result;
    }

    static byte[] writeCentralHeader(String fullFileName, int offset) throws IOException {
        final byte[] centralFileHeaderSign = new byte[]{0x50, 0x4b, 0x01, 0x02};

        File file = new File(fullFileName);
        String fn = file.getName();

        byte[] date = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(getDate(file.lastModified()))).toByteArray(), 2);
        byte[] time = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(getTime(file.lastModified()))).toByteArray(), 2);
        byte[] crc = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(CrcUtil.getCrc(fullFileName))).toByteArray(), 4);
        byte[] compressedSize = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(file.length())).toByteArray(), 4);
        byte[] fileSize = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(file.length())).toByteArray(), 4);

        byte[] filenameLength = Arrays.copyOf(BigInteger.valueOf(fn.length()).toByteArray(), 2);
        byte[] filename = fn.getBytes();

        byte[] extraLength = new byte[]{0x00, 0x00};
        byte[] extraField = new byte[]{};

        byte[] commentLength = new byte[]{0x00, 0x00};
        byte[] commentField = new byte[]{};

        byte[] diskNumber = new byte[]{0x00, 0x00};

        int internalFileAttr = 0;
        if (!isBinaryFile(file)) {
            internalFileAttr = 1;
        }

        byte[] internalFileAttrs = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(internalFileAttr)).toByteArray(), 2);
        byte[] externalFileAttrs = new byte[]{0x00, 0x00, 0x00, 0x00};
        byte[] relativeOffsetAttrs = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(offset)).toByteArray(), 4);

        byte[] result = new byte[centralFileHeaderSign.length + version.length + version.length
                + purposeBitFlag.length
                + compressionMethod.length + time.length + date.length + crc.length
                + compressedSize.length + fileSize.length
                + filenameLength.length + extraLength.length
                + commentLength.length
                + diskNumber.length + internalFileAttrs.length + externalFileAttrs.length + relativeOffsetAttrs.length
                + filename.length + extraField.length
                + commentField.length];

        System.arraycopy(centralFileHeaderSign, 0, result, 0, centralFileHeaderSign.length);
        System.arraycopy(version, 0, result, 4, version.length);
        System.arraycopy(version, 0, result, 6, version.length);
        System.arraycopy(purposeBitFlag, 0, result, 8, purposeBitFlag.length);
        System.arraycopy(compressionMethod, 0, result, 10, compressionMethod.length);
        System.arraycopy(time, 0, result, 12, time.length);
        System.arraycopy(date, 0, result, 14, date.length);
        System.arraycopy(crc, 0, result, 16, crc.length);
        System.arraycopy(compressedSize, 0, result, 20, compressedSize.length);
        System.arraycopy(fileSize, 0, result, 24, fileSize.length);
        System.arraycopy(filenameLength, 0, result, 28, filenameLength.length);
        System.arraycopy(extraLength, 0, result, 30, extraLength.length);
        System.arraycopy(commentLength, 0, result, 32, commentLength.length);
        System.arraycopy(diskNumber, 0, result, 34, diskNumber.length);
        System.arraycopy(internalFileAttrs, 0, result, 36, internalFileAttrs.length);
        System.arraycopy(externalFileAttrs, 0, result, 38, externalFileAttrs.length);
        System.arraycopy(relativeOffsetAttrs, 0, result, 42, relativeOffsetAttrs.length);
        System.arraycopy(filename, 0, result, 46, filename.length);
        System.arraycopy(extraField, 0, result, 46 + filename.length, extraField.length);
        System.arraycopy(commentField, 0, result, 46 + filename.length + extraField.length, commentField.length);


        return result;
    }

    static byte[] writeEndHeader(int centralDir, int offsetCentral, int count) throws IOException {
        final byte[] endFileHeaderSign = new byte[]{0x50, 0x4b, 0x05, 0x06};

        byte[] numberOnDisk = new byte[]{0x00, 0x00};
        byte[] diskCentralDir = new byte[]{0x00, 0x00};
        byte[] numberCentralDisk = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(count)).toByteArray(), 2);
        byte[] totalNumberDir = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(count)).toByteArray(), 2);
        byte[] sizeCentralDir = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(centralDir)).toByteArray(), 4);
        byte[] offsetCentralDir = Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(offsetCentral)).toByteArray(), 4);

        byte[] commentLength = new byte[]{0x00, 0x00};
        byte[] commentField = new byte[]{};

        byte[] result = new byte[endFileHeaderSign.length + numberOnDisk.length + diskCentralDir.length
                + numberCentralDisk.length
                + totalNumberDir.length + sizeCentralDir.length
                + offsetCentralDir.length
                + commentLength.length + commentField.length];

        System.arraycopy(endFileHeaderSign, 0, result, 0, endFileHeaderSign.length);
        System.arraycopy(numberOnDisk, 0, result, 4, numberOnDisk.length);
        System.arraycopy(diskCentralDir, 0, result, 6, diskCentralDir.length);
        System.arraycopy(numberCentralDisk, 0, result, 8, numberCentralDisk.length);
        System.arraycopy(totalNumberDir, 0, result, 10, totalNumberDir.length);
        System.arraycopy(sizeCentralDir, 0, result, 12, sizeCentralDir.length);
        System.arraycopy(offsetCentralDir, 0, result, 16, offsetCentralDir.length);
        System.arraycopy(commentLength, 0, result, 20, commentLength.length);
        System.arraycopy(commentField, 0, result, 22, commentField.length);


        return result;
    }

}

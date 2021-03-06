package com.shrinkify;

import com.shrinkify.util.ArrayUtil;
import com.shrinkify.util.FileUtil;
import javafx.scene.control.ProgressBar;
import javafx.util.Pair;

import java.io.*;
import java.util.*;

//Lemepel Ziv Compression
public class LZ77CompressionHandler implements Runnable{

    public static String loadpath, savepath;
    int wordLength = 8;
    //String filename = "Alice.txt";
    String fileEnding = ".txt";
    public enum ProcessType{
        Encode,
        Decode
    }
    public static ProcessType type;

    public void run() {
        try {
            RunEncode();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*switch (type){
            case Encode:
                RunEncode();
                break;
            case Decode:
                RunDecode();
                break;
        }*/
    }

    public void RunEncode() throws InterruptedException {
        StringBuilder str = FileUtil.LoadFile( "C:\\Users\\timid\\OneDrive\\Desktop\\PERSONAL\\JAVA\\test"+fileEnding);
        //String str = FileUtil.LoadFileBinary(loadpath);
        Encode(str);
        Decode();

    }
    public void RunDecode(){
    }

    private void Encode(StringBuilder data){
        //System.out.println(data);
        Map<String,Integer> encoding = new HashMap<>();
        List<Pair<Integer,String>> compressedBinaryData = new ArrayList<>();
        System.out.println("start: "+data.length());
        StringBuilder  sequence = new StringBuilder();
        //int wordLength = 8;
        for (int i = 0; i < data.length(); i+=wordLength) {
            float progress = ((float)i/(float)data.length())*100f;
            GUI.barProgress = progress;
            StringBuilder c = new StringBuilder();
            StringBuilder binary = new StringBuilder();
            if (i+wordLength <= data.length()){
                for (int j = 0; j < wordLength; j++) {
                    binary.append(data.charAt(i+j));
                }
                c.append(binary);
                //System.out.println(i+" | "+binary);
            }else{
                for (int j = 0; j < data.length()-i; j++) {
                    binary.append(data.charAt(i+j));
                }
                //Add extra bits
                StringBuilder extraBits = new StringBuilder();
                for (int n = binary.length(); n < wordLength; n++) {
                    extraBits.append('0');
                }
                //System.out.println(i+" | "+binary+extraBits);
                c.append(binary.append(extraBits));
            }
            sequence.append(c);
            //System.out.println(c);
            //System.out.println(i + "/" + data.length());
            if (encoding.get(sequence.toString())==null){
                //System.out.println(sequence+ " not in dict > ");
                encoding.put(sequence.toString() ,encoding.size());
                //System.out.println(i);
                if (sequence.length() > wordLength) {
                    //remove last letter and get number corresponding to resulting string
                    int compressedString = encoding.get(sequence.substring(0, sequence.length()- wordLength));
                    //Add the number and the last letter to the sequence to compressed data
                    String substring = sequence.substring(sequence.length()- wordLength);
                    compressedBinaryData.add(new Pair<>(compressedString, substring));
                    //System.out.println(g+" "+i+" adding");
                   //System.out.println(compressedString+" -|- "+ substring);
                }else {
                    compressedBinaryData.add(new Pair<>(-1, sequence.toString()));//-1 represents first character
                   // System.out.println(g+" "+i+" adding");
                  //System.out.println(-1+" | "+ sequence);

                }
                sequence = new StringBuilder();
            }
        }
        //Add the final bit
        if (encoding.get(sequence.toString())!=null) {
            compressedBinaryData.add(new Pair<>(-1, sequence.toString()));
        }
        //Write data to file
        File file = new File("C:\\Users\\timid\\OneDrive\\Desktop\\PERSONAL\\JAVA\\test.box");
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file)

        ) {
            for (Pair<Integer,String> pair :compressedBinaryData) {
                //Write header for first time (containing overflow number)
                if (pair.getKey() == -1){
                    fileOutputStream.write(1);
                }else {
                    fileOutputStream.write(0);
                }
                //Write header for index (containing overflow number)
                fileOutputStream.write(pair.getKey()/256);
                //Write index
                fileOutputStream.write(pair.getKey());
                //Write header for data (containing overflow number)
                int byteData = Integer.parseInt(pair.getValue(),2);
                //fileOutputStream.write(byteData/256);
                //Write data
                fileOutputStream.write(byteData);
                //System.out.println("WROTE >" +pair.getKey()/256+"  "+pair.getKey()+"  "+ Integer.parseInt(pair.getValue(),2));
                fileOutputStream.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Decode(){
        //String str = FileUtil.LoadFileBinary( "C:\\Users\\timid\\OneDrive\\Desktop\\PERSONAL\\JAVA\\test.box");
        //System.out.println("//////////////////////////////////");
        File file = new File("C:\\Users\\timid\\OneDrive\\Desktop\\PERSONAL\\JAVA\\test.box");
        //int wordLength = 8;
        Map<Integer,String> compressedBinaryData = new HashMap<>();
        StringBuilder uncompressedBinary = new StringBuilder();
        try (
                FileInputStream fileInputStream = new FileInputStream(file)
                //DataInputStream dataInputStream = new DataInputStream(fileInputStream);

        ) {

            while (fileInputStream.available() > 0){
                int special = fileInputStream.read();
                int i_overflowCount = fileInputStream.read();//header
                int i = fileInputStream.read();
                //int data_overflowCount = fileInputStream.read();
                int data =  fileInputStream.read();

                //Restore index and data using overflow nums
                i += (256*i_overflowCount);
                //data += (256*data_overflowCount);

                StringBuilder binaryString = new StringBuilder(Integer.toBinaryString(data));//= Long.toBinaryString(data);
                StringBuilder extraBits = new StringBuilder();
                for (int n = binaryString.length(); n < wordLength; n++) {
                    extraBits.append('0');
                }
                binaryString.insert(0,extraBits);

                //System.out.println(binaryString);
                //System.out.println("READ: "+overflowCount+" | "+i +" | "+data+" | "+binaryString);
                //System.out.println("READ: "+i +" | "+data);
                if (special == 1){
                    uncompressedBinary.append(binaryString);
                    compressedBinaryData.put(compressedBinaryData.size(), binaryString.toString());
                }else{
                    uncompressedBinary.append(binaryString.insert(0, compressedBinaryData.get(i)));
                    compressedBinaryData.put(compressedBinaryData.size(), binaryString.insert(0,compressedBinaryData.get(i)).toString());
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("end: "+uncompressedBinary.length());
        //Write data to file
        File newfile = new File("C:\\Users\\timid\\OneDrive\\Desktop\\PERSONAL\\JAVA\\test_unboxed"+fileEnding);
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(newfile)

        ) {
            for (int i = 0; i < uncompressedBinary.length(); i+=8) {
                StringBuilder binary = new StringBuilder();
                //System.out.println(uncompressedBinary);
                //System.out.println(i+"/"+uncompressedBinary.length());
                for (int j = i; j < i+8; j++) {
                    binary.append(uncompressedBinary.charAt(j));
                }
                //System.out.println("WROTE:" + binary);
                fileOutputStream.write(Integer.parseInt(binary.toString(),2));
            }
            fileOutputStream.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("done");

    }

}

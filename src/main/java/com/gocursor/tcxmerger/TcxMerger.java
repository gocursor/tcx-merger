package com.gocursor.tcxmerger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TcxMerger {
  private Document mainDoc;
  private Node activities;
  private int count;

  private void merge(Path path) throws Exception {
    if (Files.isDirectory(path)) {
      for (Path each : Files.newDirectoryStream(path)) {
        merge(each);
      }
    } else {
      if (path.toString().endsWith(".tcx")) {
        count++;
        System.out.println("" + count + ". reading:" + path);
        byte[] bytes = Files.readAllBytes(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbFactory.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(bytes));
        if (mainDoc == null) {
          // First XML file will be the main document
          mainDoc = doc;
          NodeList activitiesList = mainDoc.getElementsByTagName("Activities");
          if (activitiesList.getLength() == 0) {
            System.out.println("Can not find element Activities!");
            return;
          }
          activities = (Element) activitiesList.item(0);
        } else {
          // Merge every activity into the main document
          NodeList activityList = doc.getElementsByTagName("Activity");
          for (int i = 0; i < activityList.getLength(); i++) {
            Node activity = activityList.item(i);
            Node activityClone = activity.cloneNode(true);
            // Transfer ownership of cloned node to the destination document
            mainDoc.adoptNode(activityClone);
            activities.appendChild(activityClone);
          }
        }
      }
    }
  }

  public void merge(Path inputFolder, Path destinationFile) throws Exception {
    merge(inputFolder);
    if (mainDoc != null) {
      System.out.println("Saving: " + destinationFile);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Result output = new StreamResult(baos);
      Source input = new DOMSource(mainDoc);
      transformer.transform(input, output);
      Files.write(destinationFile, baos.toByteArray());
    }
  }

  public static void main(String[] arguments) throws Exception {
    if (arguments.length != 2) {
      System.out.println("Usage: " + TcxMerger.class.getName() + " InputFolderPath DestinationFilePath");
      return;
    }
    String inputFolderPath = arguments[0];
    String destinationFilePath = arguments[1];

    new TcxMerger().merge(Paths.get(inputFolderPath), Paths.get(destinationFilePath));
  }
}
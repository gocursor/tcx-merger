package com.gocursor.tcxmerger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class TcxMergetTest {
  public byte[] getSample(int trackNr) throws IOException {
    StringWriter sw = new StringWriter();
    PrintWriter writer = new PrintWriter(sw);
    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.print("<TrainingCenterDatabase");
    writer.print(" xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"");
    writer.print(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    writer.print(" xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
        + " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\"");
    writer.println(">");
    writer.println("  <Activities>");
    Instant startTime = Instant.parse("2017-07-01T08:00:00.085Z").plusSeconds(24 * 60 * 60 * trackNr); // Plus 1 day
    double startLatitude = 46.557371; // from -90 (south pole) to +90 (north pole)
    double startLongitude = 15.646045 - 0.01 * trackNr; // from -180 (far west) to +180 (far east)
    for (int activity = 0; activity < 1; activity++) {
      writer.println("    <Activity Sport=\"Other\">");
      writer.println("      <Id>" + startTime.toString() + "</Id>");
      writer.println("      <Lap StartTime=\"" + startTime.toString() + "\">");
      writer.println("        <TotalTimeSeconds>3600.0</TotalTimeSeconds>");
      writer.println("        <DistanceMeters>3600.0</DistanceMeters>");
      writer.println("        <Calories>500</Calories>");
      writer.println("        <Intensity>Active</Intensity>");
      writer.println("        <TriggerMethod>Manual</TriggerMethod>");
      writer.println("        <Track>");
      for (int trackPoint = 0; trackPoint <= 60; trackPoint++) {
        Instant time = startTime.plusSeconds(60L * trackPoint);
        double latitude = startLatitude + 0.001 * trackPoint;
        double longitude = startLongitude + 0.001 * trackPoint;
        double distance = 60.0 * trackPoint;
        writer.println("          <Trackpoint>");
        writer.println("            <Time>" + time.toString() + "</Time>");
        writer.println("            <Position>");
        writer.println("              <LatitudeDegrees>" + latitude + "</LatitudeDegrees>");
        writer.println("              <LongitudeDegrees>" + longitude + "</LongitudeDegrees>");
        writer.println("            </Position>");
        writer.println("            <DistanceMeters>" + distance + "</DistanceMeters>");
        writer.println("          </Trackpoint>");
      }    
      writer.println("        </Track>");
      writer.println("      </Lap>");
      writer.println("    </Activity>");
    }
    writer.println("  </Activities>");
    writer.println("</TrainingCenterDatabase>");
    writer.flush();
    return sw.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Test
  public void basicTest() throws Exception {
    FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    Path inputFolder = fs.getPath("/tcx");
    Files.createDirectory(inputFolder);

    for (int trackNr = 0; trackNr < 10; trackNr++) {
      Path file = inputFolder.resolve("track" + (trackNr + 1) + ".tcx");
      Files.write(file, getSample(trackNr));
    }

    Path destinationFile = fs.getPath("all.tcx");
    new TcxMerger().merge(inputFolder, destinationFile);

    // Check the result
    byte[] resultBytes = Files.readAllBytes(destinationFile);
    assertNotNull(resultBytes);

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbFactory.newDocumentBuilder();
    Document doc = db.parse(new ByteArrayInputStream(resultBytes));
    assertNotNull(doc);
    assertNotNull(doc.getDocumentElement());
    assertEquals("TrainingCenterDatabase", doc.getDocumentElement().getTagName());
    assertEquals(10, doc.getElementsByTagName("Activity").getLength());
  }
}

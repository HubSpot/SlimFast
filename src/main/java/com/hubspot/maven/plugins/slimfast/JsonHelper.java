package com.hubspot.maven.plugins.slimfast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonHelper {

  public static void writeArtifactsToJson(File outputFile, Collection<S3Artifact> artifacts) throws IOException {
    JSONArray json = new JSONArray();
    for (S3Artifact artifact : artifacts) {
      json.add(toJsonObject(artifact));
    }

    try (Writer writer = newWriter(outputFile)) {
      json.writeJSONString(writer);
      writer.flush();
    }
  }

  public static Collection<S3Artifact> readArtifactsFromJson(File inputFile) throws IOException {
    JSONParser parser = new JSONParser();

    try (Reader reader = newReader(inputFile)) {
      try {
        JSONArray parsed = (JSONArray) parser.parse(reader);

        List<S3Artifact> artifacts = new ArrayList<>();
        for (Object object : parsed) {
          artifacts.add(fromJsonObject((JSONObject) object));
        }

        return artifacts;
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
  }

  private static Writer newWriter(File file) throws IOException {
    FileOutputStream outputStream = new FileOutputStream(file);
    return new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
  }

  private static Reader newReader(File file) throws IOException {
    FileInputStream outputStream = new FileInputStream(file);
    return new BufferedReader(new InputStreamReader(outputStream, StandardCharsets.UTF_8));
  }

  private static JSONObject toJsonObject(S3Artifact artifact) {
    JSONObject json = new JSONObject();
    json.put("s3Bucket", artifact.getBucket());
    json.put("s3ObjectKey", artifact.getKey());
    json.put("targetPath", artifact.getTargetPath());
    json.put("md5", artifact.getMd5());
    json.put("filesize", artifact.getSize());

    return json;
  }

  private static S3Artifact fromJsonObject(JSONObject json) {
    String bucket = (String) json.get("s3Bucket");
    String key = (String) json.get("s3ObjectKey");
    String targetPath = (String) json.get("targetPath");
    String md5 = (String) json.get("md5");
    long size = ((Number) json.get("filesize")).longValue();

    return new S3Artifact(bucket, key, targetPath, md5, size);
  }
}

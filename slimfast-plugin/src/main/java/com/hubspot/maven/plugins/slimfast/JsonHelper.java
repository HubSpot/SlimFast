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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonHelper {

  private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);

  public static void writeArtifactsToJson(File outputFile, S3ArtifactWrapper wrapper)
    throws IOException {
    JSONObject json = new JSONObject();

    JSONArray artifacts = new JSONArray();
    for (S3Artifact artifact : wrapper.getArtifacts()) {
      artifacts.add(toJsonObject(artifact));
    }

    json.put("prefix", wrapper.getPrefix());
    json.put("artifacts", artifacts);

    try (Writer writer = newWriter(outputFile)) {
      json.writeJSONString(writer);
      writer.flush();
    }
  }

  public static void writeArtifactsToJson(
    File outputFile,
    PreparedArtifactWrapper wrapper
  ) throws IOException {
    JSONObject json = new JSONObject();

    JSONArray artifacts = new JSONArray();
    for (PreparedArtifact artifact : wrapper.getArtifacts()) {
      artifacts.add(toJsonObject(artifact));
    }

    json.put("prefix", wrapper.getPrefix().toString());
    json.put("artifacts", artifacts);

    try (Writer writer = newWriter(outputFile)) {
      json.writeJSONString(writer);
      writer.flush();
    }

    StringWriter writer = new StringWriter();
    json.writeJSONString(writer);

    LOG.info("Wrote artifacts json: {}", writer.toString());
  }

  public static S3ArtifactWrapper readArtifactsFromJson(File inputFile)
    throws IOException {
    JSONParser parser = new JSONParser();

    try (Reader reader = newReader(inputFile)) {
      try {
        JSONObject parsed = (JSONObject) parser.parse(reader);

        String prefix = (String) parsed.get("prefix");
        Set<S3Artifact> artifacts = new LinkedHashSet<>();
        for (Object object : (JSONArray) parsed.get("artifacts")) {
          artifacts.add(fromJsonObject((JSONObject) object));
        }

        return new S3ArtifactWrapper(prefix, artifacts);
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
  }

  public static PreparedArtifactWrapper readPreparedArtifactsFromJson(File inputFile)
    throws IOException {
    JSONParser parser = new JSONParser();

    try (Reader reader = newReader(inputFile)) {
      try {
        JSONObject parsed = (JSONObject) parser.parse(reader);

        String prefix = (String) parsed.get("prefix");
        Set<PreparedArtifact> artifacts = new LinkedHashSet<>();
        for (Object object : (JSONArray) parsed.get("artifacts")) {
          artifacts.add(preparedArtifactFromJsonObject((JSONObject) object));
        }

        return new PreparedArtifactWrapper(Path.of(prefix), artifacts);
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
  }

  private static Writer newWriter(File file) throws IOException {
    FileOutputStream outputStream = new FileOutputStream(file);
    return new BufferedWriter(
      new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
    );
  }

  private static Reader newReader(File file) throws IOException {
    FileInputStream outputStream = new FileInputStream(file);
    return new BufferedReader(
      new InputStreamReader(outputStream, StandardCharsets.UTF_8)
    );
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

  private static JSONObject toJsonObject(PreparedArtifact preparedArtifact) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("localPath", preparedArtifact.getLocalPath());
    jsonObject.put("targetPath", preparedArtifact.getTargetPath());

    return jsonObject;
  }

  private static PreparedArtifact preparedArtifactFromJsonObject(JSONObject jsonObject) {
    String localPath = (String) jsonObject.get("localPath");
    String targetPath = (String) jsonObject.get("targetPath");

    return new PreparedArtifact(localPath, targetPath);
  }
}

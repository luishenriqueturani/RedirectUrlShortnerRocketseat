package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final S3Client s3Client = S3Client.builder().build();

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

    String pathParams = (String) input.get("rawPath");

    String shortUrl = pathParams.replace("/", "");

    if(shortUrl.isEmpty()){
      throw new IllegalArgumentException("O path param não pode estar vazio: " + pathParams);
    }

    InputStream s3Object;

    try {
      GetObjectRequest request = GetObjectRequest.builder()
          .bucket("java-lambda-rocketseat-url-storage")
          .key(shortUrl + ".json")
          .build();

      s3Object = s3Client.getObject(request);

    } catch (Exception e) {
      throw new RuntimeException("Erro ao obter o objeto "+shortUrl+" do bucket: " + e.getMessage());
    }

    UrlData urlData;

    try {
      urlData = objectMapper.readValue(s3Object, UrlData.class);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao converter o objeto do bucket: " + e.getMessage());
    }

    long now = System.currentTimeMillis() / 1000;
    long expirationTime = urlData.getExpirationTime();

    Map<String, Object> response = new HashMap<>();

    if(expirationTime < now){
      response.put("statusCode", 410);
      response.put("body", "Url expirada");

      return response;
    }

    response.put("statusCode", 302);

    Map<String, String> headers = new HashMap<>();
    headers.put("Location", urlData.getOriginalUrl());

    response.put("headers", headers);

    return response;
  }
}
package net.rkr1410.resumeregistry;

import net.rkr1410.resumeregistry.model.Resume;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
public class ResumeRegistryTestClient {

    private static final String REST_SERVICE_URI = "http://localhost:8080/api";
    private static final String PARAM_EMAIL      = "email";
    private static final String PARAM_BODY_TEXT  = "body";

    private static void uploadResume(String email, String bodyText) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, Object>> emailAndResumeBodyRequestEntity =
                createEmailAndResumeBodyRequestEntity(email, bodyText);

        ResponseEntity response = restTemplate.exchange(REST_SERVICE_URI + "/resume", HttpMethod.POST,
                emailAndResumeBodyRequestEntity, ResponseEntity.class);

        HttpStatus responseStatusCode = response.getStatusCode();
        HttpStatus createdStatusCode = HttpStatus.CREATED;
        Assert.isTrue(responseStatusCode == createdStatusCode,
                "Expected http " + createdStatusCode + ", got " + responseStatusCode);
        Assert.notEmpty(response.getHeaders().get("Location"), "No Location header in response");
    }

    private static HttpEntity<MultiValueMap<String, Object>>  createEmailAndResumeBodyRequestEntity(String email, String bodyText) throws IOException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        String uploadFilename = "./resume.txt";
        File fileToUpload = new File(uploadFilename);
        FileOutputStream fos = new FileOutputStream(fileToUpload);
        fos.write(bodyText.getBytes());
        fos.close();

        MultiValueMap<String, Object> mvm = new LinkedMultiValueMap<String, Object>() {{
            add(PARAM_EMAIL, email);
            add(PARAM_BODY_TEXT, new FileSystemResource(uploadFilename));
        }};
        return new HttpEntity<>(mvm, requestHeaders);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        return new HttpMessageConverters(arrayHttpMessageConverter);
    }

    private static void downloadCurrentResumeForEmail(String email, long expectedVersion) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                REST_SERVICE_URI + "/resume/current?email={email}",
                HttpMethod.GET, createAcceptOctetStreamHttpEntity(), byte[].class, email);

        String expectedFilename = "current.txt";
        assertFilenameFoundInContentDispositionHeader(response, expectedFilename);
    }

    private static void downloadResumeForEmailByVersion(String email, long version) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                REST_SERVICE_URI + "/resume/{version}?email={email}",
                HttpMethod.GET, createAcceptOctetStreamHttpEntity(), byte[].class, version, email);

        String expectedFilename = "resume_v" + version + ".txt";
        assertFilenameFoundInContentDispositionHeader(response, expectedFilename);
    }

    private static HttpEntity<String> createAcceptOctetStreamHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        return new HttpEntity<>(headers);
    }

    private static void assertFilenameFoundInContentDispositionHeader(ResponseEntity<byte[]> response, String expectedFilename) {
        List<String> contentDispositionHeaders = response.getHeaders().get("Content-Disposition");
        Assert.notEmpty(contentDispositionHeaders, "No Content-Disposition header in response");
        Assert.isTrue(contentDispositionHeaders.size() == 1, "More that one Content-Disposition header");
        String contentDispositionHeader = contentDispositionHeaders.get(0);
        Assert.isTrue(contentDispositionHeader.contains("filename=" + expectedFilename),
                "Content-Disposition header: \"" + contentDispositionHeader + "\",\nwas expected to contain filename=" + expectedFilename);
    }

    private static void failTryingToDownloadCurrentResumeForNonExistentEmail(String emailNotYetStoredInRegistry) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            Resume currentResume = restTemplate.getForObject(
                    REST_SERVICE_URI + "/resume/current?email={email}",
                    Resume.class, emailNotYetStoredInRegistry);
            Assert.isNull(currentResume,
                    "Expected no current resume for email " + emailNotYetStoredInRegistry
                            + ", found one with version " + currentResume.getVersion());
        } catch (HttpClientErrorException e) {
            HttpStatus responseHttpStatus = e.getStatusCode();
            HttpStatus httpStatus404 = HttpStatus.NOT_FOUND;
            Assert.isTrue(responseHttpStatus == httpStatus404,
                            "Expected http " + httpStatus404 + ", got " + responseHttpStatus);
        }
    }

    private static void deleteAllResumesForEmail(String email) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(REST_SERVICE_URI + "/resume/delete?email={email}", email);
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = SpringApplication.run(ResumeRegistryTestClient.class);

        String email = "test@test.com";

        // Users must be able to upload their CVs alongside their email identifier
        uploadResume(email, "test: pierwsza wersja CV");

        // Users must be able to update their CV using their email identifier
        uploadResume(email, "test: druga wersja CV");

        // Users must be able to retrieve their current CV using the email identifier
        long expectedCurrentResumeVersion = 2;
        downloadCurrentResumeForEmail(email, expectedCurrentResumeVersion);

        // Users must be able to retrieve any version of their CV using their email identifier and version number
        downloadResumeForEmailByVersion(email, 1);

        //Users must be able to delete their CV
        deleteAllResumesForEmail(email);
        failTryingToDownloadCurrentResumeForNonExistentEmail(email);

        SpringApplication.exit(applicationContext);
    }

}

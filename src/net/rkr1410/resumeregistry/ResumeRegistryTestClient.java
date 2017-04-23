package net.rkr1410.resumeregistry;

import net.rkr1410.resumeregistry.model.Resume;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ResumeRegistryTestClient {

    private static final String REST_SERVICE_URI = "http://localhost:8080/api";
    private static final String PARAM_EMAIL      = "email";
    private static final String PARAM_BODY_TEXT  = "bodyText";

    private static void createResume(String email, String bodyText) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> emailAndResumeBodyRequestEntity = createEmailAndResumeBodyRequestEntity(email, bodyText);
        ResponseEntity response = restTemplate.exchange(REST_SERVICE_URI + "/resume", HttpMethod.POST,
                emailAndResumeBodyRequestEntity, ResponseEntity.class);

        HttpStatus responseStatusCode = response.getStatusCode();
        HttpStatus createdStatusCode = HttpStatus.CREATED;
        Assert.isTrue(responseStatusCode == createdStatusCode,
                "Expected http " + createdStatusCode + ", got " + responseStatusCode);
        Assert.notNull(response.getHeaders().get("Location"), "No Location header in response");
    }

    private static HttpEntity<MultiValueMap<String, String>>  createEmailAndResumeBodyRequestEntity(String email, String bodyText) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<String, String>() {{
            add(PARAM_EMAIL, email);
            add(PARAM_BODY_TEXT, bodyText);
        }};
        return new HttpEntity<>(mvm, requestHeaders);
    }

    private static void retrieveCurrentResumeForEmail(String email, long expectedVersion) {
        RestTemplate restTemplate = new RestTemplate();
        Resume currentResume = restTemplate.getForObject(
                REST_SERVICE_URI + "/resume/current?email={email}",
                Resume.class, email);
        long currentResumeVersion = currentResume.getVersion();
        Assert.isTrue(expectedVersion == currentResumeVersion,
                "Resume version mismatch, expected " + expectedVersion + ", got " + currentResumeVersion);
    }

    private static void retrieveResumeForEmailByVersion(String email, long version) {
        RestTemplate restTemplate = new RestTemplate();
        Resume versionedResume = restTemplate.getForObject(
                REST_SERVICE_URI + "/resume/{version}?email={email}",
                Resume.class, version, email);
        long foundResumeVersion = versionedResume.getVersion();
        Assert.isTrue(version == foundResumeVersion,
                "Resume version mismatch, expected " + version + ", got " + foundResumeVersion);
    }

    private static void failGettingCurrentResumeForNonExistentEmail(String emailNotYetStoredInRegistry) {
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

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(ResumeRegistryTestClient.class);

        String email = "test@test.com";

        // Users must be able to upload their CVs alongside their email identifier
        createResume(email, "test: pierwsza wersja CV");

        // Users must be able to update their CV using their email identifier
        createResume(email, "test: druga wersja CV");

        // Users must be able to retrieve their current CV using the email identifier
        long expectedCurrentResumeVersion = 2;
        retrieveCurrentResumeForEmail(email, expectedCurrentResumeVersion);

        // Users must be able to retrieve any version of their CV using their email identifier and version number
        retrieveResumeForEmailByVersion(email, 1);

        //Users must be able to delete their CV
        deleteAllResumesForEmail(email);
        failGettingCurrentResumeForNonExistentEmail(email);

        SpringApplication.exit(applicationContext);
    }

}

package net.rkr1410.resumeregistry.controller;

import net.rkr1410.resumeregistry.CustomMessage;
import net.rkr1410.resumeregistry.model.Resume;
import net.rkr1410.resumeregistry.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ResumeRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeRegistryController.class);

    @Autowired
    private ResumeService resumeService;

    @RequestMapping(value = "/resume", method = RequestMethod.POST)
    public ResponseEntity<?> uploadResume(
            @RequestParam("email") String email,
            @RequestParam("body") MultipartFile resumeBody,
            UriComponentsBuilder ucBuilder) {

        logger.info("Uploading resume for email {}", email);

        Resume createdResume;
        try {
            createdResume = resumeService.saveResumeForEmail(email, resumeBody.getBytes());
        } catch (IOException e) {
            return new ResponseEntity<>(new CustomMessage("File storage failed: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Resume created, version " + createdResume.getVersion());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                ucBuilder.path("/api/resume/{version}")
                        .query("email={email}")
                        .buildAndExpand(createdResume.getVersion(), email).toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/resume/current", method = RequestMethod.GET)
    public ResponseEntity<?> findCurrentResumeForEmail(@RequestParam("email") String email) {
        logger.info("Looking for current resume for email {}", email);

        Optional<Resume> currentResumeForEmail = resumeService.findCurrentResumeForEmail(email);
        if (currentResumeForEmail.isPresent()) {
            Resume resume = currentResumeForEmail.get();

            logger.info("Resume found, version " + resume.getVersion());

            return createFileResponseEntityFromResume(resume, "current.txt");
        }
        else {
            logger.info("Resume not found");

            return new ResponseEntity<>(new CustomMessage("No resumes for email " + email + " found"),
                    HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/resume/delete", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteAllResumesForEmail(
            @RequestParam("email") String email) {

        logger.info("Deleting resumes for email {}", email);

        resumeService.deleteAllResumesForEmail(email);

        logger.info("Resume records deleted");

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/resume/{version}", method = RequestMethod.GET)
    public ResponseEntity<?> findResumeForEmailByVersion(
            @PathVariable("version") long resumeVersion,
            @RequestParam("email") String email) {

        logger.info("Looking for resume version {} for email {}", resumeVersion, email);

        Optional<Resume> versionedResume = resumeService.findResumeByEmailAndVersion(email, resumeVersion);
        if (versionedResume.isPresent()) {
            Resume resume = versionedResume.get();

            logger.info("Resume found, version " + resume.getVersion());

            return createFileResponseEntityFromResume(resume, "resume_v" + resume.getVersion() + ".txt");
        }
        else {
            logger.info("Resume not found");

            return new ResponseEntity<>(new CustomMessage("No resume with version " + resumeVersion + " for email " + email + " found"),
                    HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity createFileResponseEntityFromResume(Resume resume, String fileNamePresentedToClient) {
        byte[] resumeBodyAsByteArray = resume.getBody();
        ByteArrayResource resumeFileContents = new ByteArrayResource(resumeBodyAsByteArray);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileNamePresentedToClient)
                .contentLength(resumeBodyAsByteArray.length)
                .body(resumeFileContents);
    }
}

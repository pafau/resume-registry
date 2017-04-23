package net.rkr1410.resumeregistry.service;

import net.rkr1410.resumeregistry.model.Resume;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("resumeService")
public class JavaMapBackedResumeServiceImpl implements ResumeService {
    private static Map<String, List<Resume>> resumesByEmail = new HashMap<>();

    @Override
    public Resume saveResumeForEmail(String email, byte[] body) {
        List<Resume> currentResumes = resumesByEmail.getOrDefault(email, new ArrayList<>());
        Resume newResumeVersion = new Resume(currentResumes.size() + 1, body, email);
        currentResumes.add(newResumeVersion);
        resumesByEmail.put(email, currentResumes);
        return newResumeVersion;
    }

    @Override
    public Optional<Resume> findCurrentResumeForEmail(String email) {
        List<Resume> currentResumes = resumesByEmail.getOrDefault(email, new ArrayList<>());
        return currentResumes.stream().reduce((first, last) -> last);
    }

    @Override
    public void deleteAllResumesForEmail(String email) {
        resumesByEmail.remove(email);
    }

    @Override
    public Optional<Resume> findResumeByEmailAndVersion(String email, long version) {
        List<Resume> currentResumes = resumesByEmail.getOrDefault(email, new ArrayList<>());
        return currentResumes.stream().filter(r -> r.getVersion() == version).findFirst();
    }
}

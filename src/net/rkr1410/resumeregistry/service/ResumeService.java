package net.rkr1410.resumeregistry.service;

import net.rkr1410.resumeregistry.model.Resume;

import java.util.Optional;

/**
   1) Users must be able to upload their CVs alongside their email identifier
   2) Users must be able to retrieve their current CV using the email identifier
   3) Users must be able to update their CV using their email identifier
   4) Users must be able to delete their CV
   5) Users must be able to retrieve any version of their CV using their email identifier and version number

   Moje założenia: 'update' z p.3 oznacza wgranie nowej wersji, a nie aktualizację starej.
                   'delete' z p.4 oznacza usunięcie wszytkich wersji dla danego identyfikatora email
 */
public interface ResumeService {

    Resume saveResumeForEmail(String email, byte[] body);
    Optional<Resume> findCurrentResumeForEmail(String email);
    void deleteAllResumesForEmail(String email);
    Optional<Resume> findResumeByEmailAndVersion(String email, long version);
}
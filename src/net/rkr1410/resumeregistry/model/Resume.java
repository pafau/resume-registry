package net.rkr1410.resumeregistry.model;


import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

public class Resume extends ResourceSupport {

    private long version;
    private byte[] body;
    private String email;

    // Na potrzeby ResumeRegistryTestClient, musi dać sie tworzyć przez reflection
    // instrumentacja kodu sama sobie obejdzie private, a my na zewnątrz nie wystawiamy
    // pustego konstruktora
    private Resume() {}

    public Resume(long version, byte[] body, String email) {
        this.version = version;
        this.body = body;
        this.email = email;
    }

    public long getVersion() {
        return version;
    }

    public byte[] getBody() {
        return body;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public void add(Link link) {
        if (!hasLink(link.getRel())) {
            super.add(link);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resume resume = (Resume) o;

        return version == resume.version && email.equals(resume.email);
    }

    @Override
    public int hashCode() {
        int result = (int) (version ^ (version >>> 32));
        result = 31 * result + email.hashCode();
        return result;
    }
}

package uk.co.bbr.web.migrate;

import org.jdom2.Element;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.security.dao.BbrUserDao;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class AbstractMigrateController {
    protected static final String BASE_PATH = "/tmp/bbr";

    protected final long createUser(String username, SecurityService securityService) {
        if (username == null) {
            return 1;
        }

        Optional<BbrUserDao> user = securityService.fetchUserByUsercode(username);
        if (user.isPresent()) {
            return user.get().getId();
        }

        BbrUserDao newUser = securityService.createUser(username, "NoPassword", "migrated@brassbandresults.co.uk");
        return newUser.getId();
    }

    protected final String notBlank(Element node, String childName) {
        if (node == null) {
            throw new UnsupportedOperationException("Node passed is null");
        }
        String value = node.getChildText(childName);
        if ("None".equals(value)) {
            return null;
        }
        if (value == null) {
            return null;
        }

        if (value.trim().length() == 0) {
            return null;
        }

        return value;
    }

    protected final LocalDate notBlankDate(Element node, String childName) {
        String value = this.notBlank(node, childName);
        if (value == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(value, formatter);
    }

    protected final LocalDateTime notBlankDateTime(Element node, String childName) {
        String value = this.notBlank(node, childName);
        if (value == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(value, formatter);
    }

    protected final boolean notBlankBoolean(Element node, String childName) {
        String value = this.notBlank(node, childName);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }

    protected final String[] fetchDirectories() {
        File topLevel = new File(BASE_PATH + "/People");
        return Arrays.stream(Objects.requireNonNull(topLevel.list((current, name) -> new File(current, name).isDirectory()))).sorted().toArray(String[]::new);
    }
}
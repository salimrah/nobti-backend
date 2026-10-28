package ma.noba.admin.application.dto;

import java.util.Set;

public record UpdateUserRolesRequest(Set<String> roles) {
}

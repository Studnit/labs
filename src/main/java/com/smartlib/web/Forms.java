package com.smartlib.web;

import jakarta.validation.constraints.*;

public class Forms {
    public static class Register {
        @NotNull(message = "Choose a library") private Long libraryId;
        @NotBlank @Size(max = 100) private String fullName;
        @NotBlank @Email @Size(max = 150) private String email;
        @Size(max = 20) private String phone;
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") private String password;
        private boolean guest;

        public Long getLibraryId() { return libraryId; }
        public void setLibraryId(Long libraryId) { this.libraryId = libraryId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isGuest() { return guest; }
        public void setGuest(boolean guest) { this.guest = guest; }
    }
}

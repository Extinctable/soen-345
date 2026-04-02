package code.ticketreservationapp.service;

final class EmailJsConfiguration {

    private final String endpoint;
    private final String serviceId;
    private final String templateId;
    private final String publicKey;
    private final String accessToken;

    EmailJsConfiguration(String endpoint, String serviceId, String templateId, String publicKey, String accessToken) {
        this.endpoint = endpoint;
        this.serviceId = serviceId;
        this.templateId = templateId;
        this.publicKey = publicKey;
        this.accessToken = accessToken;
    }

    static EmailJsConfiguration defaultConfig() {
        return new EmailJsConfiguration(
                "https://api.emailjs.com/api/v1.0/email/send",
                "service_ipws9e8",
                "template_8lj7m9p",
                "MvFcf6AYWDcDRbfZG",
                "82lhXcpQbSmYuThRzJpBy"
        );
    }

    String getEndpoint() {
        return endpoint;
    }

    String getServiceId() {
        return serviceId;
    }

    String getTemplateId() {
        return templateId;
    }

    String getPublicKey() {
        return publicKey;
    }

    String getAccessToken() {
        return accessToken;
    }
}
